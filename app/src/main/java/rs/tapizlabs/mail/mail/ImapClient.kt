package rs.tapizlabs.mail.mail

import android.content.Context
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.mail.AuthenticationFailedException
import javax.mail.FetchProfile
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.local.entity.ConnectionSecurity
import rs.tapizlabs.mail.data.local.entity.FolderType

/**
 * Thin wrapper around `com.sun.mail.imap.IMAPStore`/`IMAPFolder`. All blocking network I/O
 * here must be called from a background dispatcher by the caller (repository/worker/service) —
 * this class does not switch dispatchers itself except in [idle], which owns its own loop.
 */
@Singleton
class ImapClient @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    /** Opens and authenticates an [IMAPStore] for [account]. Caller owns the returned
     * store's lifecycle (must call `close()` when done, including in fetch-then-disconnect
     * flows like [MailSyncWorker] — only [idle] keeps a store open long-term). */
    fun connect(account: AccountEntity, password: String): IMAPStore {
        try {
            val session = MailSession.imapSession(account)
            val store = session.getStore(
                if (account.imapSecurity == ConnectionSecurity.SSL_TLS) "imaps" else "imap",
            ) as IMAPStore
            store.connect(account.imapHost, account.imapPort, account.username, password)
            return store
        } catch (e: AuthenticationFailedException) {
            throw MailError.AuthenticationFailed(e)
        } catch (e: MessagingException) {
            throw MailError.ConnectionFailed(e)
        }
    }

    /** Verifies host/port/security/credentials without leaving a connection open —
     * used by the Add-Account flow before it persists anything. */
    suspend fun testConnection(account: AccountEntity, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val store = connect(account, password)
                store.close()
                Result.success(Unit)
            } catch (e: MailError) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(MailError.Unknown(e))
            }
        }

    fun listFolders(store: IMAPStore): List<FolderInfo> {
        return try {
            store.defaultFolder.list("*")
                .filter { (it.type and Folder.HOLDS_MESSAGES) != 0 }
                .map { FolderInfo(it.fullName, it.name, guessFolderType(it.fullName)) }
        } catch (e: MessagingException) {
            throw MailError.Unknown(e)
        }
    }

    private fun guessFolderType(fullName: String): FolderType {
        val lower = fullName.lowercase()
        return when {
            lower == "inbox" -> FolderType.INBOX
            lower.contains("sent") -> FolderType.SENT
            lower.contains("draft") -> FolderType.DRAFTS
            lower.contains("trash") || lower.contains("deleted") -> FolderType.TRASH
            else -> FolderType.CUSTOM
        }
    }

    /** Fetches messages with UID greater than [sinceUid] (or the whole folder, capped, on
     * first sync when [sinceUid] is null). Uses a [FetchProfile] to pull envelope+flags in
     * one round-trip, then parses body/attachments per message — the parse itself still
     * requires opening each message's content, so this is the "peek and parse" pass; heavy
     * attachment bytes are NOT downloaded here (see [downloadAttachment]). */
    fun fetchNewMessages(
        store: IMAPStore,
        folderInfo: FolderInfo,
        sinceUid: Long?,
    ): List<ParsedMessage> {
        val folder = store.getFolder(folderInfo.remoteName) as IMAPFolder
        try {
            folder.open(Folder.READ_ONLY)
            val allUidsMessages: Array<Message> = if (sinceUid != null) {
                val highest = folder.uidNext - 1
                if (highest <= sinceUid) {
                    emptyArray()
                } else {
                    // UIDFolder.LASTUID (-1) tells the server "up through the highest UID
                    // currently in the mailbox" without us needing to know it in advance.
                    folder.getMessagesByUID(sinceUid + 1, javax.mail.UIDFolder.LASTUID)
                }
            } else {
                // First sync for this folder: cap to the most recent N to avoid pulling a
                // decade of mail on initial setup — full backfill is a future "load more".
                val count = folder.messageCount
                val start = maxOf(1, count - INITIAL_SYNC_LIMIT + 1)
                if (count == 0) emptyArray() else folder.getMessages(start, count)
            }

            if (allUidsMessages.isEmpty()) return emptyList()

            val fetchProfile = FetchProfile().apply {
                add(FetchProfile.Item.ENVELOPE)
                add(FetchProfile.Item.FLAGS)
                add(javax.mail.UIDFolder.FetchProfileItem.UID)
            }
            folder.fetch(allUidsMessages, fetchProfile)

            return allUidsMessages.mapNotNull { msg ->
                runCatching { parseMessage(folder, msg as MimeMessage) }.getOrNull()
            }
        } catch (e: MessagingException) {
            throw MailError.FolderUnavailable(folderInfo.remoteName, e)
        } finally {
            if (folder.isOpen) runCatching { folder.close(false) }
        }
    }

    private fun parseMessage(folder: IMAPFolder, msg: MimeMessage): ParsedMessage {
        val uid = folder.getUID(msg)
        val (plain, html, attachments) = MimePartWalker.extractBody(msg)
        val fromAddr = (msg.from?.firstOrNull() as? InternetAddress)
        return ParsedMessage(
            uid = uid,
            messageIdHeader = msg.getHeader("Message-ID")?.firstOrNull(),
            subject = runCatching { msg.subject }.getOrNull(),
            fromAddress = fromAddr?.address,
            fromName = fromAddr?.personal,
            toAddresses = addressesOf(msg, Message.RecipientType.TO),
            ccAddresses = addressesOf(msg, Message.RecipientType.CC),
            sentAt = (msg.sentDate ?: msg.receivedDate)?.time ?: System.currentTimeMillis(),
            isRead = msg.isSet(Flags.Flag.SEEN),
            isStarred = msg.isSet(Flags.Flag.FLAGGED),
            snippet = plain?.take(SNIPPET_LENGTH)?.replace("\n", " ")?.trim(),
            bodyPlain = plain,
            bodyHtml = html,
            attachments = attachments,
        )
    }

    private fun addressesOf(msg: MimeMessage, type: Message.RecipientType): List<String> =
        runCatching {
            msg.getRecipients(type)?.mapNotNull { (it as? InternetAddress)?.address } ?: emptyList()
        }.getOrDefault(emptyList())

    /** Long-lived `IMAPFolder.idle()` loop for providers that support it. Runs on [scope]
     * (expected to be the foreground [rs.tapizlabs.mail.sync.IdleSyncService]'s lifecycle
     * scope, which is itself bounded by the service's own background timeout — this
     * function does not decide when to stop, only how to survive transient drops while
     * running). Reconnects with capped exponential backoff on `FolderClosedException`/
     * `StoreClosedException` so a flaky network doesn't spin-loop and burn battery/CPU. */
    fun idle(
        scope: CoroutineScope,
        store: IMAPStore,
        folder: IMAPFolder,
        onNewMail: () -> Unit,
    ): Job = scope.launch(Dispatchers.IO) {
        var backoffMs = INITIAL_BACKOFF_MS
        while (isActive) {
            try {
                if (!folder.isOpen) folder.open(Folder.READ_ONLY)
                backoffMs = INITIAL_BACKOFF_MS
                folder.idle()
                // idle() returns when the server pushes an event (new mail, flag change,
                // expunge) or the connection drops; either way we notify and loop back in.
                onNewMail()
            } catch (e: javax.mail.FolderClosedException) {
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            } catch (e: javax.mail.StoreClosedException) {
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                runCatching { if (!store.isConnected) store.connect() }
            } catch (e: MessagingException) {
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            }
        }
    }

    /** Downloads one attachment's bytes into the app-private cache dir, keyed by message
     * UID + part index so repeat downloads of the same attachment overwrite rather than
     * duplicate. Returns the resulting [File]; the FileProvider authority for sharing it
     * is already declared in the manifest (`res/xml/file_paths.xml`, cache-path
     * "attachments"). */
    fun downloadAttachment(
        store: IMAPStore,
        folderInfo: FolderInfo,
        messageUid: Long,
        attachment: ParsedAttachment,
    ): File {
        val folder = store.getFolder(folderInfo.remoteName) as IMAPFolder
        try {
            folder.open(Folder.READ_ONLY)
            val msg = folder.getMessageByUID(messageUid) as? MimeMessage
                ?: throw MailError.Unknown(IllegalStateException("Message uid=$messageUid not found"))
            val part = MimePartWalker.findAttachmentPart(msg, attachment.partIndex)
                ?: throw MailError.Unknown(IllegalStateException("Attachment part not found"))

            val dir = File(appContext.cacheDir, "attachments").apply { mkdirs() }
            val outFile = File(dir, "${messageUid}_${attachment.partIndex}_${attachment.fileName}")
            part.inputStream.use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
            return outFile
        } catch (e: MessagingException) {
            throw MailError.Unknown(e)
        } finally {
            if (folder.isOpen) runCatching { folder.close(false) }
        }
    }

    companion object {
        private const val SNIPPET_LENGTH = 160
        private const val INITIAL_SYNC_LIMIT = 50
        private const val INITIAL_BACKOFF_MS = 5_000L
        private const val MAX_BACKOFF_MS = 5 * 60_000L
    }
}
