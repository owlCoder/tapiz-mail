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
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import rs.tapizlabs.mail.MainActivity
import rs.tapizlabs.mail.R
import rs.tapizlabs.mail.data.local.dao.AccountDao
import rs.tapizlabs.mail.data.local.dao.FolderDao
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
 * backgrounded — [backgroundLifecycleObserver] starts a [BACKGROUND_STOP_DELAY_MS] timer the
 * moment the app leaves the foreground and stops the service (and its IDLE sockets) if the
 * app hasn't come back by the time it fires. [SyncScheduler]'s periodic WorkManager job keeps
 * covering the account after that, so mail still arrives, just not instantly.
 */
@AndroidEntryPoint
class IdleSyncService : Service() {

    @Inject lateinit var imapClient: ImapClient
    @Inject lateinit var credentialStore: CredentialStore
    @Inject lateinit var accountDao: AccountDao
    @Inject lateinit var folderDao: FolderDao
    @Inject lateinit var syncRepository: SyncRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val idleJobs = mutableListOf<Job>()
    private val openStores = mutableListOf<IMAPStore>()
    private var backgroundStopJob: Job? = null

    private val backgroundLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            // App went to background: give it a grace window (e.g. quick app-switch back)
            // before tearing down the IDLE connections — avoids reconnect churn on every
            // brief backgrounding while still bounding worst-case background socket time.
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
        // API 29+ requires the foreground service type at startForeground() call time too,
        // not just declared in the manifest, or the system throws MissingForegroundServiceTypeException.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(backgroundLifecycleObserver)
        serviceScope.launch { startIdleForEligibleAccounts() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY: if the system kills this process under memory pressure, it's fine
        // (and expected) for it to restart later rather than guarantee redelivery — the
        // periodic WorkManager sync is the durability net, this service is a best-effort
        // latency improvement only.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(backgroundLifecycleObserver)
        idleJobs.forEach { it.cancel() }
        openStores.forEach { runCatching { it.close() } }
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun startIdleForEligibleAccounts() {
        val accounts = accountDao.getActiveAccounts().first().filter { it.supportsIdle }
        for (account in accounts) {
            val password = credentialStore.getImapPassword(account.id) ?: continue
            val store = runCatching { imapClient.connect(account, password) }.getOrNull() ?: continue
            openStores.add(store)

            val folders = folderDao.getFoldersForAccount(account.id).first()
            val inboxFolder = folders.firstOrNull { it.type == FolderType.INBOX } ?: continue
            val imapFolder = runCatching { store.getFolder(inboxFolder.remoteName) as IMAPFolder }
                .getOrNull() ?: continue

            // IDLE is only meaningful on the inbox for now — categorized/other folders still
            // get picked up by the periodic WorkManager pass; watching every folder per
            // account would multiply open sockets for little practical benefit here.
            val job = imapClient.idle(serviceScope, store, imapFolder) {
                serviceScope.launch { syncRepository.syncFolder(account.id, store, imapFolder) }
            }
            idleJobs.add(job)
        }
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
