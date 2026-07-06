package rs.tapizlabs.mail.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rs.tapizlabs.mail.data.local.dao.AccountDao
import rs.tapizlabs.mail.data.local.dao.AttachmentDao
import rs.tapizlabs.mail.data.local.dao.CategoryDao
import rs.tapizlabs.mail.data.local.dao.FolderDao
import rs.tapizlabs.mail.data.local.dao.MessageDao
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.local.entity.AttachmentEntity
import rs.tapizlabs.mail.data.local.entity.CategoryEntity
import rs.tapizlabs.mail.data.local.entity.FolderEntity
import rs.tapizlabs.mail.data.local.entity.FolderType
import rs.tapizlabs.mail.data.local.entity.MessageEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read/local-mutation facade over the Room DAOs for the four UI screens (Inbox, Detail,
 * Compose, Search) — screens/ViewModels never call DAOs directly per project convention.
 *
 * This does NOT cover network I/O (IMAP fetch, SMTP send): those are exposed separately via
 * [MailSyncGateway], implemented by the sync-layer agent's work, so this repository stays a
 * thin, easily-testable Room wrapper.
 */
interface MailRepository {
    fun observeAccounts(): Flow<List<AccountEntity>>
    fun observeActiveAccounts(): Flow<List<AccountEntity>>
    suspend fun getAccountOnce(accountId: String): AccountEntity?

    fun observeMessagesForAccount(accountId: String): Flow<List<MessageEntity>>
    fun observeMessagesForFolder(folderId: String): Flow<List<MessageEntity>>
    fun observeMessagesForCategory(categoryId: String): Flow<List<MessageEntity>>
    fun observeMessage(messageId: String): Flow<MessageEntity?>
    suspend fun getMessageOnce(messageId: String): MessageEntity?
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    fun observeCategoriesForAccount(accountId: String): Flow<List<CategoryEntity>>
    fun observeAllCategories(): Flow<List<CategoryEntity>>

    fun observeAttachmentsForMessage(messageId: String): Flow<List<AttachmentEntity>>

    suspend fun setRead(messageId: String, isRead: Boolean)
    suspend fun setStarred(messageId: String, isStarred: Boolean)
    suspend fun setCategory(messageId: String, categoryId: String?)
    suspend fun deleteMessage(messageId: String)

    /** Local-only drafts (never IMAP-synced — [MessageEntity.isSynced] stays false). Auto-
     * provisions a per-account "Drafts" [FolderEntity] on first use so this works even for
     * accounts whose IMAP server has no Drafts mailbox of its own. */
    fun observeDrafts(accountId: String): Flow<List<MessageEntity>>
    suspend fun saveDraft(draft: MessageEntity): MessageEntity
    suspend fun discardDraft(messageId: String)

    /** Local-only Trash, same auto-provisioned-folder pattern as Drafts: swiping/deleting a
     * synced message just moves it here (no IMAP call, since these accounts' own Trash mailbox
     * semantics vary too much to rely on) — permanently removing it is a separate, explicit
     * action from inside the Trash view. */
    fun observeTrash(accountId: String): Flow<List<MessageEntity>>
    suspend fun moveToTrash(messageId: String)
    suspend fun restoreFromTrash(messageId: String)
    suspend fun permanentlyDeleteMessage(messageId: String)

    /** Messages sent from this account (the account's real IMAP Sent mailbox, synced like any
     * other folder) — kept out of the main Inbox view (see [observeMessagesForAccount]) so
     * sent mail doesn't mix in with received mail; this pseudo-category is the one place to
     * see "what I sent" separately. */
    fun observeSent(accountId: String): Flow<List<MessageEntity>>

    /** Resolves the account's real IMAP folder id for [type] (e.g. its INBOX or Sent
     * mailbox) — used by "load more" (see [rs.tapizlabs.mail.data.repository.MailSyncGateway.loadOlderMessages])
     * to know which folder to page against for whichever pseudo-category is currently
     * selected. Returns null before the account's first sync has provisioned its folders. */
    suspend fun getFolderIdByType(accountId: String, type: FolderType): String?
}

