package rs.tapizlabs.mail.data.repository

import android.net.Uri
import com.sun.mail.imap.IMAPStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import rs.tapizlabs.mail.data.local.dao.AccountDao
import rs.tapizlabs.mail.data.local.dao.AttachmentDao
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

    /** Batch counterpart of [deleteMessageRemote] — resolves every message in [messageIds] to
     * its real IMAP folder+UID, groups them by folder, and deletes each folder's group with a
     * single connection + one expunge (see [ImapClient.deleteMessagesPermanently]) instead of
     * one connect/delete/disconnect round-trip per message. Used by "Empty trash" so clearing
     * dozens of messages doesn't open dozens of IMAP connections one after another. Best-effort
     * per message, same as [deleteMessageRemote] — an unresolvable/already-gone message is
     * skipped rather than aborting the whole batch. */
    suspend fun deleteMessagesRemote(messageIds: List<String>): Result<Unit>

    /** Downloads [attachmentId]'s bytes on demand (envelope sync never fetches attachment
     * bytes eagerly — see [ImapClient.fetchNewMessages]'s doc) and records the resulting
     * local file's `content://` URI in Room ([rs.tapizlabs.mail.data.local.dao.AttachmentDao.setLocalUri])
     * so subsequent opens/saves reuse the cached file instead of re-downloading. Returns the
     * `content://` URI string on success. */
    suspend fun downloadAttachment(attachmentId: String): Result<String>
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
    private val attachmentDao: AttachmentDao,
    private val contentResolver: android.content.ContentResolver,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
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

    override suspend fun deleteMessagesRemote(messageIds: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val messages = messageIds.mapNotNull { messageDao.getMessageOnce(it) }
                // Group by (account, real IMAP folder) so each group needs only one connection
                // + one expunge, regardless of how many messages it contains.
                val groups = messages
                    .filter { !(it.originFolderId ?: it.folderId).startsWith(LOCAL_FOLDER_ID_PREFIX) }
                    .groupBy { it.accountId to (it.originFolderId ?: it.folderId) }

                groups.forEach { (accountAndFolder, groupMessages) ->
                    val (accountId, folderId) = accountAndFolder
                    val account = accountDao.getAccountOnce(accountId) ?: return@forEach
                    val password = credentialStore.getImapPassword(accountId) ?: return@forEach
                    val folder = folderDao.getFolderOnce(folderId) ?: return@forEach

                    val store = imapClient.connect(account, password)
                    try {
                        val folderInfo = FolderInfo(folder.remoteName, folder.displayName, folder.type)
                        imapClient.deleteMessagesPermanently(store, folderInfo, groupMessages.map { it.uid })
                    } finally {
                        runCatching { store.close() }
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun downloadAttachment(attachmentId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val attachment = attachmentDao.getAttachmentOnce(attachmentId)
                    ?: return@withContext Result.failure(IllegalArgumentException("Unknown attachment $attachmentId"))
                attachment.localUri?.let { return@withContext Result.success(it) }

                val message = messageDao.getMessageOnce(attachment.messageId)
                    ?: return@withContext Result.failure(IllegalStateException("Unknown message ${attachment.messageId}"))
                // Attachments only ever belong to a real IMAP-synced message (drafts/local-only
                // messages have no attachment rows), so folderId always resolves to a real
                // mailbox here — no originFolderId fallback needed like the Trash case.
                val folder = folderDao.getFolderOnce(message.folderId)
                    ?: return@withContext Result.failure(IllegalStateException("Unknown folder ${message.folderId}"))
                val account = accountDao.getAccountOnce(message.accountId)
                    ?: return@withContext Result.failure(IllegalStateException("Unknown account ${message.accountId}"))
                val password = credentialStore.getImapPassword(message.accountId)
                    ?: return@withContext Result.failure(IllegalStateException("No IMAP credentials for ${message.accountId}"))

                val store = imapClient.connect(account, password)
                val file = try {
                    val folderInfo = FolderInfo(folder.remoteName, folder.displayName, folder.type)
                    val parsedAttachment = rs.tapizlabs.mail.mail.ParsedAttachment(
                        partIndex = attachment.partIndex,
                        fileName = attachment.fileName,
                        mimeType = attachment.mimeType,
                        sizeBytes = attachment.sizeBytes,
                        contentId = attachment.contentId,
                    )
                    imapClient.downloadAttachment(store, folderInfo, message.uid, parsedAttachment)
                } finally {
                    runCatching { store.close() }
                }

                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    file,
                ).toString()
                attachmentDao.setLocalUri(attachmentId, contentUri)
                Result.success(contentUri)
            } catch (e: Exception) {
                Result.failure(e)
            }
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
