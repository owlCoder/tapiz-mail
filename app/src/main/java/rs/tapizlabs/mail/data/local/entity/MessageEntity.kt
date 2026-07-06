package rs.tapizlabs.mail.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A cached email message, either synced from IMAP or composed offline and pending send. */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("accountId"),
        Index("folderId"),
        Index("categoryId"),
        Index("messageIdHeader"),
    ],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val folderId: String,
    /** IMAP UID within [folderId], used for sync/dedup against the server. */
    val uid: Long,
    /** RFC 822 `Message-Id` header, used for threading and cross-folder search. */
    val messageIdHeader: String,
    val subject: String,
    val fromAddress: String,
    val fromName: String,
    /** Comma-separated list of recipient addresses. */
    val toAddresses: String,
    val sentAt: Long,
    val snippet: String,
    val bodyPlain: String,
    val bodyHtml: String,
    val isRead: Boolean,
    val isStarred: Boolean,
    val hasAttachments: Boolean,
    val categoryId: String?,
    /** False for offline-composed/pending-send messages not yet confirmed on the server. */
    val isSynced: Boolean,
    /** The [folderId] this message lived in on the IMAP server right before it was moved into
     * the local-only Trash pseudo-folder (see [rs.tapizlabs.mail.data.repository.MailRepository.moveToTrash]).
     * Null for messages that were never moved to local Trash (still in their real folder) or
     * that never had a remote folder to begin with (local-only drafts). [uid] is unaffected by
     * this move — the real IMAP mailbox is never touched, only this Room row's [folderId]
     * column changes — so `(accountId, originFolderId, uid)` is enough to resolve the original
     * server-side message for a remote flag/delete mutation after the local move. */
    val originFolderId: String? = null,
)
