package rs.tapizlabs.mail.data.repository

import android.net.Uri
import com.sun.mail.imap.IMAPStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rs.tapizlabs.mail.data.local.dao.AccountDao
import rs.tapizlabs.mail.data.local.dao.FolderDao
import rs.tapizlabs.mail.data.local.dao.MessageDao
import rs.tapizlabs.mail.data.local.entity.FolderType
import rs.tapizlabs.mail.data.local.entity.MessageEntity
import rs.tapizlabs.mail.mail.FolderInfo
import rs.tapizlabs.mail.mail.ImapClient
import rs.tapizlabs.mail.mail.OutgoingAttachment
import rs.tapizlabs.mail.mail.OutgoingMessage
import rs.tapizlabs.mail.mail.SmtpClient
import rs.tapizlabs.mail.security.CredentialStore

/** Local-only folder id prefix (Drafts/Trash pseudo-folders) — same constant as
 * [SyncRepository]'s `LOCAL_FOLDER_ID_PREFIX`; duplicated here (rather than shared) since it's
 * a one-line literal and importing across these two files isn't worth the coupling. */
private const val LOCAL_FOLDER_ID_PREFIX = "local-"

/** A picked attachment ready to send: [uri] is the content:// Uri to stream bytes from,
 * [displayName] is the real file name (as shown in Compose's attachment chip via
 * `queryDisplayName`/`OpenDocument` picker) — NOT derived from [uri]'s last path segment,
 * which for content:// Uris is often an opaque id (e.g. "19493") rather than a real
 * filename, and is what recipients used to see instead of the actual file name/extension. */
data class OutgoingAttachmentRef(val uri: String, val displayName: String)

/**
 * Network-facing operations the UI layer needs (pull-to-refresh, send mail). Defined as an
 * interface so Inbox/Compose ViewModels depend on a stable contract rather than the concrete
 * IMAP/SMTP-backed implementation below.
 */
interface MailSyncGateway {
    /** Triggers a fetch of new messages for [accountId] (or all active accounts if null). Suspends until the fetch completes or fails. */
    suspend fun refresh(accountId: String? = null): Result<Unit>

    /** "Load more" older mail for one folder, triggered when the user scrolls to the bottom
     * of the Inbox list — see [SyncRepository.loadOlderMessages]. Returns the count of older
     * messages actually added; 0 means either a failure or that this folder has nothing
     * older left on the server, and callers should stop paging for it either way. */
    suspend fun loadOlderMessages(accountId: String, folderId: String): Int

    /** Sends a composed message. Attachment URIs are content:// Uris from the picker; the
     * implementation is responsible for reading/streaming their bytes into the MIME body. */
    suspend fun sendMessage(
        accountId: String,
        to: List<String>,
        cc: List<String>,
        bcc: List<String>,
        subject: String,
        bodyPlain: String,
        attachments: List<OutgoingAttachmentRef>,
        inReplyToMessageId: String?,
    ): Result<Unit>

    /** Best-effort IMAP-side `\Seen` flag mutation mirroring a local read/unread change — the
     * local Room write (via [MailRepository.setRead]) stays the source of truth for the UI, so
     * a failure here (offline, server down) must never be surfaced as blocking; callers should
     * fire-and-forget or log-and-ignore this result. No-ops (returns success) for messages
     * whose current folder has no IMAP counterpart (local-only Drafts/Trash) or that are
     * otherwise unresolvable to a server-side folder+UID. */
    suspend fun setMessageSeenRemote(messageId: String, isRead: Boolean): Result<Unit>

    /** Best-effort IMAP-side `\Flagged` mutation mirroring a local star toggle — same
     * best-effort contract as [setMessageSeenRemote]. */
    suspend fun setMessageStarredRemote(messageId: String, isStarred: Boolean): Result<Unit>

    /** Best-effort IMAP-side permanent delete (`\Deleted` flag + expunge) mirroring a local
     * "permanently delete from Trash" action — same best-effort contract as
     * [setMessageSeenRemote]: must be called while the Room row still exists (before the local
     * delete) so [MessageEntity.originFolderId]/accountId/uid are still available to resolve
     * the message's real IMAP folder. */
    suspend fun deleteMessageRemote(messageId: String): Result<Unit>
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
    private val imapClient: ImapClient,
    private val messageDao: MessageDao,
    private val folderDao: FolderDao,
    private val contentResolver: android.content.ContentResolver,
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

    override suspend fun loadOlderMessages(accountId: String, folderId: String): Int =
        runCatching { syncRepository.loadOlderMessages(accountId, folderId) }.getOrDefault(0)

