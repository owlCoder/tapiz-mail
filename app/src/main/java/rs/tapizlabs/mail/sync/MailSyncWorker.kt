package rs.tapizlabs.mail.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import rs.tapizlabs.mail.data.repository.SyncRepository

/**
 * Fallback periodic sync path — runs on WorkManager's schedule (see [SyncScheduler]) for
 * accounts without IMAP IDLE support, and for every account once [IdleSyncService] isn't
 * running (app backgrounded past its timeout). Connects, fetches new mail since the last
 * known UID per folder, upserts into Room, and finishes — no long-lived connection is held
 * here, so this alone cannot drain battery the way an always-on socket would.
 */
@HiltWorker
class MailSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val accountId = inputData.getString(KEY_ACCOUNT_ID) ?: return Result.failure()
        return try {
            syncRepository.syncAccount(accountId)
            Result.success()
        } catch (e: Exception) {
            // Retry rather than fail outright: transient network/server issues (exactly the
            // kind a flaky UNS server produces) should back off and try again on WorkManager's
            // own schedule rather than requiring a manual refresh.
            Result.retry()
        }
    }

    companion object {
        const val KEY_ACCOUNT_ID = "account_id"
    }
}
