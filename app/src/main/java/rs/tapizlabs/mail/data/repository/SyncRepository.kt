package rs.tapizlabs.mail.data.repository

import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rs.tapizlabs.mail.data.local.dao.AccountDao
import rs.tapizlabs.mail.data.local.dao.AttachmentDao
import rs.tapizlabs.mail.data.local.dao.CategoryRuleDao
import rs.tapizlabs.mail.data.local.dao.FolderDao
import rs.tapizlabs.mail.data.local.dao.MessageDao
import rs.tapizlabs.mail.data.local.entity.AttachmentEntity
import rs.tapizlabs.mail.data.local.entity.CategoryMatcher
import rs.tapizlabs.mail.data.local.entity.CategoryRuleEntity
import rs.tapizlabs.mail.data.local.entity.FolderEntity
import rs.tapizlabs.mail.data.local.entity.FolderType
import rs.tapizlabs.mail.data.local.entity.MessageEntity
import rs.tapizlabs.mail.mail.FolderInfo
import rs.tapizlabs.mail.mail.ImapClient
import rs.tapizlabs.mail.mail.ParsedAttachment
import rs.tapizlabs.mail.mail.ParsedMessage
import rs.tapizlabs.mail.security.CredentialStore
import rs.tapizlabs.mail.sync.NewMailNotifier

/** Shared prefix for [MailRepository.saveDraft]'s synthetic local-only Drafts
 * [FolderEntity] id (`"$LOCAL_FOLDER_ID_PREFIX$accountId"`-based) — this folder has no IMAP
 * counterpart and must be excluded from any code that iterates folders to sync against the
 * server. */
private const val LOCAL_FOLDER_ID_PREFIX = "local-"

/**
 * Single fetch-and-upsert path for one account's folders, shared by
 * [rs.tapizlabs.mail.sync.MailSyncWorker] (periodic fallback) and
 * [rs.tapizlabs.mail.sync.IdleSyncService] (IDLE push callback) so there is exactly one place
 * that talks to both [ImapClient] and Room — avoids two divergent copies of fetch/parse/upsert
 * logic that could drift (e.g. one path forgetting to run [CategoryMatcher]).
 */