    override suspend fun sendMessage(
        accountId: String,
        to: List<String>,
        cc: List<String>,
        bcc: List<String>,
        subject: String,
        bodyPlain: String,
        attachments: List<OutgoingAttachmentRef>,
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
            attachments = attachments.map { ref ->
                val uri = Uri.parse(ref.uri)
                OutgoingAttachment(
                    uri = uri,
                    // Real file name from the picker (Compose's queryDisplayName), not
                    // uri.lastPathSegment — for content:// Uris that's often an opaque id
                    // (e.g. "19493"/"document:19305") rather than the actual filename, which
                    // is what recipients used to see instead of a proper name + extension.
                    fileName = ref.displayName,
                    mimeType = contentResolver.getType(uri) ?: "application/octet-stream",
                )
            },
        )
        val sendResult = smtpClient.send(account, password, message)
        val sentMimeMessage = sendResult.getOrNull()
        if (sentMimeMessage != null) {
            // Best-effort: append a copy to this account's IMAP Sent folder. Plain SMTP
            // delivery alone does NOT do this — some providers (Gmail) add it server-side
            // as part of accepting the send, but others (UNS, observed directly: messages
            // sent through this app's SMTP-only path never appeared in Sent, while sending
            // the same account through UNS's own webmail did) require the client to append
            // it itself. A failure here must never turn an already-successful send into a
            // reported failure — the recipient already has the message either way.
            runCatching { appendToSent(accountId, sentMimeMessage) }
        }
        return sendResult.map { }
    }

    private suspend fun appendToSent(accountId: String, mimeMessage: javax.mail.internet.MimeMessage) =
        withContext(Dispatchers.IO) {
            val account = accountDao.getAccountOnce(accountId) ?: return@withContext
            val password = credentialStore.getImapPassword(accountId) ?: return@withContext
            val sentFolder = folderDao.getFolderOnceByType(accountId, FolderType.SENT) ?: return@withContext

            val store = imapClient.connect(account, password)
            try {
                val folderInfo = FolderInfo(sentFolder.remoteName, sentFolder.displayName, sentFolder.type)
                imapClient.appendToSentFolder(store, folderInfo, mimeMessage)
            } finally {
                runCatching { store.close() }
            }
        }

    override suspend fun setMessageSeenRemote(messageId: String, isRead: Boolean): Result<Unit> =
        withRemoteMessage(messageId) { store, folderInfo, uid ->
            imapClient.setMessageSeen(store, folderInfo, uid, isRead)
        }

    override suspend fun setMessageStarredRemote(messageId: String, isStarred: Boolean): Result<Unit> =
        withRemoteMessage(messageId) { store, folderInfo, uid ->
            imapClient.setMessageFlagged(store, folderInfo, uid, isStarred)
        }

    override suspend fun deleteMessageRemote(messageId: String): Result<Unit> =
        withRemoteMessage(messageId) { store, folderInfo, uid ->
            imapClient.deleteMessagePermanently(store, folderInfo, uid)
        }

    /** Resolves [messageId] to its real IMAP folder + UID, connects, runs [block], and always
     * disconnects — shared by [setMessageSeenRemote]/[deleteMessageRemote] so both best-effort
     * mutations follow the exact same "no-op if unresolvable, never throw past this method"
     * contract. Prefers [MessageEntity.originFolderId] (set when a message is moved into the
     * local-only Trash pseudo-folder — see [MailRepository.moveToTrash]) over [MessageEntity.folderId]
     * so messages already in local Trash still resolve to their real server-side mailbox
     * instead of the non-existent "local-trash-*" one. */
    private suspend fun withRemoteMessage(
        messageId: String,
        block: (store: IMAPStore, folderInfo: FolderInfo, uid: Long) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val message = messageDao.getMessageOnce(messageId)
                ?: return@withContext Result.success(Unit)
            val remoteFolderId = message.originFolderId ?: message.folderId
            if (remoteFolderId.startsWith(LOCAL_FOLDER_ID_PREFIX)) {
                // Never had (or no longer has) a real IMAP mailbox to mutate — e.g. a
                // local-only draft, or a Trash message whose origin somehow wasn't recorded.
                return@withContext Result.success(Unit)
            }
            val folder = folderDao.getFolderOnce(remoteFolderId)
                ?: return@withContext Result.success(Unit)
            val account = accountDao.getAccountOnce(message.accountId)
                ?: return@withContext Result.success(Unit)
            val password = credentialStore.getImapPassword(message.accountId)
                ?: return@withContext Result.success(Unit)

            val store = imapClient.connect(account, password)
            try {
                val folderInfo = FolderInfo(folder.remoteName, folder.displayName, folder.type)
                block(store, folderInfo, message.uid)
                Result.success(Unit)
            } finally {
                runCatching { store.close() }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
