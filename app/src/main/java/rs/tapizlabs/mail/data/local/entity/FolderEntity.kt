package rs.tapizlabs.mail.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Canonical role of a mail folder, independent of its provider-specific remote name. */
enum class FolderType {
    INBOX,
    SENT,
    DRAFTS,
    TRASH,
    ARCHIVE,
    CUSTOM,
}

/** A folder/mailbox synced from an [AccountEntity]'s IMAP server. */
@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId")],
)
data class FolderEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    /** Provider-specific mailbox name as seen over IMAP, e.g. "INBOX", "[Gmail]/Sent Mail". */
    val remoteName: String,
    val displayName: String,
    val type: FolderType,
    val unreadCount: Int,
)