@Singleton
class RoomMailRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val messageDao: MessageDao,
    private val categoryDao: CategoryDao,
    private val attachmentDao: AttachmentDao,
    private val folderDao: FolderDao,
) : MailRepository {

    override fun observeAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    override fun observeActiveAccounts(): Flow<List<AccountEntity>> = accountDao.getActiveAccounts()

    override suspend fun getAccountOnce(accountId: String): AccountEntity? =
        accountDao.getAccountOnce(accountId)

    override fun observeMessagesForAccount(accountId: String): Flow<List<MessageEntity>> =
        // Excludes the real IMAP Sent folder (join-based, see getMessagesForAccountExcludingFolderType)
        // so sent mail doesn't mix into the main Inbox view — Sent has its own pseudo-category
        // (see observeSent) mirroring how Drafts/Trash are split out.
        messageDao.getMessagesForAccountExcludingFolderType(accountId, FolderType.SENT).map { messages ->
            // Also excludes the local Trash folder — a message moved there (see moveToTrash)
            // must disappear from the normal Inbox view even though it's still the same
            // account's row; the Trash pseudo-category is the only place it should still show up.
            messages.filter { it.folderId != "local-trash-$accountId" }
        }

    override fun observeMessagesForFolder(folderId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForFolder(folderId)

    override fun observeMessagesForCategory(categoryId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForCategory(categoryId)

    override fun observeMessage(messageId: String): Flow<MessageEntity?> =
        messageDao.getMessage(messageId)

    override suspend fun getMessageOnce(messageId: String): MessageEntity? =
        messageDao.getMessageOnce(messageId)

    override fun searchMessages(query: String): Flow<List<MessageEntity>> =
        messageDao.searchMessages(query)

    override fun observeCategoriesForAccount(accountId: String): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesForAccount(accountId)

    override fun observeAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    override fun observeAttachmentsForMessage(messageId: String): Flow<List<AttachmentEntity>> =
        attachmentDao.getAttachmentsForMessage(messageId)

    override suspend fun setRead(messageId: String, isRead: Boolean) =
        messageDao.setRead(messageId, isRead)

    override suspend fun setStarred(messageId: String, isStarred: Boolean) =
        messageDao.setStarred(messageId, isStarred)

    override suspend fun setCategory(messageId: String, categoryId: String?) =
        messageDao.setCategory(messageId, categoryId)

    override suspend fun deleteMessage(messageId: String) = messageDao.deleteById(messageId)

    override fun observeDrafts(accountId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForAccount(accountId).map { messages ->
            messages.filter { !it.isSynced }
        }

    override suspend fun saveDraft(draft: MessageEntity): MessageEntity {
        val draftsFolderId = getOrCreateDraftsFolder(draft.accountId)
        val toSave = draft.copy(folderId = draftsFolderId, isSynced = false)
        messageDao.upsert(toSave)
        return toSave
    }

    override suspend fun discardDraft(messageId: String) = messageDao.deleteById(messageId)

    override fun observeTrash(accountId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForAccount(accountId).map { messages ->
            messages.filter { it.folderId == "local-trash-$accountId" }
        }

    override suspend fun moveToTrash(messageId: String) {
        val message = messageDao.getMessageOnce(messageId) ?: return
        val trashFolderId = getOrCreateTrashFolder(message.accountId)
        // Remembers the message's real IMAP folder (originFolderId) before overwriting
        // folderId with the local-only Trash pseudo-folder — the IMAP UID itself doesn't
        // change (the real mailbox is never touched here), so accountId+originFolderId+uid
        // is enough to resolve this message back to its server-side location later for a
        // remote seen-flag/delete mutation (see MailSyncGateway.deleteMessageRemote). Only
        // set on the first move into Trash; a message already in Trash keeps its original
        // origin rather than overwriting it with the Trash folder id itself.
        val updated = message.copy(
            folderId = trashFolderId,
            originFolderId = message.originFolderId ?: message.folderId,
        )
        messageDao.upsert(updated)
    }

    override suspend fun restoreFromTrash(messageId: String) {
        val message = messageDao.getMessageOnce(messageId) ?: return
        // The synced Inbox folder is provisioned by SyncRepository.ensureFoldersProvisioned
        // during the account's first sync, so by the time anything is in Trash it must
        // already exist — falls back to leaving the message in Trash (no-op) if somehow not.
        val inboxFolderId = folderDao.getFolderOnceByType(message.accountId, FolderType.INBOX)?.id ?: return
        messageDao.moveToFolder(messageId, inboxFolderId)
    }

    override suspend fun permanentlyDeleteMessage(messageId: String) = messageDao.deleteById(messageId)

    override fun observeSent(accountId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForAccountByFolderType(accountId, FolderType.SENT)

    override suspend fun getFolderIdByType(accountId: String, type: FolderType): String? =
        folderDao.getFolderOnceByType(accountId, type)?.id

    /** Drafts folders created for local-only drafts are never IMAP-synced, so their id
     * doesn't need to match any remote mailbox — a stable per-account id keeps
     * [getOrCreateDraftsFolder] idempotent without a lookup race. */
    private suspend fun getOrCreateDraftsFolder(accountId: String): String {
        folderDao.getFolderOnceByType(accountId, FolderType.DRAFTS)?.let { return it.id }
        val folder = FolderEntity(
            id = "local-drafts-$accountId",
            accountId = accountId,
            remoteName = "Drafts",
            displayName = "Drafts",
            type = FolderType.DRAFTS,
            unreadCount = 0,
        )
        folderDao.upsert(folder)
        return folder.id
    }

    /** Same rationale as [getOrCreateDraftsFolder]: a local-only Trash folder, excluded from
     * IMAP sync via [rs.tapizlabs.mail.data.repository.SyncRepository]'s `local-` id prefix
     * filter, so moving a message here never triggers a remote fetch/parse against it. */
    private suspend fun getOrCreateTrashFolder(accountId: String): String {
        folderDao.getFolderOnceByType(accountId, FolderType.TRASH)?.let { return it.id }
        val folder = FolderEntity(
            id = "local-trash-$accountId",
            accountId = accountId,
            remoteName = "Trash",
            displayName = "Trash",
            type = FolderType.TRASH,
            unreadCount = 0,
        )
        folderDao.upsert(folder)
        return folder.id
    }
}
