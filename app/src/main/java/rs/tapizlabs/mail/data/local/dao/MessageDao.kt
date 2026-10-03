package rs.tapizlabs.mail.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import rs.tapizlabs.mail.data.local.entity.MessageEntity

/** Columns of [MessageListRow] — shared by every list query so none of them drags the body
 * columns through the cursor. */
private const val LIST_COLUMNS = """
    messages.id, messages.accountId, messages.folderId, messages.fromName, messages.fromAddress,
    messages.toAddresses, messages.subject, messages.snippet, messages.sentAt, messages.isRead,
    messages.isStarred, messages.hasAttachments, messages.categoryId, messages.isSynced
"""

/** "Belongs in the main Inbox view": a synced message living in a real INBOX/CUSTOM mailbox.
 * Sent has its own pseudo-category, and Drafts/Trash/Junk mailboxes (server-side or the
 * local-only `local-*` pseudo-folders) never mix into received mail. */
private const val INBOX_PREDICATE = "folders.type IN ('INBOX', 'CUSTOM') AND messages.isSynced = 1"

/** Local-only Trash pseudo-folder ids are `local-trash-<accountId>` (see MailRepository). */
private const val TRASH_PREDICATE = "messages.folderId LIKE 'local-trash-%'"

@Dao
interface MessageDao {

    /** Main Inbox list. A null [accountId] means "all accounts"; a non-null [categoryId]
     * narrows the same view to one user category. */
    @Query(
        """
        SELECT $LIST_COLUMNS FROM messages
        INNER JOIN folders ON messages.folderId = folders.id
        WHERE $INBOX_PREDICATE
          AND (:accountId IS NULL OR messages.accountId = :accountId)
          AND (:categoryId IS NULL OR messages.categoryId = :categoryId)
        ORDER BY messages.sentAt DESC
        """
    )
    fun observeInbox(accountId: String?, categoryId: String?): Flow<List<MessageListRow>>

    /** The account's real IMAP Sent mailbox — joined on folder type rather than a cached
     * folderId, since that id is server/sync-assigned, not a stable `local-*` literal. */
    @Query(
        """
        SELECT $LIST_COLUMNS FROM messages
        INNER JOIN folders ON messages.folderId = folders.id
        WHERE folders.type = 'SENT' AND messages.isSynced = 1
          AND (:accountId IS NULL OR messages.accountId = :accountId)
        ORDER BY messages.sentAt DESC
        """
    )
    fun observeSent(accountId: String?): Flow<List<MessageListRow>>

    /** Local-only drafts — never IMAP-synced, so `isSynced = 0` is what identifies them. */
    @Query(
        """
        SELECT $LIST_COLUMNS FROM messages
        WHERE messages.isSynced = 0 AND NOT ($TRASH_PREDICATE)
          AND (:accountId IS NULL OR messages.accountId = :accountId)
        ORDER BY messages.sentAt DESC
        """
    )
    fun observeDrafts(accountId: String?): Flow<List<MessageListRow>>

    @Query(
        """
        SELECT $LIST_COLUMNS FROM messages
        WHERE $TRASH_PREDICATE
          AND (:accountId IS NULL OR messages.accountId = :accountId)
        ORDER BY messages.sentAt DESC
        """
    )
    fun observeTrash(accountId: String?): Flow<List<MessageListRow>>

