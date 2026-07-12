package rs.tapizlabs.mail.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A second, OEM-resilient sync trigger that runs *alongside* [SyncScheduler]'s
 * WorkManager path (belt and suspenders). Aggressive skins (MIUI/HyperOS, etc.)
 * throttle WorkManager so periodic sync only fires when the app is opened; an
 * exact `AllowWhileIdle` alarm punches through Doze and those restrictions.
 *
 * One self-rescheduling alarm per account, fired via [MailSyncAlarmReceiver],
 * which syncs the account and re-arms the next interval. WorkManager stays in
 * place, so if exact alarms are ever unavailable nothing regresses.
 */
@Singleton
class MailAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Floor kept in sync with [SyncScheduler]; alarms can go tighter than
     * WorkManager's 15-min minimum, but we keep the same clamp for battery. */
    private val minIntervalMinutes = 15L

    fun scheduleFor(accountId: String, intervalMinutes: Int) {
        val am = alarmManager ?: return
        val interval = intervalMinutes.toLong().coerceAtLeast(minIntervalMinutes)
        val triggerAt = System.currentTimeMillis() + interval * 60_000L
        val pi = pendingIntent(accountId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancelFor(accountId: String) {
        alarmManager?.cancel(pendingIntent(accountId))
    }

    fun rescheduleAll(accounts: List<Pair<String, Int>>) {
        accounts.forEach { (id, interval) -> scheduleFor(id, interval) }
    }

    private fun pendingIntent(accountId: String): PendingIntent {
        val intent = Intent(context, MailSyncAlarmReceiver::class.java).apply {
            action = MailSyncAlarmReceiver.ACTION_SYNC
            putExtra(MailSyncAlarmReceiver.EXTRA_ACCOUNT_ID, accountId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        // Per-account request code so alarms don't collide.
        return PendingIntent.getBroadcast(context, accountId.hashCode(), intent, flags)
    }
}
