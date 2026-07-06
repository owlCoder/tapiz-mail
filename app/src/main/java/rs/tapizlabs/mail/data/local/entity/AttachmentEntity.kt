package rs.tapizlabs.mail.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** An attachment belonging to a [MessageEntity], downloaded lazily. */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("messageId")],
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val messageId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    /** Local content URI once downloaded; null until fetched on demand. */
    val localUri: String?,
    /** `Content-ID` header for inline images referenced from `bodyHtml`; null otherwise. */
    val contentId: String?,
    /** Index of this body part within the source message's MimeMultipart structure — needed
     * to re-fetch this specific attachment's bytes on demand (see
     * [rs.tapizlabs.mail.mail.ImapClient.downloadAttachment]) since envelope sync never
     * downloads attachment bytes eagerly. Defaults to 0 for rows written before this field
     * existed; those can no longer be re-downloaded correctly, but existing already-cached
     * [localUri]s are unaffected. */
    val partIndex: Int = 0,
)
