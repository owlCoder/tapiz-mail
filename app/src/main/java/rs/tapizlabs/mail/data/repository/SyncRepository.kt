package rs.tapizlabs.mail.data.repository

import com.sun.mail.imap.IMAPStore
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import rs.tapizlabs.mail.data.local.dao.AccountDao
import rs.tapizlabs.mail.data.local.dao.AttachmentDao
import rs.tapizlabs.mail.data.local.dao.CategoryRuleDao
import rs.tapizlabs.mail.data.local.dao.FolderDao
import rs.tapizlabs.mail.data.local.dao.MessageDao
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.local.entity.AttachmentEntity
import rs.tapizlabs.mail.data.local.entity.CategoryMatcher
import rs.tapizlabs.mail.data.local.entity.CategoryRuleEntity
import rs.tapizlabs.mail.data.local.entity.FolderEntity
import rs.tapizlabs.mail.data.local.entity.FolderType
import rs.tapizlabs.mail.data.local.entity.MessageEntity
import rs.tapizlabs.mail.mail.FolderInfo
import rs.tapizlabs.mail.mail.ImapClient
import rs.tapizlabs.mail.mail.ParsedAttachment
import rs.tapizlabs.mail.mail.ParsedMessage
import rs.tapizlabs.mail.mail.RemoteFlags
import rs.tapizlabs.mail.security.CredentialStore
import rs.tapizlabs.mail.sync.NewMailNotifier

/** Shared prefix for [MailRepository]'s synthetic local-only Drafts/Trash [FolderEntity] ids
 * (`"local-drafts-$accountId"`/`"local-trash-$accountId"`) — these folders have no IMAP
 * counterpart and must be excluded from any code that iterates folders to sync against the
 * server. */
internal const val LOCAL_FOLDER_ID_PREFIX = "local-"

/** Mailbox roles that are actually synced. Server-side Drafts/Trash/Junk are not: the app
 * keeps its own local Drafts and Trash, and nothing ever displays those mailboxes — fetching
 * them only cost time/battery and (before the Inbox query was tightened) leaked spam and
 * already-deleted mail into the Inbox. */
private val SYNCED_FOLDER_TYPES = setOf(FolderType.INBOX, FolderType.SENT, FolderType.CUSTOM)

/** How many of a folder's newest cached messages get their flags/existence reconciled against
 * the server per sync — one small FLAGS fetch, bounded so a mailbox with thousands of cached
 * messages doesn't pay for all of them every 15 minutes. */
private const val RECONCILE_WINDOW = 500

/** Stays well under SQLite's bind-variable limit for `IN (...)` lists. */
private const val SQL_CHUNK_SIZE = 500

private const val BACKFILL_HTML_CHARS = 20_000
private const val BACKFILL_BATCH_SIZE = 50

/**
 * Single fetch-and-upsert path for one account's folders, shared by
 * [rs.tapizlabs.mail.sync.MailSyncWorker] (periodic fallback) and
 * [rs.tapizlabs.mail.sync.IdleSyncService] (IDLE push callback) so there is exactly one place
 * that talks to both [ImapClient] and Room — avoids two divergent copies of fetch/parse/upsert
 * logic that could drift (e.g. one path forgetting to run [CategoryMatcher]).
 */
