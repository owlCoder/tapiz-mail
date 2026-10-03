package rs.tapizlabs.mail.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import rs.tapizlabs.mail.MainActivity
import rs.tapizlabs.mail.R
import rs.tapizlabs.mail.data.local.dao.AccountDao
import rs.tapizlabs.mail.data.local.dao.FolderDao
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.data.local.entity.FolderType
import rs.tapizlabs.mail.data.repository.SyncRepository
import rs.tapizlabs.mail.mail.ImapClient
import rs.tapizlabs.mail.security.CredentialStore

/**
 * Foreground service holding IMAP IDLE connections open for every account with
 * `supportsIdle = true` — set at Add-Account time from an actual IDLE capability probe
 * (see `ImapClient.testConnectionWithIdleProbe`), not assumed from the provider, so any
 * server that advertises IDLE (including some university/custom IMAP setups) gets
 * near-instant new-mail notifications without polling. This is the ONLY place
 * in the app that keeps a long-lived socket open; everything else (see [MailSyncWorker]/
 * [SyncScheduler]) is short, connect-fetch-disconnect.
 *
 * Battery bound: the service only runs while the app is foregrounded or briefly
 * backgrounded — `MainActivity` (re)starts it whenever the app is in the foreground and at
 * least one account supports IDLE, and [backgroundLifecycleObserver] starts a
 * [BACKGROUND_STOP_DELAY_MS] timer the moment the app leaves the foreground, stopping the
 * service (and its IDLE sockets) if the app hasn't come back by the time it fires.
 * [SyncScheduler]'s periodic WorkManager job keeps covering the account after that, so mail
 * still arrives, just not instantly.
 */
@AndroidEntryPoint
class IdleSyncService : Service() {

    @Inject lateinit var imapClient: ImapClient
    @Inject lateinit var credentialStore: CredentialStore
    @Inject lateinit var accountDao: AccountDao
    @Inject lateinit var folderDao: FolderDao
    @Inject lateinit var syncRepository: SyncRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var watchJob: Job? = null
    private var backgroundStopJob: Job? = null

    private val backgroundLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            // App went to background: give it a grace window (e.g. quick app-switch back)
            // before tearing down the IDLE connections — avoids reconnect churn on every
            // brief backgrounding while still bounding worst-case background socket time.
            backgroundStopJob?.cancel()
            backgroundStopJob = serviceScope.launch {
                delay(BACKGROUND_STOP_DELAY_MS)
                stopSelf()
            }
        }

        override fun onStart(owner: LifecycleOwner) {
            backgroundStopJob?.cancel()
            backgroundStopJob = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ProcessLifecycleOwner.get().lifecycle.addObserver(backgroundLifecycleObserver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Started by MainActivity each time the app comes to the foreground (see its
        // STARTED-scoped collector), so this can run many times against one service instance.
        if (!enterForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (watchJob == null) {
            watchJob = serviceScope.launch { watchIdleAccounts() }
        }
        // NOT sticky: a sticky restart happens while the app is in the background, where
        // Android 12+ refuses startForeground() (ForegroundServiceStartNotAllowedException)
        // — a guaranteed crash for a service whose whole point is to exist only around
        // foreground use. The periodic WorkManager/alarm sync is the durability net.
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Android 15+ caps `dataSync` foreground services (6h per 24h) and requires the service
     * to stop itself when the system calls this, or the app is crashed for it. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(backgroundLifecycleObserver)
        // Cancels every idle loop; each loop closes its own IMAP store off the main thread
        // (see ImapClient.idleLoop) — closing sockets here would be network I/O on main.
        serviceScope.cancel()
        super.onDestroy()
    }

    /** API 29+ requires the foreground service type at startForeground() call time too, not
     * just declared in the manifest. Returns false if the system refuses (service somehow
     * started while the app isn't in the foreground) so the caller can bail out cleanly. */
    private fun enterForeground(): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        true
    } catch (e: Exception) {
        false
    }

    /** Keeps one IDLE loop per IDLE-capable active account, following the account list
     * live: adding/editing/removing an account restarts the loops with the new set instead
     * of requiring an app restart, and the service stops itself once none are left. */
    private suspend fun watchIdleAccounts() {
        accountDao.getActiveAccounts()
            .map { accounts -> accounts.filter { it.supportsIdle } }
            .distinctUntilChanged()
            .collectLatest { accounts ->
                if (accounts.isEmpty()) {
                    stopSelf()
                    return@collectLatest
                }
                coroutineScope {
                    accounts.forEach { account -> launch { idleOnInbox(account) } }
                }
            }
    }

    /** IDLE is only meaningful on the inbox for now — categorized/other folders still get
     * picked up by the periodic WorkManager pass; watching every folder per account would
     * multiply open sockets for little practical benefit here. */
    private suspend fun idleOnInbox(account: AccountEntity) {
        val inboxName = folderDao.getFolderOnceByType(account.id, FolderType.INBOX)?.remoteName ?: "INBOX"
        imapClient.idleLoop(
            account = account,
            password = { credentialStore.getImapPassword(account.id) },
            folderName = inboxName,
            // On its own short-lived connection (normal timeouts), not the IDLE one: a
            // stalled fetch must fail fast rather than hang on the long IDLE read timeout
            // while holding the account's sync lock.
            onChange = { runCatching { syncRepository.syncInbox(account.id) } },
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.idle_sync_channel_name),
            NotificationManager.IMPORTANCE_MIN,
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.idle_sync_notification_title))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "idle_sync"
        private const val NOTIFICATION_ID = 1001

        /** Grace window after the app backgrounds before IDLE connections are torn down —
         * long enough to survive a quick app-switch-away-and-back, short enough that a
         * genuinely backgrounded app isn't holding sockets open for the whole Doze cycle. */
        private const val BACKGROUND_STOP_DELAY_MS = 3 * 60_000L
    }
}
