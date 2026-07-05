package rs.tapizlabs.mail.mail

import android.content.ContentResolver
import android.net.Uri
import javax.mail.AuthenticationFailedException
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.local.entity.ConnectionSecurity

/** A local file/content attachment to include on an outgoing message, referenced by
 * content [uri] (e.g. from the system file/photo picker) rather than bytes already in
 * memory, so large attachments aren't held twice. */
data class OutgoingAttachment(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
)

data class OutgoingMessage(
    val to: List<String>,
    val cc: List<String> = emptyList(),
    val bcc: List<String> = emptyList(),
    val subject: String,
    val bodyPlain: String,
    val bodyHtml: String? = null,
    val attachments: List<OutgoingAttachment> = emptyList(),
)

/**
 * Wraps outgoing mail via `javax.mail.Transport`. Builds a `MimeMultipart` with a text
 * part (plain, optionally alternative HTML) plus one `MimeBodyPart` per attachment, streamed
 * from the caller's `ContentResolver` rather than read fully into a `ByteArray` first.
 */
@Singleton
class SmtpClient @Inject constructor(
    private val contentResolver: ContentResolver,
) {

    suspend fun send(
        account: AccountEntity,
        password: String,
        message: OutgoingMessage,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = MailSession.smtpSession(account)
            val mimeMessage = buildMimeMessage(session, account, message)
            val transport = session.getTransport(
                if (account.smtpSecurity == ConnectionSecurity.SSL_TLS) "smtps" else "smtp",
            )
            try {
                transport.connect(account.smtpHost, account.smtpPort, account.username, password)
                transport.sendMessage(mimeMessage, mimeMessage.allRecipients)
            } finally {
                runCatching { transport.close() }
            }
            Result.success(Unit)
        } catch (e: AuthenticationFailedException) {
            Result.failure(MailError.AuthenticationFailed(e))
        } catch (e: MessagingException) {
            Result.failure(MailError.ConnectionFailed(e))
        } catch (e: Exception) {
            Result.failure(MailError.Unknown(e))
        }
    }

    private fun buildMimeMessage(
        session: Session,
        account: AccountEntity,
        message: OutgoingMessage,
    ): MimeMessage = MimeMessage(session).apply {
        setFrom(InternetAddress(account.emailAddress, account.displayName))
        setRecipients(javax.mail.Message.RecipientType.TO, message.to.toAddressArray())
        if (message.cc.isNotEmpty()) {
            setRecipients(javax.mail.Message.RecipientType.CC, message.cc.toAddressArray())
        }
        if (message.bcc.isNotEmpty()) {
            setRecipients(javax.mail.Message.RecipientType.BCC, message.bcc.toAddressArray())
        }
        subject = message.subject
        setContent(buildContent(message))
    }

    private fun buildContent(message: OutgoingMessage): Multipart {
        val root = MimeMultipart("mixed")
        root.addBodyPart(buildTextPart(message))
        message.attachments.forEach { root.addBodyPart(buildAttachmentPart(it)) }
        return root
    }

    /** `multipart/alternative` when an HTML body is supplied (plain first as the
     * lowest-fidelity fallback, per MIME convention), otherwise a single plain-text part. */
    private fun buildTextPart(message: OutgoingMessage): MimeBodyPart {
        val html = message.bodyHtml
        if (html == null) {
            return MimeBodyPart().apply { setText(message.bodyPlain, "utf-8") }
        }
        val alt = MimeMultipart("alternative")
        alt.addBodyPart(MimeBodyPart().apply { setText(message.bodyPlain, "utf-8") })
        alt.addBodyPart(MimeBodyPart().apply { setContent(html, "text/html; charset=utf-8") })
        return MimeBodyPart().apply { setContent(alt) }
    }

    private fun buildAttachmentPart(attachment: OutgoingAttachment): MimeBodyPart {
        val part = MimeBodyPart()
        val bytes = contentResolver.openInputStream(attachment.uri)?.use { it.readBytes() }
            ?: throw MailError.Unknown(IllegalStateException("Cannot open ${attachment.uri}"))
        part.setContent(bytes, attachment.mimeType)
        part.fileName = attachment.fileName
        part.setDisposition(javax.mail.Part.ATTACHMENT)
        return part
    }

    private fun List<String>.toAddressArray(): Array<javax.mail.Address> =
        map { InternetAddress(it) as javax.mail.Address }.toTypedArray()
}