@Singleton
class SyncRepository @Inject constructor(
    private val imapClient: ImapClient,
    private val credentialStore: CredentialStore,
    private val accountDao: AccountDao,
    private val folderDao: FolderDao,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val newMailNotifier: NewMailNotifier,
) {

    /** Serializes sync passes per account (keyed by accountId) — [rs.tapizlabs.mail.sync.MailSyncWorker]'s
     * periodic pass and [rs.tapizlabs.mail.sync.IdleSyncService]'s IDLE push callback can otherwise fire
     * for the same account at nearly the same time, both read the same stale highest-known
     * uid, both fetch the same "new" messages from IMAP, and both notify — the exact cause of
     * the duplicate-notification-on-resync symptom. Per-account (not global) so one slow
     * server never blocks another account's sync. */
    private val accountSyncLocks = ConcurrentHashMap<String, Mutex>()

    /** Messages whose read/star flags were changed locally and not yet confirmed on the
     * server, mapped to the number of pushes still outstanding (in flight, or failed because
     * the device was offline). Sync's flag reconcile skips overwriting these with the server's
     * stale state and pushes the local state instead. A count rather than a set, so that with
     * two quick toggles the first push completing doesn't lift the protection while the second
     * is still queued. In-memory only: after a process death an unconfirmed change falls back
     * to whatever the server says, which is the pre-existing behavior. */
    private val locallyChangedFlags = ConcurrentHashMap<String, Int>()

    private val derivedTextBackfillPending = AtomicBoolean(true)

    private fun lockFor(accountId: String): Mutex = accountSyncLocks.computeIfAbsent(accountId) { Mutex() }

    /** Runs [block] under [accountId]'s sync lock — for remote mutations (flag changes,
     * deletes) that must not interleave with a sync pass reading the same mailbox. */
    suspend fun <T> withAccountLock(accountId: String, block: suspend () -> T): T =
        lockFor(accountId).withLock { block() }

    /** Call before writing a flag change to Room; pair with [markFlagPushSucceeded] once
     * the server has it. */
    fun markFlagsChangedLocally(messageId: String) {
        locallyChangedFlags.merge(messageId, 1, Int::plus)
    }

    /** One direct push of [messageId]'s flags reached the server. */
    fun markFlagPushSucceeded(messageId: String) {
        locallyChangedFlags.computeIfPresent(messageId) { _, pending -> if (pending <= 1) null else pending - 1 }
    }

    /** Syncs every folder of one account: connect once, fetch-new-since-last-uid per
     * folder, upsert into Room, disconnect. Returns the number of new messages stored.
     * Throws if the account can't be reached at all; a single folder's failure is contained. */
    suspend fun syncAccount(accountId: String): Int = syncFolders(accountId, typeFilter = null)

    /** Syncs only the account's Inbox — the IDLE push path, where the Inbox is the one
     * mailbox being watched and re-checking every other folder per push would be wasted work. */
    suspend fun syncInbox(accountId: String): Int = syncFolders(accountId, typeFilter = FolderType.INBOX)

    private suspend fun syncFolders(accountId: String, typeFilter: FolderType?): Int = withContext(Dispatchers.IO) {
        lockFor(accountId).withLock {
            val account = accountDao.getAccountOnce(accountId) ?: return@withLock 0
            val password = credentialStore.getImapPassword(accountId) ?: return@withLock 0
            val rules = categoryRuleDao.getRulesForAccountOnce(accountId)
            // Purely local and usually a no-op query — done before connecting so it happens
            // even when the server turns out to be unreachable.
            backfillDerivedText()

            val store = imapClient.connect(account, password)
            try {
                // Nothing else ever provisions a real account's remote folders into Room —
                // without this, the folder list below is permanently empty and no account
                // ever syncs. Runs on every full sync (one cheap IMAP LIST) so it also picks
                // up folders added/removed/re-typed on the server later; the Inbox-only push
                // path skips it once the Inbox is known.
                if (typeFilter == null || folderDao.getFolderOnceByType(accountId, typeFilter) == null) {
                    ensureFoldersProvisioned(accountId, store)
                }

                val folders = folderDao.getFoldersForAccountOnce(accountId).filter { folder ->
                    !folder.id.startsWith(LOCAL_FOLDER_ID_PREFIX) &&
                        folder.type in SYNCED_FOLDER_TYPES &&
                        (typeFilter == null || folder.type == typeFilter)
                }
                var newCount = 0
                for (folder in folders) {
                    // One folder's failure (e.g. a stale/renamed remote mailbox) must not abort
                    // sync for every other folder on the account, INBOX included.
                    newCount += runCatching { syncOneFolder(store, account, folder, rules) }.getOrDefault(0)
                }
                newCount
            } finally {
                runCatching { store.close() }
            }
        }
    }

    /** Repairs text derived by older versions: gives HTML-only messages the derived text body
     * new syncs store up front (see [rs.tapizlabs.mail.data.local.dao.MessageDao.getHtmlOnlyWithoutText]),
     * and re-collapses snippets that still carry raw line-break whitespace. Both loops are
     * self-terminating: every processed row stops matching its query (a row whose HTML
     * yields no text at all gets a single space so it isn't picked up again). */
    private suspend fun backfillDerivedText() {
        // Once per process: the query scans the whole table, which isn't worth repeating
        // on every sync once there's nothing left for it to find.
        if (!derivedTextBackfillPending.compareAndSet(true, false)) return
        while (true) {
            val rows = messageDao.getHtmlOnlyWithoutText(BACKFILL_HTML_CHARS, BACKFILL_BATCH_SIZE)
            if (rows.isEmpty()) break
            for (row in rows) {
                val plain = runCatching { imapClient.htmlToPlainText(row.text) }.getOrDefault("")
                messageDao.setDerivedText(row.id, plain.ifEmpty { " " }, imapClient.snippetOf(plain))
            }
        }
        while (true) {
            val rows = messageDao.getUncollapsedSnippets(BACKFILL_BATCH_SIZE)
            if (rows.isEmpty()) break
            rows.forEach { messageDao.setSnippet(it.id, imapClient.snippetOf(it.text)) }
        }
    }

    private suspend fun ensureFoldersProvisioned(accountId: String, store: IMAPStore) {
        val remoteFolders = imapClient.listFolders(store)
        val existing = folderDao.getFoldersForAccountOnce(accountId)
        val existingByRemoteName = existing
            .filterNot { it.id.startsWith(LOCAL_FOLDER_ID_PREFIX) }
            .associateBy { it.remoteName }
        val toUpsert = remoteFolders.map { info ->
            val known = existingByRemoteName[info.remoteName]
            FolderEntity(
                id = known?.id ?: "$accountId:${info.remoteName}",
                accountId = accountId,
                remoteName = info.remoteName,
                displayName = info.displayName,
                type = info.type,
                unreadCount = known?.unreadCount ?: 0,
            )
        }
        folderDao.upsertAll(toUpsert)

        // Drop mailboxes that no longer exist (deleted/renamed on the server, or virtual
        // ones — Gmail's All Mail etc. — that older versions used to sync); their cached
        // messages go with them via the foreign key. Only when the listing is clearly sane,
        // so a truncated/empty LIST response can never wipe the local cache.
        if (remoteFolders.any { it.type == FolderType.INBOX }) {
            val remoteNames = remoteFolders.mapTo(HashSet()) { it.remoteName }
            val stale = existingByRemoteName.values.filter { it.remoteName !in remoteNames }.map { it.id }
            stale.chunked(SQL_CHUNK_SIZE).forEach { folderDao.deleteByIds(it) }
        }
    }

    /** New mail + flag/deletion reconcile for one folder on an already-connected [store],
     * in a single mailbox open. Returns the number of genuinely new messages stored. */
    private suspend fun syncOneFolder(
        store: IMAPStore,
        account: AccountEntity,
        folder: FolderEntity,
        rules: List<CategoryRuleEntity>,
    ): Int {
        val lastUid = messageDao.getHighestKnownUid(folder.id)
        val localRows = if (lastUid != null) messageDao.getFlagRows(folder.id, RECONCILE_WINDOW) else emptyList()
        val flagsToPush = localRows
            .filter { locallyChangedFlags.containsKey(it.id) }
            .associate { it.uid to RemoteFlags(it.isRead, it.isStarred) }

        val snapshot = imapClient.syncFolder(
            store = store,
            folderInfo = folder.toInfo(),
            sinceUid = lastUid,
            reconcileFromUid = localRows.minOfOrNull { it.uid },
            flagsToPush = flagsToPush,
        )

        val inserted = insertNewMessages(folder, snapshot.newMessages, rules)
        // No notifications for the very first sync of a folder (lastUid == null): that's the
        // initial backfill of mail the user already knows about, not "new mail" — it used to
        // fire up to 25 notifications the moment an account was added. Mail already read on
        // another client isn't worth a notification either.
        if (folder.type == FolderType.INBOX && lastUid != null) {
            newMailNotifier.notifyNewMessages(account.displayName, inserted.filterNot { it.isRead })
        }

        snapshot.remoteFlags?.let { remote ->
            for (row in localRows) {
                val remoteFlags = remote[row.uid]
                when {
                    // Gone from the server (deleted/moved from another client). A copy the
                    // user moved to local Trash stays there — it's theirs to purge.
                    remoteFlags == null -> if (row.folderId == folder.id) messageDao.deleteById(row.id)
                    // This pass just wrote the row's local flags to the server — whatever
                    // pushes were pending for it are now moot.
                    row.uid in flagsToPush -> locallyChangedFlags.remove(row.id)
                    locallyChangedFlags.containsKey(row.id) -> Unit
                    remoteFlags.isRead != row.isRead || remoteFlags.isStarred != row.isStarred ->
                        messageDao.setFlags(row.id, remoteFlags.isRead, remoteFlags.isStarred)
                }
            }
        }
        return inserted.size
    }

    /** "Load more" for one account/folder as the user scrolls to the bottom of the Inbox
     * list — fetches the next [ImapClient.OLDER_PAGE_SIZE] messages older than whatever is
     * already cached (see [ImapClient.fetchOlderMessages]) and stores them the same way a
     * normal sync does. Returns the number of older messages the server returned (0 means the
     * folder's oldest cached message is already sequence number 1 — nothing left to page in —
     * so the caller should stop requesting more for this folder). Throws on connection/auth
     * failure so the caller can tell "nothing older" apart from "couldn't reach the server". */
    suspend fun loadOlderMessages(accountId: String, folderId: String): Int = withContext(Dispatchers.IO) {
        lockFor(accountId).withLock {
            val account = accountDao.getAccountOnce(accountId) ?: return@withLock 0
            val password = credentialStore.getImapPassword(accountId) ?: return@withLock 0
            val folder = folderDao.getFolderOnce(folderId) ?: return@withLock 0
            val rules = categoryRuleDao.getRulesForAccountOnce(accountId)

            val store = imapClient.connect(account, password)
            try {
                val oldestUid = messageDao.getLowestKnownUid(folderId)
                val parsed = imapClient.fetchOlderMessages(store, folder.toInfo(), oldestUid, ImapClient.OLDER_PAGE_SIZE)
                insertNewMessages(folder, parsed, rules)
                parsed.size
            } finally {
                runCatching { store.close() }
            }
        }
    }

    /** Stores only the messages Room doesn't have yet (by uid, counting ones sitting in local
     * Trash as known) and returns exactly those — existing rows are never overwritten, so a
     * re-delivered uid can't undo a local read/star/category change or pull a message back
     * out of Trash. */
    private suspend fun insertNewMessages(
        folder: FolderEntity,
        parsed: List<ParsedMessage>,
        rules: List<CategoryRuleEntity>,
    ): List<MessageEntity> {
        if (parsed.isEmpty()) return emptyList()
        val knownUids = parsed.map { it.uid }.chunked(SQL_CHUNK_SIZE)
            .flatMapTo(HashSet()) { messageDao.getKnownUids(folder.id, it) }
        val fresh = parsed.filterNot { it.uid in knownUids }
        if (fresh.isEmpty()) return emptyList()

        val messageEntities = fresh.map { it.toMessageEntity(folder.accountId, folder.id, rules) }
        messageDao.insertAllIgnoring(messageEntities)

        val attachmentEntities = fresh.zip(messageEntities).flatMap { (msg, entity) ->
            msg.attachments.map { it.toAttachmentEntity(entity.id) }
        }
        if (attachmentEntities.isNotEmpty()) {
            attachmentDao.upsertAll(attachmentEntities)
        }
        return messageEntities
    }

    private fun FolderEntity.toInfo() = FolderInfo(remoteName, displayName, type)

    private fun ParsedMessage.toMessageEntity(
        accountId: String,
        folderId: String,
        rules: List<CategoryRuleEntity>,
    ): MessageEntity {
        val entity = MessageEntity(
            id = "$accountId:$folderId:$uid",
            accountId = accountId,
            folderId = folderId,
            uid = uid,
            messageIdHeader = messageIdHeader.orEmpty(),
            subject = subject.orEmpty(),
            fromAddress = fromAddress.orEmpty(),
            fromName = fromName.orEmpty(),
            toAddresses = toAddresses.joinToString(","),
            sentAt = sentAt,
            snippet = snippet.orEmpty(),
            bodyPlain = bodyPlain.orEmpty(),
            bodyHtml = bodyHtml.orEmpty(),
            isRead = isRead,
            isStarred = isStarred,
            hasAttachments = attachments.isNotEmpty(),
            categoryId = null,
            isSynced = true,
        )
        return entity.copy(categoryId = CategoryMatcher.categorize(entity, rules))
    }

    private fun ParsedAttachment.toAttachmentEntity(messageId: String): AttachmentEntity =
        AttachmentEntity(
            id = "$messageId:$partIndex",
            messageId = messageId,
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            localUri = null,
            contentId = contentId,
            partIndex = partIndex,
        )
}
