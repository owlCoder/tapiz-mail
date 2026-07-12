package rs.tapizlabs.mail.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import rs.tapizlabs.mail.data.repository.AccountRepository
import rs.tapizlabs.mail.data.repository.SyncRepository
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

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                        // Re-arm every account's alarm after reboot/update.
                        val accounts = accountRepository.observeAccounts().first()
                        alarmScheduler.rescheduleAll(accounts.map { it.id to it.syncIntervalMinutes })
                    }
                    ACTION_SYNC -> {
                        val accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID)
                        if (accountId != null) {
                            // Re-arm first so a sync failure can't break the chain.
                            val account = accountRepository.getAccountOnce(accountId)
                            if (account != null) {
                                alarmScheduler.scheduleFor(accountId, account.syncIntervalMinutes)
                                runCatching { syncRepository.syncAccount(accountId) }
                            }
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SYNC = "rs.tapizlabs.mail.SYNC_ALARM"
        const val EXTRA_ACCOUNT_ID = "account_id"
    }
}
