package rs.tapizlabs.mail.data.local.dao

/**
 * Everything a message-list row renders, WITHOUT the body columns. List screens (Inbox,
 * Sent/Drafts/Trash, Search) observe these instead of full [rs.tapizlabs.mail.data.local.entity.MessageEntity]
 * rows: `bodyPlain`/`bodyHtml` are routinely tens or hundreds of KB each, so `SELECT *` for a
 * whole list both copied megabytes through the CursorWindow on every Room invalidation and
 * could fail outright once a single row outgrew the window (~2 MB).
 */
data class MessageListRow(
    val id: String,
    val accountId: String,
    val folderId: String,
    val fromName: String,
    val fromAddress: String,
    val toAddresses: String,
    val subject: String,
    val snippet: String,
    val sentAt: Long,
    val isRead: Boolean,
    val isStarred: Boolean,
    val hasAttachments: Boolean,
    val categoryId: String?,
    val isSynced: Boolean,
)

/** Badge counts for the fixed Inbox/Drafts/Trash chips, computed in SQL in one pass. */
data class MailboxCounts(
    val inboxUnread: Int = 0,
    val drafts: Int = 0,
    val trash: Int = 0,
)

/** Unread count per user category (see [MessageDao.getCategoryUnreadCounts]). */
data class CategoryCount(
    val categoryId: String,
    val count: Int,
)

/** The locally cached flag state of one synced message, used to reconcile `\Seen`/`\Flagged`
 * and server-side deletions against the IMAP mailbox during sync. */
data class LocalFlagRow(
    val id: String,
    val uid: Long,
    val folderId: String,
    val isRead: Boolean,
    val isStarred: Boolean,
)

/** One message's id plus a single text column (or a prefix of one) — for the maintenance
 * queries that repair text derived by older versions (see [MessageDao.getHtmlOnlyWithoutText],
 * [MessageDao.getUncollapsedSnippets]). */
data class MessageTextRow(
    val id: String,
    val text: String,
)

/** The columns category rules can match on, for re-categorizing cached mail. */
data class CategorizableRow(
    val id: String,
    val fromName: String,
    val fromAddress: String,
    val subject: String,
    val bodyHead: String,
    val categoryId: String?,
)
