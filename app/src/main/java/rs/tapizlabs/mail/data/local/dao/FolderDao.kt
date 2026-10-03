package rs.tapizlabs.mail.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import rs.tapizlabs.mail.data.local.entity.FolderEntity
import rs.tapizlabs.mail.data.local.entity.FolderType

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders WHERE accountId = :accountId ORDER BY displayName ASC")
    fun getFoldersForAccount(accountId: String): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE accountId = :accountId")
    suspend fun getFoldersForAccountOnce(accountId: String): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :folderId")
    fun getFolder(folderId: String): Flow<FolderEntity?>

    @Query("SELECT * FROM folders WHERE id = :folderId")
    suspend fun getFolderOnce(folderId: String): FolderEntity?

    /** The account's real IMAP mailbox with role [type] — excludes the local-only
     * `local-*` pseudo-folders (Drafts/Trash), which share a [FolderType] with their
     * server-side counterparts but have no mailbox behind them. */
    @Query("SELECT * FROM folders WHERE accountId = :accountId AND type = :type AND id NOT LIKE 'local-%' LIMIT 1")
    suspend fun getFolderOnceByType(accountId: String, type: FolderType): FolderEntity?

    @Upsert
    suspend fun upsert(folder: FolderEntity)

    @Upsert
    suspend fun upsertAll(folders: List<FolderEntity>)

    @Delete
    suspend fun delete(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id IN (:folderIds)")
    suspend fun deleteByIds(folderIds: List<String>)

    @Query("DELETE FROM folders WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: String)
}
