package rs.tapizlabs.mail.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Transport security mode for an IMAP/SMTP connection. */
enum class ConnectionSecurity {
    SSL_TLS,
    STARTTLS,
    NONE,
}

/**
 * A configured mail account (Gmail, Outlook, custom/UNS webmail, ...).
 *
 * Passwords are intentionally NOT stored here — they live only in
 * [rs.tapizlabs.mail.security.CredentialStore], keyed by [id]. This entity only holds
 * connection metadata + the username needed to authenticate.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val emailAddress: String,
    val imapHost: String,
    val imapPort: Int,
    val imapSecurity: ConnectionSecurity,
    val smtpHost: String,
    val smtpPort: Int,
    val smtpSecurity: ConnectionSecurity,
    val username: String,
    val syncIntervalMinutes: Int,
    val supportsIdle: Boolean,
    val isActive: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
)
