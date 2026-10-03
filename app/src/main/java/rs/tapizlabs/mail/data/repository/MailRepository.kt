package rs.tapizlabs.mail.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rs.tapizlabs.mail.data.local.dao.AccountDao
import rs.tapizlabs.mail.data.local.dao.AttachmentDao
import rs.tapizlabs.mail.data.local.dao.CategoryDao
import rs.tapizlabs.mail.data.local.dao.FolderDao
import rs.tapizlabs.mail.data.local.dao.MailboxCounts
import rs.tapizlabs.mail.data.local.dao.MessageDao
import rs.tapizlabs.mail.data.local.dao.MessageListRow
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.local.entity.AttachmentEntity
import rs.tapizlabs.mail.data.local.entity.CategoryEntity
import rs.tapizlabs.mail.data.local.entity.FolderEntity
import rs.tapizlabs.mail.data.local.entity.FolderType
import rs.tapizlabs.mail.data.local.entity.MessageEntity
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

    /** Main Inbox list for [accountId] (null = all accounts), optionally narrowed to one
     * user category. Only synced mail in real INBOX/custom mailboxes — Sent, drafts, Trash and
     * Junk are never mixed in (see [rs.tapizlabs.mail.data.local.dao.MessageDao.observeInbox]). */
    fun observeInbox(accountId: String?, categoryId: String?): Flow<List<MessageListRow>>
    fun observeMessage(messageId: String): Flow<MessageEntity?>
    suspend fun getMessageOnce(messageId: String): MessageEntity?
    fun searchMessages(query: String, accountId: String?, attachmentsOnly: Boolean): Flow<List<MessageListRow>>

    /** Badge counts for the fixed chips, independent of which chip is selected. */
    fun observeMailboxCounts(accountId: String?): Flow<MailboxCounts>

    /** Unread count per category id within the Inbox view. */
    fun observeCategoryUnreadCounts(accountId: String?): Flow<Map<String, Int>>

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
    fun observeDrafts(accountId: String?): Flow<List<MessageListRow>>
    suspend fun saveDraft(draft: MessageEntity): MessageEntity
    suspend fun discardDraft(messageId: String)

    /** Local-only Trash, same auto-provisioned-folder pattern as Drafts: swiping/deleting a
     * synced message just moves it here (no IMAP call, since these accounts' own Trash mailbox
     * semantics vary too much to rely on) — permanently removing it is a separate, explicit
     * action from inside the Trash view. */
    fun observeTrash(accountId: String?): Flow<List<MessageListRow>>
    suspend fun getTrashMessageIds(accountId: String?): List<String>
    suspend fun moveToTrash(messageId: String)
    suspend fun restoreFromTrash(messageId: String)
    suspend fun permanentlyDeleteMessage(messageId: String)
    suspend fun permanentlyDeleteMessages(messageIds: List<String>)

    /** Messages sent from this account (the account's real IMAP Sent mailbox, synced like any
     * other folder) — kept out of the main Inbox view (see [observeInbox]) so
     * sent mail doesn't mix in with received mail; this pseudo-category is the one place to
     * see "what I sent" separately. */
    fun observeSent(accountId: String?): Flow<List<MessageListRow>>

    /** Resolves the account's real IMAP folder id for [type] (e.g. its INBOX or Sent
     * mailbox) — used by "load more" (see [rs.tapizlabs.mail.data.repository.MailSyncGateway.loadOlderMessages])
     * to know which folder to page against for whichever pseudo-category is currently
     * selected. Returns null before the account's first sync has provisioned its folders. */
    suspend fun getFolderIdByType(accountId: String, type: FolderType): String?
}

private const val SEARCH_RESULT_LIMIT = 200

/** Stays well under SQLite's bind-variable limit for `IN (...)` lists. */
private const val SQL_CHUNK_SIZE = 500

/** Escapes LIKE wildcards so a search for "50%" or "a_b" matches those characters literally
 * (the DAO query declares `\` as its ESCAPE character). */
