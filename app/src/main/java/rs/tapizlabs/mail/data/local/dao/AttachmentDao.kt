package rs.tapizlabs.mail.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import rs.tapizlabs.mail.data.local.entity.AttachmentEntity

@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE messageId = :messageId")
    fun getAttachmentsForMessage(messageId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE id = :attachmentId")
    suspend fun getAttachmentOnce(attachmentId: String): AttachmentEntity?

    @Upsert
    suspend fun upsert(attachment: AttachmentEntity)

    @Upsert
    suspend fun upsertAll(attachments: List<AttachmentEntity>)

    @Query("UPDATE attachments SET localUri = :localUri WHERE id = :attachmentId")
    suspend fun setLocalUri(attachmentId: String, localUri: String?)

    @Delete
    suspend fun delete(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE messageId = :messageId")
    suspend fun deleteAllForMessage(messageId: String)
}
