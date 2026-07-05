package rs.tapizlabs.mail.data.repository

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import rs.tapizlabs.mail.data.local.dao.AccountDao
import rs.tapizlabs.mail.mail.OutgoingAttachment
import rs.tapizlabs.mail.mail.OutgoingMessage
import rs.tapizlabs.mail.mail.SmtpClient
import rs.tapizlabs.mail.security.CredentialStore

/**
 * Network-facing operations the UI layer needs (pull-to-refresh, send mail). Defined as an
 * interface so Inbox/Compose ViewModels depend on a stable contract rather than the concrete
 * IMAP/SMTP-backed implementation below.
 */
interface MailSyncGateway {
    /** Triggers a fetch of new messages for [accountId] (or all active accounts if null). Suspends until the fetch completes or fails. */
    suspend fun refresh(accountId: String? = null): Result<Unit>

    /** Sends a composed message. Attachment URIs are content:// Uris from the picker; the
     * implementation is responsible for reading/streaming their bytes into the MIME body. */
    suspend fun sendMessage(
        accountId: String,
        to: List<String>,
        cc: List<String>,
        bcc: List<String>,
        subject: String,
        bodyPlain: String,
        attachmentUris: List<String>,
        inReplyToMessageId: String?,
    ): Result<Unit>
}

/**
 * Real [MailSyncGateway] backed by [SyncRepository] (IMAP fetch/upsert) and [SmtpClient]
 * (send). Bound in [rs.tapizlabs.mail.di.RepositoryModule] in place of the placeholder
 * `NoOpMailSyncGateway` that existed before this protocol layer landed.
 */
@Singleton
class DefaultMailSyncGateway @Inject constructor(
    private val syncRepository: SyncRepository,
    private val smtpClient: SmtpClient,
    private val accountDao: AccountDao,
    private val credentialStore: CredentialStore,
) : MailSyncGateway {

    override suspend fun refresh(accountId: String?): Result<Unit> = try {
        val ids = if (accountId != null) {
            listOf(accountId)
        } else {
            accountDao.getActiveAccounts().first().map { it.id }
        }
        ids.forEach { syncRepository.syncAccount(it) }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun sendMessage(
        accountId: String,
        to: List<String>,
        cc: List<String>,
        bcc: List<String>,
        subject: String,
        bodyPlain: String,
        attachmentUris: List<String>,
        inReplyToMessageId: String?,
    ): Result<Unit> {
        val account = accountDao.getAccountOnce(accountId)
            ?: return Result.failure(IllegalArgumentException("Unknown account $accountId"))
        val password = credentialStore.getSmtpPassword(accountId)
            ?: return Result.failure(IllegalStateException("No SMTP credentials for $accountId"))

        val message = OutgoingMessage(
            to = to,
            cc = cc,
            bcc = bcc,
            subject = subject,
            bodyPlain = bodyPlain,
            attachments = attachmentUris.map { uriString ->
                val uri = Uri.parse(uriString)
                OutgoingAttachment(uri = uri, fileName = uri.lastPathSegment ?: "attachment", mimeType = "application/octet-stream")
            },
        )
        return smtpClient.send(account, password, message)
    }
}
