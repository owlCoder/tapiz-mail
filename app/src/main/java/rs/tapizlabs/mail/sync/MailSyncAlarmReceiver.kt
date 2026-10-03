package rs.tapizlabs.mail.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import rs.tapizlabs.mail.data.repository.AccountRepository
import rs.tapizlabs.mail.data.repository.SyncRepository
import rs.tapizlabs.mail.di.ApplicationScope
import javax.inject.Inject

/**
 * Fires the exact per-account sync alarm (see [MailAlarmScheduler]) and, on
 * device boot / app update, re-arms alarms for all accounts. Each fire syncs
 * the account and re-arms the next interval so the chain survives OEM killers.
 */
@AndroidEntryPoint
class MailSyncAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var syncRepository: SyncRepository
    @Inject lateinit var accountRepository: AccountRepository
    @Inject lateinit var alarmScheduler: MailAlarmScheduler
    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        appScope.launch {
            try {
                when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                        // Re-arm every account's alarm after reboot/update.
                        val accounts = accountRepository.observeAccounts().first()
                        alarmScheduler.rescheduleAll(accounts.map { it.id to it.syncIntervalMinutes })
                    }
                    ACTION_SYNC -> {
                        val accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)
                        val account = accountId?.let { accountRepository.getAccountOnce(it) }
                        if (account != null) {
                            // Re-arm first so a sync failure can't break the chain.
                            alarmScheduler.scheduleFor(account.id, account.syncIntervalMinutes)
                            syncWithinBroadcastBudget(account.id)
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    /** Alarm broadcasts are delivered as foreground broadcasts, so this receiver has ~10s
     * before the system reports an ANR and kills the process — while a full account sync on a
     * slow server easily takes longer. The sync itself runs on the application scope (it
     * keeps going past this receiver), and if it hasn't finished within the budget the work
     * is also handed to WorkManager, which can hold the process alive properly. */
    private suspend fun syncWithinBroadcastBudget(accountId: String) {
        val sync = appScope.launch { runCatching { syncRepository.syncAccount(accountId) } }
        val finished = withTimeoutOrNull(BROADCAST_BUDGET_MS) { sync.join() } != null
        if (!finished) syncScheduler.syncNow(accountId)
    }

    companion object {
        const val ACTION_SYNC = "rs.tapizlabs.mail.SYNC_ALARM"
        const val EXTRA_ACCOUNT_ID = "account_id"
        private const val BROADCAST_BUDGET_MS = 8_000L
    }
}