    @Query(
        """
        SELECT messages.id FROM messages
        WHERE $TRASH_PREDICATE
          AND (:accountId IS NULL OR messages.accountId = :accountId)
        """
    )
    suspend fun getTrashIds(accountId: String?): List<String>

    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN $INBOX_PREDICATE AND messages.isRead = 0 THEN 1 ELSE 0 END), 0) AS inboxUnread,
            COALESCE(SUM(CASE WHEN messages.isSynced = 0 AND NOT ($TRASH_PREDICATE) THEN 1 ELSE 0 END), 0) AS drafts,
            COALESCE(SUM(CASE WHEN $TRASH_PREDICATE THEN 1 ELSE 0 END), 0) AS trash
        FROM messages
        INNER JOIN folders ON messages.folderId = folders.id
        WHERE (:accountId IS NULL OR messages.accountId = :accountId)
        """
    )
    fun observeMailboxCounts(accountId: String?): Flow<MailboxCounts>

    /** Unread messages per category within the Inbox view — independent of which chip is
     * currently selected, so the badges don't collapse to 0 when another category is open. */
    @Query(
        """
        SELECT messages.categoryId AS categoryId, COUNT(*) AS count FROM messages
        INNER JOIN folders ON messages.folderId = folders.id
        WHERE $INBOX_PREDICATE AND messages.isRead = 0 AND messages.categoryId IS NOT NULL
          AND (:accountId IS NULL OR messages.accountId = :accountId)
        GROUP BY messages.categoryId
        """
    )
    fun getCategoryUnreadCounts(accountId: String?): Flow<List<CategoryCount>>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun getMessage(messageId: String): Flow<MessageEntity?>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageOnce(messageId: String): MessageEntity?

    /** UIDs out of [uids] this folder already has cached — including messages currently in
     * the local Trash pseudo-folder whose [MessageEntity.originFolderId] is this folder. A
     * moved-to-trash message keeps its IMAP uid but its folderId no longer matches, so a plain
     * folderId lookup would treat it as never-seen and a re-fetch would resurrect it. Callers
     * must keep [uids] under SQLite's bind-variable limit (chunk large lists). */
    @Query(
        "SELECT uid FROM messages WHERE (folderId = :folderId OR originFolderId = :folderId) AND uid IN (:uids)"
    )
    suspend fun getKnownUids(folderId: String, uids: List<Long>): List<Long>

    /** Highest uid ever seen for [folderId], including messages now in local Trash (see
     * [getKnownUids]) — the "since last uid" cursor for incremental sync. */
    @Query("SELECT MAX(uid) FROM messages WHERE isSynced = 1 AND (folderId = :folderId OR originFolderId = :folderId)")
    suspend fun getHighestKnownUid(folderId: String): Long?

    /** Lowest cached uid for [folderId] — the cursor "load older" pages backwards from. */
    @Query("SELECT MIN(uid) FROM messages WHERE isSynced = 1 AND (folderId = :folderId OR originFolderId = :folderId)")
    suspend fun getLowestKnownUid(folderId: String): Long?

    /** Flag state of the newest [limit] cached messages of [folderId] (by uid), including
     * ones moved to local Trash — the window sync reconciles against the server. */
    @Query(
        """
        SELECT id, uid, folderId, isRead, isStarred FROM messages
        WHERE isSynced = 1 AND (folderId = :folderId OR originFolderId = :folderId)
        ORDER BY uid DESC LIMIT :limit
        """
    )
    suspend fun getFlagRows(folderId: String, limit: Int): List<LocalFlagRow>

    /**
     * `LIKE`-based search across subject/sender/body, filtered and capped in SQL. Room FTS4
     * would rank better, but a LIKE scan is sufficient for the per-device cache size and keeps
     * the schema simple. [query] must already have `%`/`_`/`\` escaped (see MailRepository).
     */
    @Query(
        """
        SELECT $LIST_COLUMNS FROM messages
        WHERE messages.isSynced = 1
          AND (:accountId IS NULL OR messages.accountId = :accountId)
          AND (:attachmentsOnly = 0 OR messages.hasAttachments = 1)
          AND (messages.subject LIKE '%' || :query || '%' ESCAPE '\'
            OR messages.fromName LIKE '%' || :query || '%' ESCAPE '\'
            OR messages.fromAddress LIKE '%' || :query || '%' ESCAPE '\'
            OR messages.bodyPlain LIKE '%' || :query || '%' ESCAPE '\')
        ORDER BY messages.sentAt DESC
        LIMIT :limit
        """
    )
    fun searchMessages(query: String, accountId: String?, attachmentsOnly: Boolean, limit: Int): Flow<List<MessageListRow>>

    /** HTML-only messages stored by versions that didn't derive a text body for them: no
     * snippet in the list, invisible to search and body rules, empty quote on reply. Only the
     * head of the HTML is read — enough for a snippet/searchable text without pulling
     * multi-hundred-KB bodies through the cursor. */
    @Query(
        """
        SELECT id, substr(bodyHtml, 1, :maxHtmlChars) AS text FROM messages
        WHERE isSynced = 1 AND bodyPlain = '' AND bodyHtml != ''
        LIMIT :limit
        """
    )
    suspend fun getHtmlOnlyWithoutText(maxHtmlChars: Int, limit: Int): List<MessageTextRow>

    @Query("UPDATE messages SET bodyPlain = :bodyPlain, snippet = :snippet WHERE id = :messageId")
    suspend fun setDerivedText(messageId: String, bodyPlain: String, snippet: String)

    /** Snippets stored by versions that only swapped `\n` for a space, leaving stray `\r`s and
     * runs of blanks that render as ragged gaps in the list. */
    @Query("SELECT id, snippet AS text FROM messages WHERE snippet LIKE '%  %' OR snippet LIKE '%' || char(13) || '%' LIMIT :limit")
    suspend fun getUncollapsedSnippets(limit: Int): List<MessageTextRow>

    @Query("UPDATE messages SET snippet = :snippet WHERE id = :messageId")
    suspend fun setSnippet(messageId: String, snippet: String)

    /** One page of [accountId]'s synced mail in the shape category rules evaluate — bodies
     * capped to their head, paged by rowid so the walk is stable while rows are updated. */
    @Query(
        """
        SELECT id, fromName, fromAddress, subject, substr(bodyPlain, 1, :maxBodyChars) AS bodyHead, categoryId
        FROM messages
        WHERE accountId = :accountId AND isSynced = 1
        ORDER BY rowid LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getCategorizableRows(accountId: String, maxBodyChars: Int, limit: Int, offset: Int): List<CategorizableRow>

    @Upsert
    suspend fun upsert(message: MessageEntity)

    /** Insert-only: a row that already exists is left untouched, so re-fetching a message
     * never overwrites local state (read/star flags, category, a move to local Trash). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoring(messages: List<MessageEntity>)

    @Query("UPDATE messages SET isRead = :isRead WHERE id = :messageId")
    suspend fun setRead(messageId: String, isRead: Boolean)

    @Query("UPDATE messages SET isStarred = :isStarred WHERE id = :messageId")
    suspend fun setStarred(messageId: String, isStarred: Boolean)

    @Query("UPDATE messages SET isRead = :isRead, isStarred = :isStarred WHERE id = :messageId")
    suspend fun setFlags(messageId: String, isRead: Boolean, isStarred: Boolean)

    @Query("UPDATE messages SET categoryId = :categoryId WHERE id = :messageId")
    suspend fun setCategory(messageId: String, categoryId: String?)

    /** Moves a message into the local Trash pseudo-folder, remembering its real IMAP folder
     * in [MessageEntity.originFolderId] on the first move only (SQLite evaluates the right-hand
     * side against the pre-update row, so `folderId` here is still the original folder). */
    @Query(
        """
        UPDATE messages
        SET originFolderId = COALESCE(originFolderId, folderId), folderId = :trashFolderId
        WHERE id = :messageId
        """
    )
    suspend fun moveToTrash(messageId: String, trashFolderId: String)

    @Query("UPDATE messages SET folderId = :folderId, originFolderId = NULL WHERE id = :messageId")
    suspend fun restoreToFolder(messageId: String, folderId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: String)

    /** Callers must keep [messageIds] under SQLite's bind-variable limit (chunk large lists). */
    @Query("DELETE FROM messages WHERE id IN (:messageIds)")
    suspend fun deleteByIds(messageIds: List<String>)
}
