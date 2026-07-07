package rs.tapizlabs.mail.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import rs.tapizlabs.mail.data.local.entity.FolderType
import rs.tapizlabs.mail.data.local.entity.MessageEntity

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE folderId = :folderId ORDER BY sentAt DESC")
    fun getMessagesForFolder(folderId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE accountId = :accountId ORDER BY sentAt DESC")
    fun getMessagesForAccount(accountId: String): Flow<List<MessageEntity>>

    /** Joins against `folders` on [type] so the Sent pseudo-category (and the main Inbox
     * view's exclusion of it) always resolve against whichever [FolderEntity] currently has
     * that role for this account, instead of a cached/guessed folderId — the Sent folder,
     * unlike the local-only Drafts/Trash pseudo-folders, is a real IMAP mailbox whose id is
     * server/sync-assigned, not a stable `"local-*"` literal. */
    @Query(
        """
        SELECT messages.* FROM messages
        INNER JOIN folders ON messages.folderId = folders.id
        WHERE messages.accountId = :accountId AND folders.type = :type
        ORDER BY messages.sentAt DESC
        """
    )
    fun getMessagesForAccountByFolderType(accountId: String, type: FolderType): Flow<List<MessageEntity>>

    /** Same join as [getMessagesForAccountByFolderType] but the inverse — every message for
     * this account whose folder's type is NOT [excludedType]. Used by the main Inbox view to
     * exclude the real IMAP Sent mailbox (see [getMessagesForAccountByFolderType]'s doc) the
     * same reactive way local Trash is excluded by folderId. */
    @Query(
        """
        SELECT messages.* FROM messages
        INNER JOIN folders ON messages.folderId = folders.id
        WHERE messages.accountId = :accountId AND folders.type != :excludedType
        ORDER BY messages.sentAt DESC
        """
    )
    fun getMessagesForAccountExcludingFolderType(accountId: String, excludedType: FolderType): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE categoryId = :categoryId ORDER BY sentAt DESC")
    fun getMessagesForCategory(categoryId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun getMessage(messageId: String): Flow<MessageEntity?>

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageOnce(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE folderId = :folderId AND uid = :uid LIMIT 1")
    suspend fun findByUid(folderId: String, uid: Long): MessageEntity?

    /** Same lookup as [findByUid] but also matches messages moved to the local Trash
     * pseudo-folder whose [MessageEntity.originFolderId] was this real folder — a moved-to-
     * trash message keeps its IMAP [MessageEntity.uid] but its [MessageEntity.folderId] no
     * longer equals [folderId], so a plain folderId+uid lookup would miss it and callers would
     * wrongly treat it as never-seen. */
    @Query(
        "SELECT * FROM messages WHERE uid = :uid AND (folderId = :folderId OR originFolderId = :folderId) LIMIT 1"
    )
    suspend fun findByUidIncludingTrash(folderId: String, uid: Long): MessageEntity?

    /** Highest [MessageEntity.uid] this account has ever seen for [folderId], including
     * messages currently sitting in the local Trash pseudo-folder (see
     * [findByUidIncludingTrash]) — without this, [rs.tapizlabs.mail.data.repository.SyncRepository]'s
     * "since last uid" sync would miss that a moved-to-trash message's uid was already seen
     * and re-fetch/re-notify for it after every refresh. */
    @Query("SELECT MAX(uid) FROM messages WHERE folderId = :folderId OR originFolderId = :folderId")
    suspend fun getHighestKnownUidIncludingTrash(folderId: String): Long?

    @Query("SELECT * FROM messages WHERE isSynced = 0")
    fun getPendingMessages(): Flow<List<MessageEntity>>

    /**
     * Simple `LIKE`-based full-text search across subject/sender/body. Room FTS4 would give
     * better ranking, but a plain index-backed LIKE is sufficient for the expected per-account
     * cache size and keeps the schema simpler for the sync layer to populate.
     */
    @Query(
        """
        SELECT * FROM messages
        WHERE subject LIKE '%' || :query || '%'
           OR fromName LIKE '%' || :query || '%'
           OR fromAddress LIKE '%' || :query || '%'
           OR bodyPlain LIKE '%' || :query || '%'
        ORDER BY sentAt DESC
        """
    )
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    @Upsert
    suspend fun upsert(message: MessageEntity)

    @Upsert
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("UPDATE messages SET isRead = :isRead WHERE id = :messageId")
    suspend fun setRead(messageId: String, isRead: Boolean)

    @Query("UPDATE messages SET isStarred = :isStarred WHERE id = :messageId")
    suspend fun setStarred(messageId: String, isStarred: Boolean)

    @Query("UPDATE messages SET categoryId = :categoryId WHERE id = :messageId")
    suspend fun setCategory(messageId: String, categoryId: String?)

    @Query("UPDATE messages SET folderId = :folderId WHERE id = :messageId")
    suspend fun moveToFolder(messageId: String, folderId: String)

    @Delete
    suspend fun delete(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteById(messageId: String)

    @Query("DELETE FROM messages WHERE folderId = :folderId")
    suspend fun deleteAllForFolder(folderId: String)
}
