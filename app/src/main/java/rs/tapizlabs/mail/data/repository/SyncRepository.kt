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
            val folders = folderDao.getFoldersForAccount(accountId).first()
            var newCount = 0
            for (folder in folders) {
                newCount += syncOneFolder(store, folder, rules, account.displayName)
            }
            newCount
        } finally {
            runCatching { store.close() }
        }
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
     * for Sent/Drafts/Trash/Archive/Custom folders syncing in the background. */
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