private fun escapeLike(query: String): String =
    query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

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

    override fun observeInbox(accountId: String?, categoryId: String?): Flow<List<MessageListRow>> =
        messageDao.observeInbox(accountId, categoryId)

    override fun observeMessage(messageId: String): Flow<MessageEntity?> =
        messageDao.getMessage(messageId)

    override suspend fun getMessageOnce(messageId: String): MessageEntity? =
        messageDao.getMessageOnce(messageId)

    override fun searchMessages(query: String, accountId: String?, attachmentsOnly: Boolean): Flow<List<MessageListRow>> =
        messageDao.searchMessages(escapeLike(query.trim()), accountId, attachmentsOnly, SEARCH_RESULT_LIMIT)

    override fun observeMailboxCounts(accountId: String?): Flow<MailboxCounts> =
        messageDao.observeMailboxCounts(accountId)

    override fun observeCategoryUnreadCounts(accountId: String?): Flow<Map<String, Int>> =
        messageDao.getCategoryUnreadCounts(accountId).map { counts ->
            counts.associate { it.categoryId to it.count }
        }

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

    override fun observeDrafts(accountId: String?): Flow<List<MessageListRow>> =
        messageDao.observeDrafts(accountId)

    override suspend fun saveDraft(draft: MessageEntity): MessageEntity {
        val draftsFolderId = getOrCreateDraftsFolder(draft.accountId)
        val toSave = draft.copy(folderId = draftsFolderId, isSynced = false)
        messageDao.upsert(toSave)
        return toSave
    }

    override suspend fun discardDraft(messageId: String) = messageDao.deleteById(messageId)

    override fun observeTrash(accountId: String?): Flow<List<MessageListRow>> =
        messageDao.observeTrash(accountId)

    override suspend fun getTrashMessageIds(accountId: String?): List<String> =
        messageDao.getTrashIds(accountId)

    override suspend fun moveToTrash(messageId: String) {
        val message = messageDao.getMessageOnce(messageId) ?: return
        val trashFolderId = getOrCreateTrashFolder(message.accountId)
        // The real IMAP mailbox is never touched here — only this row's folderId changes,
        // with its original folder remembered in originFolderId (first move only), so
        // accountId+originFolderId+uid still resolves the server-side message later for a
        // remote flag/delete mutation (see MailSyncGateway.deleteMessageRemote) or a restore.
        messageDao.moveToTrash(messageId, trashFolderId)
    }

    override suspend fun restoreFromTrash(messageId: String) {
        val message = messageDao.getMessageOnce(messageId) ?: return
        // Back to wherever it came from (Inbox, Sent, a custom folder, local Drafts); falls
        // back to the account's Inbox if that folder is gone, and leaves the message in Trash
        // (no-op) if there's no Inbox either.
        val targetFolderId = message.originFolderId
            ?.let { folderDao.getFolderOnce(it)?.id }
            ?: folderDao.getFolderOnceByType(message.accountId, FolderType.INBOX)?.id
            ?: return
        messageDao.restoreToFolder(messageId, targetFolderId)
    }

    override suspend fun permanentlyDeleteMessage(messageId: String) = messageDao.deleteById(messageId)

    override suspend fun permanentlyDeleteMessages(messageIds: List<String>) {
        messageIds.chunked(SQL_CHUNK_SIZE).forEach { messageDao.deleteByIds(it) }
    }

    override fun observeSent(accountId: String?): Flow<List<MessageListRow>> =
        messageDao.observeSent(accountId)

    override suspend fun getFolderIdByType(accountId: String, type: FolderType): String? =
        folderDao.getFolderOnceByType(accountId, type)?.id

    /** Drafts folders created for local-only drafts are never IMAP-synced, so their id
     * doesn't need to match any remote mailbox — a stable per-account id keeps
     * [getOrCreateDraftsFolder] idempotent without a lookup race. Deliberately looked up by
     * that id, NOT by [FolderType]: the account's real server-side Drafts mailbox has the same
     * type, and filing local drafts under it would tie them to a folder sync may prune. */
    private suspend fun getOrCreateDraftsFolder(accountId: String): String {
        val folderId = "local-drafts-$accountId"
        folderDao.getFolderOnce(folderId)?.let { return it.id }
        val folder = FolderEntity(
            id = folderId,
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
     * filter, so moving a message here never triggers a remote fetch/parse against it. Looked
     * up by id for the same reason as Drafts — resolving by type used to return the server's
     * own Trash mailbox, and messages "moved" there showed up in neither Trash nor Inbox. */
    private suspend fun getOrCreateTrashFolder(accountId: String): String {
        val folderId = "local-trash-$accountId"
        folderDao.getFolderOnce(folderId)?.let { return it.id }
        val folder = FolderEntity(
            id = folderId,
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
