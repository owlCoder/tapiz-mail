package rs.tapizlabs.mail.mail

import rs.tapizlabs.mail.data.local.entity.FolderType

/**
 * Protocol-layer DTOs. Deliberately decoupled from the Room entities (`MessageEntity`,
 * `AttachmentEntity`, `FolderEntity`) — mapping between these and Room happens in the
 * repository layer, not here, so this package has zero Room/DB dependency and stays
 * independently testable against a fake/embedded IMAP server.
 */

data class FolderInfo(
    val remoteName: String,
    val displayName: String,
    val type: FolderType,
)

data class ParsedAttachment(
    /** Index of this body part within the message's MimeMultipart structure, used to
     * re-fetch/download the part on demand instead of eagerly pulling attachment bytes
     * during the lightweight envelope/snippet sync pass. */
    val partIndex: Int,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val contentId: String?,
)

data class ParsedMessage(
    val uid: Long,
    val messageIdHeader: String?,
    val subject: String?,
    val fromAddress: String?,
    val fromName: String?,
    val toAddresses: List<String>,
    val ccAddresses: List<String>,
    val sentAt: Long,
    val isRead: Boolean,
    val isStarred: Boolean,
    val snippet: String?,
    val bodyPlain: String?,
    val bodyHtml: String?,
    val attachments: List<ParsedAttachment>,
)

/** Typed connection/fetch failures so callers (Add-Account flow, sync worker, IDLE
 * service) can react without catching raw checked `MessagingException`s everywhere. */
sealed class MailError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class AuthenticationFailed(cause: Throwable) :
        MailError("Authentication failed: ${cause.reasonSuffix()}", cause)
    class ConnectionFailed(cause: Throwable) :
        MailError("Could not connect to server: ${cause.reasonSuffix()}", cause)
    class FolderUnavailable(folderName: String, cause: Throwable) :
        MailError("Folder unavailable: $folderName (${cause.reasonSuffix()})", cause)
    class Unknown(cause: Throwable) : MailError(cause.message ?: "Unknown mail error", cause)
}

/** Appends the underlying exception's own class + message (e.g. "UnknownHostException:
 * webmail.uns.ac.rs" or "SocketTimeoutException: connect timed out") so the generic
 * [MailError] wrapper messages shown in the UI (Add-Account "Test connection" failure,
 * sync errors) actually say WHY a connection failed instead of just that it failed. */
private fun Throwable.reasonSuffix(): String {
    val detail = message?.takeIf { it.isNotBlank() } ?: javaClass.simpleName
    return "${javaClass.simpleName}: $detail"
}
