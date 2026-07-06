package rs.tapizlabs.mail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rs.tapizlabs.mail.ui.i18n.LocalAppLanguage
import rs.tapizlabs.mail.ui.i18n.toLocale
import rs.tapizlabs.mail.ui.model.MessageListItemUi
import rs.tapizlabs.mail.ui.theme.AppColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * One inbox row: avatar-initial circle, sender, subject, snippet, relative time, unread
 * indicator, star toggle, attachment paperclip. Presentational only — actions bubble up via
 * callbacks; the caller (ViewModel/screen) owns what happens on tap/star/swipe.
 *
 * @param isDraft shows a small pencil badge next to the subject — set only when the Inbox's
 * Drafts pseudo-category ([rs.tapizlabs.mail.ui.inbox.PSEUDO_CATEGORY_DRAFTS]) is selected,
 * since that's the sole view that mixes in local-only drafts (the normal Inbox/Search views
 * filter them out — see their ViewModels).
 */
@Composable
fun MessageListItem(
    message: MessageListItemUi,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    modifier: Modifier = Modifier,
    isDraft: Boolean = false,
) {
    val colors = AppColors
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarInitial(
            name = message.fromName.ifBlank { message.fromAddress },
            colorIndex = message.categoryColorIndex ?: senderHashIndex(message.fromAddress),
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!message.isRead) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(colors.primary),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = message.fromName.ifBlank { message.fromAddress },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colors.textPrimary,
                        fontWeight = if (!message.isRead) FontWeight.Bold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                val appLanguage = LocalAppLanguage.current
                Text(
                    text = relativeTime(message.sentAt, appLanguage.toLocale()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.textMuted,
                        fontSize = 11.sp,
                    ),
                )
            }

            Spacer(Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDraft) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Draft",
                        tint = colors.textMuted,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = message.subject.ifBlank { "(no subject)" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colors.textPrimary,
                        fontWeight = if (!message.isRead) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (message.snippet.isNotBlank()) {
                Text(
                    text = message.snippet,
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        if (message.hasAttachments) {
            Icon(
                imageVector = Icons.Outlined.AttachFile,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
        }

        IconButton(onClick = onToggleStar, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (message.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (message.isStarred) "Unstar" else "Star",
                tint = if (message.isStarred) colors.amber else colors.textMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AvatarInitial(name: String, colorIndex: Int) {
    val colors = AppColors
    val tint = colors.categoryTints[abs(colorIndex) % colors.categoryTints.size]
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium.copy(
                color = tint,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

private fun senderHashIndex(fromAddress: String): Int = fromAddress.hashCode()

/** Built per-call against [locale] (not a fixed top-level formatter) so the month name
 * ("MMM") and any locale-specific separators follow the in-app language selection
 * ([rs.tapizlabs.mail.ui.i18n.LocalAppLanguage]) instead of the device's system locale —
 * without an explicit [Locale], [DateTimeFormatter.ofPattern] silently uses
 * `Locale.getDefault()`, which can disagree with what the user picked in this app. */
private fun relativeTime(epochMillis: Long, locale: Locale): String {
    val relativeTimeToday = DateTimeFormatter.ofPattern("HH:mm", locale)
    val relativeTimeOlder = DateTimeFormatter.ofPattern("MMM d", locale)
    val zone = ZoneId.systemDefault()
    val then = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val now = Instant.now().atZone(zone)
    return if (then.toLocalDate() == now.toLocalDate()) {
        then.format(relativeTimeToday)
    } else {
        then.format(relativeTimeOlder)
    }
}
