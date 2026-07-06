package rs.tapizlabs.mail.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import rs.tapizlabs.mail.MainActivity
import rs.tapizlabs.mail.R
import rs.tapizlabs.mail.core.local.PrefsStore
import rs.tapizlabs.mail.data.local.entity.MessageEntity

/**
 * Posts a "new mail" notification for freshly-synced inbox messages. Called from
 * [SyncRepository] after a sync pass upserts new messages into an INBOX-type folder —
 * Sent/Drafts/Trash syncs never reach here, only genuinely new incoming mail does.
 *
 * Separate notification channel from [IdleSyncService]'s silent "sync active" channel:
 * that one is `IMPORTANCE_MIN`/ongoing (a status indicator, not an alert), this one is
 * `IMPORTANCE_DEFAULT` so it actually notifies the user the way a mail app should.
 */
@Singleton
class NewMailNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefsStore: PrefsStore,
) {

    /** Rendered once and reused for every notification — the brand envelope glyph shown as the
     * large icon on the right side of the notification, alongside the monochrome small icon in
     * the status bar (the two occupy different slots and both are expected on modern Android). */
    private val largeIcon: Bitmap? by lazy {
        ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)?.toBitmap(
            width = LARGE_ICON_SIZE_PX,
            height = LARGE_ICON_SIZE_PX,
            config = Bitmap.Config.ARGB_8888,
        )
    }

    suspend fun notifyNewMessages(accountDisplayName: String, messages: List<MessageEntity>) {
        if (messages.isEmpty()) return
        if (!prefsStore.notificationsEnabledPref.first()) return
        ensureChannel()
        val manager = context.getSystemService(NotificationManager::class.java)

        if (messages.size == 1) {
            manager.notify(NOTIFICATION_ID, buildSingleMessageNotification(accountDisplayName, messages.first()))
        } else {
            manager.notify(NOTIFICATION_ID, buildSummaryNotification(accountDisplayName, messages))
        }
    }

    private fun buildSingleMessageNotification(accountDisplayName: String, message: MessageEntity): Notification {
        val senderLabel = message.fromName.ifBlank { message.fromAddress }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle(senderLabel)
            .setContentText(message.subject.ifBlank { "(no subject)" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.snippet.ifBlank { message.subject }))
            .setSubText(accountDisplayName)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(message.id))
            .build()
    }

    private fun buildSummaryNotification(accountDisplayName: String, messages: List<MessageEntity>): Notification {
        val inboxStyle = NotificationCompat.InboxStyle()
            .setSummaryText(accountDisplayName)
        messages.take(MAX_INBOX_STYLE_LINES).forEach { message ->
            val senderLabel = message.fromName.ifBlank { message.fromAddress }
            inboxStyle.addLine("$senderLabel: ${message.subject.ifBlank { "(no subject)" }}")
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle("${messages.size} new messages")
            .setContentText(accountDisplayName)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(messages.first().id))
            .build()
    }

    private fun contentIntent(messageId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_MESSAGE_ID, messageId)
        }
        return PendingIntent.getActivity(
            context,
            messageId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.new_mail_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_MESSAGE_ID = "extra_message_id"
        private const val CHANNEL_ID = "new_mail"
        private const val NOTIFICATION_ID = 2001
        private const val MAX_INBOX_STYLE_LINES = 5
        private const val LARGE_ICON_SIZE_PX = 128
    }
}
