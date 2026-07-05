package rs.tapizlabs.mail.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** Action triggered by a Gmail-style list-item swipe gesture. */
enum class SwipeAction {
    ARCHIVE,
    DELETE,
    MARK_READ,
    MARK_UNREAD,
    NONE,
}

/**
 * Per-account swipe-action configuration for the message list.
 *
 * One row per account (keyed by [accountId], enforced as the primary key) rather than a single
 * global row: swipe behavior is a per-mailbox preference in Gmail-style clients (e.g. a user may
 * want swipe-to-archive on a Gmail account but swipe-to-delete on a university account that has
 * no Archive folder). The sync/UI layer falls back to a sensible default when no row exists yet
 * for a given account.
 */
@Entity(
    tableName = "swipe_action_configs",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SwipeActionConfigEntity(
    @PrimaryKey val accountId: String,
    val swipeLeftAction: SwipeAction,
    val swipeRightAction: SwipeAction,
)
