package rs.tapizlabs.mail.mail

import android.content.Context
import android.os.SystemClock
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.mail.AuthenticationFailedException
import javax.mail.FetchProfile
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.FolderClosedException
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.ReadOnlyFolderException
import javax.mail.StoreClosedException
import javax.mail.UIDFolder
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
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
 * this class does not switch dispatchers itself except in [idleLoop], which owns its own loop.
 */
@Singleton
class ImapClient @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    /** Opens and authenticates an [IMAPStore] for [account]. Caller owns the returned
     * store's lifecycle (must call `close()` when done, including in fetch-then-disconnect
     * flows like [rs.tapizlabs.mail.sync.MailSyncWorker] — only [idleLoop] keeps a store open
     * long-term, and it builds its own via [forIdle]). */
    fun connect(account: AccountEntity, password: String, forIdle: Boolean = false): IMAPStore {
        try {
            val session = MailSession.imapSession(account, forIdle)
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
        testConnectionWithIdleProbe(account, password).map { }

    /** Same handshake as [testConnection], but also reports whether the server advertises
     * the IMAP IDLE capability — used to decide [AccountEntity.supportsIdle] at save time
     * instead of hardcoding it based on provider. Custom/university IMAP servers (e.g. UNS
     * webmail) are not Gmail/Outlook but many still support IDLE; probing beats assuming. */
    suspend fun testConnectionWithIdleProbe(account: AccountEntity, password: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val store = connect(account, password)
                try {
                    Result.success(runCatching { store.hasCapability("IDLE") }.getOrDefault(false))
                } finally {
                    runCatching { store.close() }
                }
            } catch (e: MailError) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(MailError.Unknown(e))
            }
        }

    /** Lists the account's real, selectable mailboxes. Virtual "view" mailboxes (Gmail's
     * All Mail / Starred / Important — RFC 6154 `\All`/`\Flagged`/`\Important`) are left out:
     * they only re-expose messages that already live in Inbox/Sent/labels, so syncing them
     * showed every message two or three times. */
    fun listFolders(store: IMAPStore): List<FolderInfo> {
        return try {
            store.defaultFolder.list("*")
                .filter { (it.type and Folder.HOLDS_MESSAGES) != 0 }
                .mapNotNull { folder ->
                    val attributes = runCatching { (folder as IMAPFolder).attributes }
                        .getOrNull().orEmpty().map { it.lowercase() }
                    if (attributes.any { it in VIRTUAL_FOLDER_ATTRIBUTES }) return@mapNotNull null
                    FolderInfo(folder.fullName, folder.name, folderTypeOf(folder.fullName, folder.name, attributes))
                }
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
     * unless the client does this append itself. `appendMessages` works on a closed folder
     * (it issues a plain IMAP APPEND), so nothing is opened or expunged here. */
    fun appendToSentFolder(store: IMAPStore, folderInfo: FolderInfo, mimeMessage: MimeMessage) {
        val folder = store.getFolder(folderInfo.remoteName) as IMAPFolder
        try {
            mimeMessage.setFlag(Flags.Flag.SEEN, true)
            folder.appendMessages(arrayOf(mimeMessage))
        } catch (e: MessagingException) {
            throw MailError.FolderUnavailable(folderInfo.remoteName, e)
        }
    }

    /** Prefers the server's own RFC 6154 special-use attributes (they survive localized or
     * renamed mailboxes, e.g. Gmail's "[Gmail]/Послате"), falling back to the mailbox name. */
    private fun folderTypeOf(fullName: String, name: String, attributes: List<String>): FolderType {
        when {
            "\\sent" in attributes -> return FolderType.SENT
            "\\drafts" in attributes -> return FolderType.DRAFTS
            "\\trash" in attributes -> return FolderType.TRASH
            "\\junk" in attributes -> return FolderType.JUNK
        }
        val lower = name.lowercase()
        return when {
            fullName.equals("inbox", ignoreCase = true) -> FolderType.INBOX
            SENT_NAME_HINTS.any { it in lower } -> FolderType.SENT
            DRAFTS_NAME_HINTS.any { it in lower } -> FolderType.DRAFTS
            TRASH_NAME_HINTS.any { it in lower } || lower == "bin" -> FolderType.TRASH
            JUNK_NAME_HINTS.any { it in lower } -> FolderType.JUNK
            else -> FolderType.CUSTOM
        }
    }

    /**
     * One round of folder sync on a single opened mailbox: fetches messages with UID greater
     * than [sinceUid] (or the newest [INITIAL_SYNC_LIMIT] on first sync, when [sinceUid] is
     * null), and — when [reconcileFromUid] is given — reads the server's current flags for the
     * already-cached window `reconcileFromUid..sinceUid` so the caller can mirror read/star
     * changes and deletions made from other clients/webmail.
     *
     * [flagsToPush] holds local flag changes the server hasn't confirmed yet (keyed by uid);
     * they're written to the server here and reported back as the message's flags, so a
     * reconcile never reverts a change the user just made in this app.
     *
     * Body text is fetched per message; attachment bytes are NOT (see [downloadAttachment]).
     */
    fun syncFolder(
        store: IMAPStore,
        folderInfo: FolderInfo,
        sinceUid: Long?,
        reconcileFromUid: Long? = null,
        flagsToPush: Map<Long, RemoteFlags> = emptyMap(),
    ): FolderSnapshot {
        val folder = store.getFolder(folderInfo.remoteName) as IMAPFolder
        try {
            open(folder, writable = flagsToPush.isNotEmpty())
            val newMessages = fetchNewMessages(folder, sinceUid)
            val remoteFlags = if (sinceUid != null && reconcileFromUid != null) {
                readAndPushFlags(folder, reconcileFromUid, sinceUid, flagsToPush)
            } else {
                null
            }
            return FolderSnapshot(newMessages, remoteFlags)
        } catch (e: MessagingException) {
            throw MailError.FolderUnavailable(folderInfo.remoteName, e)
        } finally {
            if (folder.isOpen) runCatching { folder.close(false) }
        }
    }

    private fun fetchNewMessages(folder: IMAPFolder, sinceUid: Long?): List<ParsedMessage> {
        val candidates: Array<Message> = if (sinceUid != null) {
            // UIDNEXT is -1 when the server doesn't report it; only trust it to skip the
            // fetch when it's actually known.
            val uidNext = folder.uidNext
            if (uidNext != -1L && uidNext - 1 <= sinceUid) {
                emptyArray()
            } else {
                // UIDFolder.LASTUID (-1) tells the server "up through the highest UID
                // currently in the mailbox" without us needing to know it in advance.
                folder.getMessagesByUID(sinceUid + 1, UIDFolder.LASTUID)
            }
        } else {
            // First sync for this folder: cap to the most recent N to avoid pulling a
            // decade of mail on initial setup — the rest is reachable via fetchOlderMessages.
            val count = folder.messageCount
            val start = maxOf(1, count - INITIAL_SYNC_LIMIT + 1)
            if (count == 0) emptyArray() else folder.getMessages(start, count)
        }
        if (candidates.isEmpty()) return emptyList()

        folder.fetch(candidates, ENVELOPE_PROFILE)
        // "UID n:*" always matches the mailbox's last message, even when its UID is below n,
        // so an up-to-date folder would otherwise re-return its newest message every sync.
        val fresh = if (sinceUid != null) candidates.filter { folder.getUID(it) > sinceUid } else candidates.toList()
        return parseInOrder(folder, fresh)
    }

    private fun readAndPushFlags(
        folder: IMAPFolder,
        fromUid: Long,
        toUid: Long,
        flagsToPush: Map<Long, RemoteFlags>,
    ): Map<Long, RemoteFlags> {
        val known = folder.getMessagesByUID(fromUid, toUid)
        folder.fetch(known, FLAGS_PROFILE)
        val writable = folder.mode == Folder.READ_WRITE
        val result = HashMap<Long, RemoteFlags>(known.size)
        for (msg in known) {
            val uid = folder.getUID(msg)
            var flags = RemoteFlags(isRead = msg.isSet(Flags.Flag.SEEN), isStarred = msg.isSet(Flags.Flag.FLAGGED))
            val wanted = flagsToPush[uid]
            if (wanted != null && wanted != flags && writable) {
                if (wanted.isRead != flags.isRead) msg.setFlag(Flags.Flag.SEEN, wanted.isRead)
                if (wanted.isStarred != flags.isStarred) msg.setFlag(Flags.Flag.FLAGGED, wanted.isStarred)
                flags = wanted
            }
            result[uid] = flags
        }
        return result
    }

    /** Fetches up to [limit] messages older than [oldestKnownUid], for "load more" as the user
     * scrolls to the bottom of the Inbox list — the initial sync only pulls the newest
     * [INITIAL_SYNC_LIMIT] messages (see [syncFolder]), so this is how the rest of a large
     * mailbox becomes reachable without a slow/expensive full backfill on first setup.
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
                // The oldest cached message may itself be gone from the server by now; the
                // first surviving message at or after that UID marks the same position.
                val anchor = folder.getMessageByUID(oldestKnownUid)
                    ?: folder.getMessagesByUID(oldestKnownUid, UIDFolder.LASTUID)
                        .firstOrNull { folder.getUID(it) >= oldestKnownUid }
                anchor?.messageNumber ?: (folder.messageCount + 1)
            } else {
                folder.messageCount + 1
            }
            if (beforeSeqNum <= 1) return emptyList()
            val start = maxOf(1, beforeSeqNum - limit)
            val end = beforeSeqNum - 1
            val messages = folder.getMessages(start, end)
            if (messages.isEmpty()) return emptyList()

            folder.fetch(messages, ENVELOPE_PROFILE)
            // Newest-first: if the connection drops mid-page, what was parsed stays
            // contiguous with the already-cached messages instead of leaving a gap between
            // them that the "oldest known uid" cursor would then skip over for good.
            return parseInOrder(folder, messages.reversed())
        } catch (e: MessagingException) {
            throw MailError.FolderUnavailable(folderInfo.remoteName, e)
        } finally {
            if (folder.isOpen) runCatching { folder.close(false) }
        }
    }

    /** Parses [messages] in the given order, stopping at the first connection-level failure
     * so the result is always a contiguous prefix — callers advance a UID cursor past whatever
     * is returned, and a hole in the middle would never be fetched again. A message that's
     * merely malformed is skipped on its own. */
    private fun parseInOrder(folder: IMAPFolder, messages: List<Message>): List<ParsedMessage> {
        val parsed = ArrayList<ParsedMessage>(messages.size)
        for (msg in messages) {
            try {
                parsed += parseMessage(folder, msg as MimeMessage)
            } catch (e: FolderClosedException) {
                break
            } catch (e: StoreClosedException) {
                break
            } catch (e: Exception) {
                // Unparseable message — skipping it is the only option that doesn't block
                // every message after it.
            }
        }
        return parsed
    }

    private fun parseMessage(folder: IMAPFolder, msg: MimeMessage): ParsedMessage {
        val uid = folder.getUID(msg)
        val (rawPlain, rawHtml, attachments) = MimePartWalker.extractBody(msg)
        val html = rawHtml?.take(MAX_HTML_BODY_CHARS)
        // HTML-only mail (most newsletters/notifications) has no text part at all — derive
        // one so the list snippet, search, body-rules and reply quoting still have text.
        val plain = (rawPlain ?: html?.let(HtmlText::toPlainText))?.take(MAX_PLAIN_BODY_CHARS)
        val fromAddr = runCatching { msg.from?.firstOrNull() as? InternetAddress }.getOrNull()
        return ParsedMessage(
            uid = uid,
            // Read from the already-fetched ENVELOPE (IMAPMessage overrides getMessageID);
            // getHeader("Message-ID") would cost one extra round-trip per message.
            messageIdHeader = runCatching { msg.messageID }.getOrNull(),
            subject = runCatching { msg.subject }.getOrNull(),
            fromAddress = fromAddr?.address,
            fromName = fromAddr?.personal,
            toAddresses = addressesOf(msg, Message.RecipientType.TO),
            ccAddresses = addressesOf(msg, Message.RecipientType.CC),
            sentAt = (msg.sentDate ?: msg.receivedDate)?.time ?: System.currentTimeMillis(),
            isRead = msg.isSet(Flags.Flag.SEEN),
            isStarred = msg.isSet(Flags.Flag.FLAGGED),
            snippet = plain?.let(::snippetOf),
            bodyPlain = plain,
            bodyHtml = html,
            attachments = attachments,
        )
    }

    /** One-line list preview of a text body: whitespace runs (line breaks, indentation)
     * collapsed to single spaces. */
    fun snippetOf(plain: String): String =
        plain.take(SNIPPET_SCAN_CHARS).replace(WHITESPACE_RUN, " ").trim().take(SNIPPET_LENGTH)

    /** Text rendition of an HTML body, for callers outside this package (see [HtmlText]). */
    fun htmlToPlainText(html: String): String = HtmlText.toPlainText(html)

    private fun addressesOf(msg: MimeMessage, type: Message.RecipientType): List<String> =
        runCatching {
            msg.getRecipients(type)?.mapNotNull { (it as? InternetAddress)?.address } ?: emptyList()
        }.getOrDefault(emptyList())

    /**
     * Long-lived IMAP IDLE loop on [folderName] for providers that support it. Owns its whole
     * connection lifecycle: connects (with the IDLE-specific read timeout, see [MailSession]),
     * idles, and on any drop reconnects with capped exponential backoff so a flaky network
     * doesn't spin-loop and burn battery. Suspends until the calling coroutine is cancelled.
     *
     * [password] is re-read on every (re)connect so a password changed in Settings is picked
     * up without restarting the loop; returning null (credentials gone) ends it.
     *
     * [onChange] runs once after every (re)connect — to catch up on anything that arrived
     * while disconnected — and again each time the server pushes a mailbox change.
     *
     * `IMAPFolder.idle(true)` returns after ONE server notification; the no-arg `idle()`
     * keeps idling until some other thread issues a command, i.e. it never returns for new
     * mail on its own — which is why the previous implementation never triggered a sync.
     */
    suspend fun idleLoop(
        account: AccountEntity,
        password: () -> String?,
        folderName: String,
        onChange: suspend () -> Unit,
    ): Unit = coroutineScope {
        val currentStore = AtomicReference<IMAPStore?>()
        // idle() blocks in a socket read that coroutine cancellation can't interrupt; closing
        // the store from another thread is what makes it return. This child is cancelled
        // together with the loop below and does exactly that, off the main thread.
        val closer = launch(Dispatchers.IO) {
            try {
                awaitCancellation()
            } finally {
                currentStore.getAndSet(null)?.let { runCatching { it.close() } }
            }
        }
        withContext(Dispatchers.IO) {
            var backoffMs = INITIAL_BACKOFF_MS
            while (isActive) {
                val currentPassword = password() ?: break
                val connectedAt = SystemClock.elapsedRealtime()
                try {
                    val store = connect(account, currentPassword, forIdle = true)
                    currentStore.set(store)
                    val folder = store.getFolder(folderName) as IMAPFolder
                    folder.open(Folder.READ_ONLY)
                    var syncedCount = folder.messageCount
                    onChange()
                    while (isActive) {
                        // Mail that landed while onChange() was syncing: the server owes this
                        // connection an EXISTS for it as soon as IDLE starts, but not every
                        // server delivers that (observed: only connections already idling got
                        // notified). Asking for the count issues a NOOP when the connection
                        // has been quiet, which settles it either way.
                        val count = folder.messageCount
                        if (count != syncedCount) {
                            syncedCount = count
                            onChange()
                            continue
                        }
                        folder.idle(true)
                        syncedCount = folder.messageCount
                        if (isActive) onChange()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Dropped connection, read timeout, auth/server error — reconnect below.
                } finally {
                    currentStore.getAndSet(null)?.let { runCatching { it.close() } }
                }
                // A connection that stayed up for a while was healthy; only back off harder
                // when attempts keep failing quickly.
                val wasStable = SystemClock.elapsedRealtime() - connectedAt >= STABLE_CONNECTION_MS
                backoffMs = if (wasStable) INITIAL_BACKOFF_MS else (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                delay(backoffMs)
            }
        }
        closer.cancel()
    }

    /** Downloads one attachment's bytes into the app-private cache dir, under a directory
     * named by [cacheKey] (unique per message — IMAP UIDs alone repeat across folders and
     * accounts, and two messages' same-named attachments used to overwrite each other).
     * Returns the resulting [File]; the FileProvider authority for sharing it is already
     * declared in the manifest (`res/xml/file_paths.xml`, cache-path "attachments"). */
    fun downloadAttachment(
        store: IMAPStore,
        folderInfo: FolderInfo,
        messageUid: Long,
        attachment: ParsedAttachment,
        cacheKey: String,
    ): File {
        val folder = store.getFolder(folderInfo.remoteName) as IMAPFolder
        try {
            folder.open(Folder.READ_ONLY)
            val msg = folder.getMessageByUID(messageUid) as? MimeMessage
                ?: throw MailError.Unknown(IllegalStateException("Message uid=$messageUid not found"))
            val part = MimePartWalker.findAttachmentPart(msg, attachment.partIndex)
                ?: throw MailError.Unknown(IllegalStateException("Attachment part not found"))

            val dir = File(File(appContext.cacheDir, "attachments"), cacheKey).apply { mkdirs() }
            val outFile = File(dir, safeFileName(attachment.fileName))
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

    /** Attachment names come straight from the sender's MIME headers — strip anything that
     * could escape the cache directory or isn't a legal file name. */
    private fun safeFileName(raw: String): String =
        raw.replace(UNSAFE_FILE_NAME_CHARS, "_").trim('.', ' ').take(MAX_FILE_NAME_LENGTH).ifBlank { "attachment" }

    /** Flips the `\Seen` flag on the server for one message so other IMAP clients/webmail
     * agree with this app's local read/unread state. */
    fun setMessageSeen(store: IMAPStore, folderInfo: FolderInfo, uid: Long, seen: Boolean) =
        setFlag(store, folderInfo, uid, Flags.Flag.SEEN, seen)

    /** Flips the `\Flagged` flag on the server for one message — the IMAP-side counterpart
     * of the app's local star toggle. */
    fun setMessageFlagged(store: IMAPStore, folderInfo: FolderInfo, uid: Long, flagged: Boolean) =
        setFlag(store, folderInfo, uid, Flags.Flag.FLAGGED, flagged)

    /** Opens the folder [Folder.READ_WRITE] — flag mutation requires write access — and
     * closes without expunging (`close(false)`), since a flag change alone should never
     * trigger message removal. */
    private fun setFlag(store: IMAPStore, folderInfo: FolderInfo, uid: Long, flag: Flags.Flag, value: Boolean) {
        val folder = store.getFolder(folderInfo.remoteName) as IMAPFolder
        try {
            folder.open(Folder.READ_WRITE)
            val msg = folder.getMessageByUID(uid)
                ?: throw MailError.Unknown(IllegalStateException("Message uid=$uid not found"))
            msg.setFlag(flag, value)
        } catch (e: MessagingException) {
            throw MailError.FolderUnavailable(folderInfo.remoteName, e)
        } finally {
            if (folder.isOpen) runCatching { folder.close(false) }
        }
    }

    /** Marks a message `\Deleted` and expunges it so it is physically removed from the
     * server mailbox — the IMAP-side counterpart of the app's local "permanently delete
     * from Trash" action. */
    fun deleteMessagePermanently(store: IMAPStore, folderInfo: FolderInfo, uid: Long) =
        deleteMessagesPermanently(store, folderInfo, listOf(uid))

    /** Flags every message in [uids] `\Deleted` on a single opened folder, then expunges
     * once on close (`close(true)`), instead of one connect/open/close round-trip per
     * message. Used for bulk actions like "Empty trash" where all the messages live in the
     * same real IMAP folder. Missing UIDs (e.g. already gone server-side) are skipped rather
     * than aborting the whole batch. */
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

    private fun open(folder: IMAPFolder, writable: Boolean) {
        if (writable) {
            try {
                folder.open(Folder.READ_WRITE)
                return
            } catch (e: ReadOnlyFolderException) {
                // Shared/read-only mailbox — fall through; flag pushes are skipped for it.
            }
        }
        folder.open(Folder.READ_ONLY)
    }

    companion object {
        private const val SNIPPET_LENGTH = 160
        /** How much of the body is scanned to build the snippet — more than [SNIPPET_LENGTH]
         * since leading blank lines/indentation collapse away. */
        private const val SNIPPET_SCAN_CHARS = 600
        private const val INITIAL_SYNC_LIMIT = 25
        /** Page size for [fetchOlderMessages] — matches [INITIAL_SYNC_LIMIT] so scrolling to
         * the bottom of the Inbox always loads another same-sized batch of older mail. */
        const val OLDER_PAGE_SIZE = 25
        private const val INITIAL_BACKOFF_MS = 5_000L
        private const val MAX_BACKOFF_MS = 5 * 60_000L
        private const val STABLE_CONNECTION_MS = 60_000L

        /** Caps on stored body text. A Room row is read through a ~2 MB CursorWindow, so an
         * unbounded body (some marketing mail ships multi-MB HTML) makes the row unreadable. */
        private const val MAX_HTML_BODY_CHARS = 600_000
        private const val MAX_PLAIN_BODY_CHARS = 200_000

        private const val MAX_FILE_NAME_LENGTH = 120
        private val UNSAFE_FILE_NAME_CHARS = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
        private val WHITESPACE_RUN = Regex("\\s+")

        private val VIRTUAL_FOLDER_ATTRIBUTES = setOf("\\all", "\\flagged", "\\important", "\\noselect")
        private val SENT_NAME_HINTS = listOf("sent", "poslat", "gesendet", "enviad", "envoy")
        private val DRAFTS_NAME_HINTS = listOf("draft", "nedovr", "entw", "borrador", "brouillon")
        private val TRASH_NAME_HINTS = listOf("trash", "deleted", "otpad", "papierkorb", "papelera", "corbeille")
        private val JUNK_NAME_HINTS = listOf("junk", "spam", "bulk")

        /** Envelope + flags + uid + BODYSTRUCTURE in one round-trip for the whole batch —
         * without CONTENT_INFO every message would issue its own BODYSTRUCTURE fetch before
         * its body parts could be located. */
        private val ENVELOPE_PROFILE = FetchProfile().apply {
            add(FetchProfile.Item.ENVELOPE)
            add(FetchProfile.Item.FLAGS)
            add(FetchProfile.Item.CONTENT_INFO)
            add(UIDFolder.FetchProfileItem.UID)
        }

        private val FLAGS_PROFILE = FetchProfile().apply {
            add(FetchProfile.Item.FLAGS)
            add(UIDFolder.FetchProfileItem.UID)
        }
    }
}
