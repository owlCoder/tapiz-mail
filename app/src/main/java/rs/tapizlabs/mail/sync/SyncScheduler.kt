package rs.tapizlabs.mail.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import rs.tapizlabs.mail.data.local.entity.AccountEntity

/**
 * Thin [WorkManager] wrapper scheduling one [MailSyncWorker] [androidx.work.PeriodicWorkRequest]
 * per account. This is the fallback/baseline sync path — it runs regardless of whether
 * [IdleSyncService] is also active, so accounts without IDLE support (or once the foreground
 * service has stopped) still get bounded, Doze-aware periodic sync.
 *
 * Unique work name is keyed by account id so scheduling account B never clobbers account A's
 * periodic request, and re-scheduling the same account (e.g. after editing its sync interval)
 * safely replaces the old request via [ExistingPeriodicWorkPolicy.UPDATE].
 */
@Singleton
class SyncScheduler @Inject constructor(
    private val workManager: WorkManager,
    // Second, OEM-resilient trigger driven in lockstep with WorkManager so every
    // existing scheduleFor/cancelFor/rescheduleAll caller gets both automatically.
    private val alarmScheduler: MailAlarmScheduler,
) {

    /** Minimum interval WorkManager honors for periodic work; account intervals below this
     * are clamped rather than silently rejected at enqueue time. */
    private val minIntervalMinutes = 15L

    fun scheduleFor(account: AccountEntity) {
        val intervalMinutes = account.syncIntervalMinutes.toLong().coerceAtLeast(minIntervalMinutes)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<MailSyncWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(workDataOf(MailSyncWorker.KEY_ACCOUNT_ID to account.id))
            // Capped exponential backoff so a server that's briefly unreachable doesn't retry
            // aggressively and drain battery — WorkManager's floor is 10s, we widen the ceiling
            // implicitly via the periodic interval itself re-triggering.
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .addTag(TAG_PERIODIC_SYNC)
            .build()

        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName(account.id),
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        alarmScheduler.scheduleFor(account.id, account.syncIntervalMinutes)
    }

    fun cancelFor(accountId: String) {
        workManager.cancelUniqueWork(uniqueWorkName(accountId))
        alarmScheduler.cancelFor(accountId)
    }

    fun rescheduleAll(accounts: List<AccountEntity>) {
        accounts.forEach { scheduleFor(it) }
    }

    private fun uniqueWorkName(accountId: String) = "$UNIQUE_WORK_PREFIX$accountId"

    companion object {
        private const val UNIQUE_WORK_PREFIX = "mail_sync_"
        private const val TAG_PERIODIC_SYNC = "mail_periodic_sync"
    }
}
