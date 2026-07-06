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

    /** Appends [mimeMessage] to [folderInfo]'s remote mailbox (typically the account's Sent
     * folder) and flags it `\Seen` — mirrors what a webmail client does after a successful
     * SMTP send. Plain SMTP delivery alone does NOT put a copy in the sender's own Sent
     * folder; some providers (Gmail) append it server-side automatically as part of
     * accepting the send, but others (UNS's IMAP server, observed directly) do not, so a
     * message sent through this app's SMTP-only path would silently never show up in Sent
     * unless the client does this append itself. Opens READ_WRITE since `appendMessages`
     * requires write access; closes without expunge (a plain append never marks anything
     * `\Deleted`). */
    fun appendToSentFolder(store: IMAPStore, folderInfo: FolderInfo, mimeMessage: MimeMessage) {
        val folder = store.getFolder(folderInfo.remoteName) as IMAPFolder
        try {
            mimeMessage.setFlag(Flags.Flag.SEEN, true)
            folder.appendMessages(arrayOf(mimeMessage))
        } catch (e: MessagingException) {
            throw MailError.FolderUnavailable(folderInfo.remoteName, e)
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

    /** Fetches up to [limit] messages older than [oldestKnownUid], for "load more" as the user
     * scrolls to the bottom of the Inbox list — the initial sync only pulls the newest
     * [INITIAL_SYNC_LIMIT] messages (see [fetchNewMessages]'s no-`sinceUid` branch), so this is
     * how the rest of a large mailbox becomes reachable without a slow/expensive full backfill
     * on first setup.
     *
     * Internally converts [oldestKnownUid] to its IMAP sequence number (`Message.getMessageNumber()`)
     * and walks backwards from there by sequence — sequence number (1 = oldest in the mailbox,
     * [IMAPFolder.getMessageCount] = newest) is what directly expresses "the N messages before
     * this position"; UID ordering matches sequence order but isn't guaranteed contiguous/dense,
     * so it can't be used to compute a fixed-size older page the same way.
     *
     * @param oldestKnownUid the UID of the oldest message already cached locally (from Room —
     * callers already track this per folder, so this avoids making them compute/track a
     * sequence number of their own). Pass `null` if the folder has no cached messages yet
     * (returns the newest [limit] instead, same as first sync). Returns an empty list once
     * the oldest cached message is already sequence number 1 (nothing older left on the
     * server) — callers should treat that as "no more pages" for this folder. */
    fun fetchOlderMessages(
        store: IMAPStore,
        folderInfo: FolderInfo,
        oldestKnownUid: Long?,
        limit: Int,
    ): List<ParsedMessage> {
        val folder = store.getFolder(folderInfo.remoteName) as IMAPFolder
        try {
            folder.open(Folder.READ_ONLY)
            val beforeSeqNum = if (oldestKnownUid != null) {
                val oldestMessage = folder.getMessageByUID(oldestKnownUid) ?: return emptyList()
                oldestMessage.messageNumber
            } else {
                folder.messageCount + 1
            }
            if (beforeSeqNum <= 1) return emptyList()
            val start = maxOf(1, beforeSeqNum - limit)
            val end = beforeSeqNum - 1
            val messages = folder.getMessages(start, end)
            if (messages.isEmpty()) return emptyList()

            val fetchProfile = FetchProfile().apply {
                add(FetchProfile.Item.ENVELOPE)
                add(FetchProfile.Item.FLAGS)
                add(javax.mail.UIDFolder.FetchProfileItem.UID)
            }
            folder.fetch(messages, fetchProfile)

            return messages.mapNotNull { msg ->
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

    /** Flips the `\Seen` flag on the server for one message so other IMAP clients/webmail
     * agree with this app's local read/unread state. Opens the folder [Folder.READ_WRITE] —
     * unlike [fetchNewMessages]'s `READ_ONLY` peek — since flag mutation requires write
     * access; closes without expunging (`close(false)`) since a flag change alone should
     * never trigger message removal. */
    fun setMessageSeen(store: IMAPStore, folderInfo: FolderInfo, uid: Long, seen: Boolean) {
        val folder = store.getFolder(folderInfo.remoteName) as IMAPFolder
        try {
            folder.open(Folder.READ_WRITE)
            val msg = folder.getMessageByUID(uid)
                ?: throw MailError.Unknown(IllegalStateException("Message uid=$uid not found"))
            msg.setFlag(Flags.Flag.SEEN, seen)
        } catch (e: MessagingException) {
            throw MailError.FolderUnavailable(folderInfo.remoteName, e)
        } finally {
            if (folder.isOpen) runCatching { folder.close(false) }
        }
    }

    /** Flips the `\Flagged` flag on the server for one message — the IMAP-side counterpart
     * of the app's local star toggle, so other IMAP clients/webmail see the same starred
     * state. Same READ_WRITE-open/close(false) recipe as [setMessageSeen]. */
    fun setMessageFlagged(store: IMAPStore, folderInfo: FolderInfo, uid: Long, flagged: Boolean) {
        val folder = store.getFolder(folderInfo.remoteName) as IMAPFolder
        try {
            folder.open(Folder.READ_WRITE)
            val msg = folder.getMessageByUID(uid)
                ?: throw MailError.Unknown(IllegalStateException("Message uid=$uid not found"))
            msg.setFlag(Flags.Flag.FLAGGED, flagged)
        } catch (e: MessagingException) {
            throw MailError.FolderUnavailable(folderInfo.remoteName, e)
        } finally {
            if (folder.isOpen) runCatching { folder.close(false) }
        }
    }

    /** Marks a message `\Deleted` and expunges it so it is physically removed from the
     * server mailbox — the IMAP-side counterpart of the app's local "permanently delete
     * from Trash" action. `close(true)` expunges every `\Deleted`-flagged message in the
     * folder on close, which is exactly the one message we just flagged here. */
    fun deleteMessagePermanently(store: IMAPStore, folderInfo: FolderInfo, uid: Long) {
        val folder = store.getFolder(folderInfo.remoteName) as IMAPFolder
        try {
            folder.open(Folder.READ_WRITE)
            val msg = folder.getMessageByUID(uid)
                ?: throw MailError.Unknown(IllegalStateException("Message uid=$uid not found"))
            msg.setFlag(Flags.Flag.DELETED, true)
        } catch (e: MessagingException) {
            throw MailError.FolderUnavailable(folderInfo.remoteName, e)
        } finally {
            if (folder.isOpen) runCatching { folder.close(true) }
        }
    }

    /** Batch version of [deleteMessagePermanently] — flags every message in [uids] `\Deleted`
     * on a single opened folder, then expunges once on close, instead of one connect/open/
     * close round-trip per message (what "Empty trash" used to do, one IMAP connection per
     * message, before this existed). Used for bulk actions like "Empty trash" where all the
     * messages live in the same real IMAP folder. Missing UIDs (e.g. already gone server-side)
     * are skipped rather than aborting the whole batch. */
    fun deleteMessagesPermanently(store: IMAPStore, folderInfo: FolderInfo, uids: List<Long>) {
        if (uids.isEmpty()) return
        val folder = store.getFolder(folderInfo.remoteName) as IMAPFolder
        try {
            folder.open(Folder.READ_WRITE)
            uids.forEach { uid ->
                runCatching { folder.getMessageByUID(uid)?.setFlag(Flags.Flag.DELETED, true) }
            }
        } catch (e: MessagingException) {
            throw MailError.FolderUnavailable(folderInfo.remoteName, e)
        } finally {
            if (folder.isOpen) runCatching { folder.close(true) }
        }
    }

    companion object {
        private const val SNIPPET_LENGTH = 160
        private const val INITIAL_SYNC_LIMIT = 25
        /** Page size for [fetchOlderMessages] — matches [INITIAL_SYNC_LIMIT] so scrolling to
         * the bottom of the Inbox always loads another same-sized batch of older mail. */
        const val OLDER_PAGE_SIZE = 25
        private const val INITIAL_BACKOFF_MS = 5_000L
        private const val MAX_BACKOFF_MS = 5 * 60_000L
    }
}
