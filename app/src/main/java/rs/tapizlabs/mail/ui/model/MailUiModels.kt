package rs.tapizlabs.mail.ui.model

import rs.tapizlabs.mail.data.local.dao.MessageListRow

/**
 * UI-facing view models for a mail list row — deliberately NOT the Room rows themselves, so
 * composables only ever see what a row renders. ViewModels map
 * [rs.tapizlabs.mail.data.local.dao.MessageListRow] -> these via [toListItemUi].
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
    /** Comma-separated recipients — shown instead of the sender when [isOutgoing]. */
    val toAddresses: String = "",
    /** True for rows in the Sent/Drafts views, where every row's sender is the user's own
     * address and the recipient is the information that tells rows apart. */
    val isOutgoing: Boolean = false,
)

fun MessageListRow.toListItemUi(isOutgoing: Boolean = false) = MessageListItemUi(
    id = id,
    fromName = fromName,
    fromAddress = fromAddress,
    subject = subject,
    snippet = snippet,
    sentAt = sentAt,
    isRead = isRead,
    isStarred = isStarred,
    hasAttachments = hasAttachments,
    categoryColorIndex = null,
    toAddresses = toAddresses,
    isOutgoing = isOutgoing,
)

data class CategoryChipUi(
    val id: String?,
    val name: String,
    /** Badge number — unread count for Inbox/categories, item count for Drafts/Trash;
     * the chip hides it when 0. */
    val count: Int,
    val colorIndex: Int,
)

data class AccountSummaryUi(
    val id: String,
    val displayName: String,
    val emailAddress: String,
)
