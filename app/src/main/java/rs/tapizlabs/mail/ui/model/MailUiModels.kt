package rs.tapizlabs.mail.ui.model

/**
 * UI-facing view models for a mail list row / detail screen — deliberately NOT the Room
 * entities (`MessageEntity`, `AttachmentEntity`, `CategoryEntity`) so this `ui` package has
 * no compile-time dependency on `data.local`. ViewModels map entities -> these models.
 *
 * TODO: align field names/types once `data/local/entity/MessageEntity.kt` etc. land from
 * the Room-agent's work — this mirrors the shape described in the task brief:
 * MessageEntity(id, accountId, folderId, uid, subject, fromAddress, fromName, toAddresses,
 * sentAt: Long, snippet, bodyPlain, bodyHtml, isRead, isStarred, hasAttachments,
 * categoryId nullable).
 */
data class MessageListItemUi(
    val id: String,
    val fromName: String,
    val fromAddress: String,
    val subject: String,
    val snippet: String,
    val sentAt: Long,
    val isRead: Boolean,
    val isStarred: Boolean,
    val hasAttachments: Boolean,
    val categoryColorIndex: Int?,
)

data class CategoryChipUi(
    val id: String?,
    val name: String,
    val count: Int,
    val colorIndex: Int,
)

data class AccountSummaryUi(
    val id: String,
    val displayName: String,
    val emailAddress: String,
)
