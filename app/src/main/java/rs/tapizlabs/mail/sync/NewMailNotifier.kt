package rs.tapizlabs.mail.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.AudioAttributes
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
 *
 * When more than one message arrives in a single sync pass, each message gets its own child
 * notification (unique id derived from [MessageEntity.id], same [GROUP_KEY]) in addition to a
 * `setGroupSummary` notification — standard Android notification grouping. This is what lets
 * the user expand the "N new messages" stack and tap an individual line to open that exact
 * message, rather than every line opening whichever message happened to be first.
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
        val soundEnabled = prefsStore.notificationSoundEnabledPref.first()
        val channelId = ensureChannel(soundEnabled)
        val manager = context.getSystemService(NotificationManager::class.java)

        if (messages.size == 1) {
            val message = messages.first()
            manager.notify(
                notificationIdFor(message.id),
                buildSingleMessageNotification(channelId, soundEnabled, accountDisplayName, message, isGrouped = false),
            )
            return
        }

        // Grouped notifications: one real notification per message (so each is individually
        // tappable/dismissable and opens its own message) plus a group-summary notification —
        // the summary is what collapses/expands the stack ("N new messages"), same as Gmail's
        // Android app. All share [GROUP_KEY] so the system associates them.
        messages.forEach { message ->
            manager.notify(
                notificationIdFor(message.id),
                buildSingleMessageNotification(channelId, soundEnabled, accountDisplayName, message, isGrouped = true),
            )
        }
        manager.notify(SUMMARY_NOTIFICATION_ID, buildSummaryNotification(channelId, soundEnabled, accountDisplayName, messages))
    }

    private fun buildSingleMessageNotification(
        channelId: String,
        soundEnabled: Boolean,
        accountDisplayName: String,
        message: MessageEntity,
        isGrouped: Boolean,
    ): Notification {
        val senderLabel = message.fromName.ifBlank { message.fromAddress }
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle(senderLabel)
            .setContentText(message.subject.ifBlank { "(no subject)" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.snippet.ifBlank { message.subject }))
            .setSubText(accountDisplayName)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(message.id))
            .apply { if (isGrouped) setGroup(GROUP_KEY) }
            .applyPreOSound(soundEnabled)
            .build()
    }

    private fun buildSummaryNotification(
        channelId: String,
        soundEnabled: Boolean,
        accountDisplayName: String,
        messages: List<MessageEntity>,
    ): Notification {
        val inboxStyle = NotificationCompat.InboxStyle()
            .setSummaryText(accountDisplayName)
        messages.take(MAX_INBOX_STYLE_LINES).forEach { message ->
            val senderLabel = message.fromName.ifBlank { message.fromAddress }
            inboxStyle.addLine("$senderLabel: ${message.subject.ifBlank { "(no subject)" }}")
        }

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle("${messages.size} new messages")
            .setContentText(accountDisplayName)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(messages.first().id))
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .applyPreOSound(soundEnabled)
            .build()
    }

    /** Stable per-message notification id (so re-notifying the same message, e.g. a second
     * sync pass that somehow re-delivers it, updates rather than duplicates it) while staying
     * distinct from [SUMMARY_NOTIFICATION_ID]. */
    private fun notificationIdFor(messageId: String): Int {
        val hash = messageId.hashCode() and 0x7FFFFFFF
        return if (hash == SUMMARY_NOTIFICATION_ID) hash + 1 else hash
    }

    /** Below API 26 there is no notification channel to carry the sound, so it must be set
     * directly on the notification — a no-op on API 26+, where [ensureChannel] already picked
     * the channel with (or without) sound baked in. */
    private fun NotificationCompat.Builder.applyPreOSound(soundEnabled: Boolean): NotificationCompat.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return this
        return if (soundEnabled) {
            setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
        } else {
            setSound(null)
        }
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

    /** Returns the channel id to post on for the current sound preference. Two separate
     * channels (rather than one channel with a mutable sound) because
     * [NotificationChannel.setSound] only has effect at creation time on API 26+ — once a
     * channel exists, the user's own system Settings entry for it wins forever, so flipping
     * the in-app sound toggle on an already-created channel would silently do nothing. */
    private fun ensureChannel(soundEnabled: Boolean): String {
        val channelId = if (soundEnabled) CHANNEL_ID_SOUND else CHANNEL_ID_SILENT
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelId
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.new_mail_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        if (soundEnabled) {
            channel.setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        } else {
            channel.setSound(null, null)
        }
        manager.createNotificationChannel(channel)
        return channelId
    }

    companion object {
        const val EXTRA_MESSAGE_ID = "extra_message_id"
        private const val CHANNEL_ID_SILENT = "new_mail"
        private const val CHANNEL_ID_SOUND = "new_mail_sound"
        private const val GROUP_KEY = "rs.tapizlabs.mail.NEW_MAIL_GROUP"
        private const val SUMMARY_NOTIFICATION_ID = 2001
        private const val MAX_INBOX_STYLE_LINES = 5
        private const val LARGE_ICON_SIZE_PX = 128
    }
}