@Singleton
class SyncRepository @Inject constructor(
    private val imapClient: ImapClient,
    private val credentialStore: CredentialStore,
    private val accountDao: AccountDao,
    private val folderDao: FolderDao,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val newMailNotifier: NewMailNotifier,
) {

    /** Syncs every folder of one account: connect once, fetch-new-since-last-uid per
     * folder, upsert into Room, disconnect. Returns the number of new messages stored
     * (0 on no-op/soft-failure) so callers can decide whether to show a "new mail"
     * notification without needing their own message-diffing. */
    suspend fun syncAccount(accountId: String): Int = withContext(Dispatchers.IO) {
        val account = accountDao.getAccountOnce(accountId) ?: return@withContext 0
        val password = credentialStore.getImapPassword(accountId) ?: return@withContext 0
        val rules = categoryRuleDao.getAllRulesOnce()

        val store = imapClient.connect(account, password)
        try {
            // Nothing else ever provisions a real account's remote folders into Room (the
            // only other FolderEntity writer is MailRepository's synthetic local-only
            // Drafts folder) — without this, `folders` below is permanently empty and no
            // account ever syncs its Inbox, no matter how many times refresh/IDLE fires.
            // Runs on every sync (cheap IMAP LIST call) so it also self-heals if it was
            // ever skipped, and picks up folders added on the server later.
            ensureFoldersProvisioned(accountId, store)

            // Excludes the synthetic local-only Drafts folder (see MailRepository.saveDraft)
            // — it has no IMAP-side counterpart, so fetchNewMessages would always throw
            // FolderNotFoundException for it.
            val folders = folderDao.getFoldersForAccount(accountId).first()
                .filterNot { it.id.startsWith(LOCAL_FOLDER_ID_PREFIX) }
            var newCount = 0
            for (folder in folders) {
                // One folder's failure (e.g. a stale/renamed remote mailbox) must not abort
                // sync for every other folder on the account, INBOX included.
                newCount += runCatching { syncOneFolder(store, folder, rules, account.displayName) }
                    .getOrDefault(0)
            }
            newCount
        } finally {
            runCatching { store.close() }
        }
    }

    private suspend fun ensureFoldersProvisioned(accountId: String, store: IMAPStore) {
        val remoteFolders = imapClient.listFolders(store)
        val existingByRemoteName = folderDao.getFoldersForAccount(accountId).first()
            .associateBy { it.remoteName }
        val toUpsert = remoteFolders.map { info ->
            val existing = existingByRemoteName[info.remoteName]
            FolderEntity(
                id = existing?.id ?: "$accountId:${info.remoteName}",
                accountId = accountId,
                remoteName = info.remoteName,
                displayName = info.displayName,
                type = info.type,
                unreadCount = existing?.unreadCount ?: 0,
            )
        }
        folderDao.upsertAll(toUpsert)
    }

    /** Fetches+upserts a single folder on an already-connected [store] (does not close
     * it) — used by the IDLE push path where we already know which folder changed and
     * don't need to re-check every folder on the account. */
    suspend fun syncFolder(accountId: String, store: IMAPStore, imapFolder: IMAPFolder): Int =
        withContext(Dispatchers.IO) {
            val account = accountDao.getAccountOnce(accountId) ?: return@withContext 0
            val folder = folderDao.getFoldersForAccount(accountId)
                .first()
                .firstOrNull { it.remoteName == imapFolder.fullName }
                ?: return@withContext 0
            val rules = categoryRuleDao.getAllRulesOnce()
            syncOneFolder(store, folder, rules, account.displayName)
        }

    /** Notifies the user only for genuinely new incoming mail — [FolderType.INBOX] — never
     * for Sent/Drafts/Trash/Custom folders syncing in the background. */
    private suspend fun syncOneFolder(
        store: IMAPStore,
        folder: FolderEntity,
        rules: List<CategoryRuleEntity>,
        accountDisplayName: String,
    ): Int {
        val folderInfo = FolderInfo(folder.remoteName, folder.displayName, folder.type)
        val lastUid = highestKnownUid(folder.id)
        val parsed = imapClient.fetchNewMessages(store, folderInfo, lastUid)
        if (parsed.isEmpty()) return 0
        val upserted = upsertMessages(folder.accountId, folder.id, parsed, rules)
        if (folder.type == FolderType.INBOX) {
            newMailNotifier.notifyNewMessages(accountDisplayName, upserted)
        }
        return parsed.size
    }

    /** [MessageDao] has no dedicated "max uid" query (owned by the Room/DAO agent, out of
     * scope to add here); this derives it from the folder's cached messages instead. Cheap
     * enough since it runs once per folder per sync pass, not once per message. */
    private suspend fun highestKnownUid(folderId: String): Long? =
        messageDao.getMessagesForFolder(folderId).first().maxOfOrNull { it.uid }

    private suspend fun lowestKnownUid(folderId: String): Long? =
        messageDao.getMessagesForFolder(folderId).first().minOfOrNull { it.uid }

    /** "Load more" for one account/folder as the user scrolls to the bottom of the Inbox
     * list — fetches the next [ImapClient.OLDER_PAGE_SIZE] messages older than whatever is
     * already cached (see [ImapClient.fetchOlderMessages]) and upserts them the same way a
     * normal sync does. Returns the number of older messages actually added (0 means either
     * a connection/auth failure, or the folder's oldest cached message is already sequence
     * number 1 — i.e. there's nothing left to page in — either way the caller should stop
     * requesting more for this folder). */
    suspend fun loadOlderMessages(accountId: String, folderId: String): Int = withContext(Dispatchers.IO) {
        val account = accountDao.getAccountOnce(accountId) ?: return@withContext 0
        val password = credentialStore.getImapPassword(accountId) ?: return@withContext 0
        val folder = folderDao.getFolderOnce(folderId) ?: return@withContext 0
        val rules = categoryRuleDao.getAllRulesOnce()

        val store = imapClient.connect(account, password)
        try {
            val folderInfo = FolderInfo(folder.remoteName, folder.displayName, folder.type)
            val oldestUid = lowestKnownUid(folderId)
            val parsed = imapClient.fetchOlderMessages(store, folderInfo, oldestUid, ImapClient.OLDER_PAGE_SIZE)
            if (parsed.isEmpty()) return@withContext 0
            upsertMessages(accountId, folderId, parsed, rules)
            parsed.size
        } finally {
            runCatching { store.close() }
        }
    }

    private suspend fun upsertMessages(
        accountId: String,
        folderId: String,
        parsed: List<ParsedMessage>,
        rules: List<CategoryRuleEntity>,
    ): List<MessageEntity> {
        val messageEntities = parsed.map { it.toMessageEntity(accountId, folderId, rules) }
        messageDao.upsertAll(messageEntities)

        val attachmentEntities = parsed.zip(messageEntities).flatMap { (msg, entity) ->
            msg.attachments.map { it.toAttachmentEntity(entity.id) }
        }
        if (attachmentEntities.isNotEmpty()) {
            attachmentDao.upsertAll(attachmentEntities)
        }
        return messageEntities
    }

    private fun ParsedMessage.toMessageEntity(
        accountId: String,
        folderId: String,
        rules: List<CategoryRuleEntity>,
    ): MessageEntity {
        val entity = MessageEntity(
            id = "$accountId:$folderId:$uid",
            accountId = accountId,
            folderId = folderId,
            uid = uid,
            messageIdHeader = messageIdHeader.orEmpty(),
            subject = subject.orEmpty(),
            fromAddress = fromAddress.orEmpty(),
            fromName = fromName.orEmpty(),
            toAddresses = toAddresses.joinToString(","),
            sentAt = sentAt,
            snippet = snippet.orEmpty(),
            bodyPlain = bodyPlain.orEmpty(),
            bodyHtml = bodyHtml.orEmpty(),
            isRead = isRead,
            isStarred = isStarred,
            hasAttachments = attachments.isNotEmpty(),
            categoryId = null,
            isSynced = true,
        )
        return entity.copy(categoryId = CategoryMatcher.categorize(entity, rules))
    }

    private fun ParsedAttachment.toAttachmentEntity(messageId: String): AttachmentEntity =
        AttachmentEntity(
            id = "$messageId:$partIndex",
            messageId = messageId,
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            localUri = null,
            contentId = contentId,
        )
}
