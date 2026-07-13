package rs.tapizlabs.mail.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.components.MailGhostButton
import rs.tapizlabs.mail.ui.components.MailPrimaryButton
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Dedicated onboarding step asking for the POST_NOTIFICATIONS permission (Android 13+),
 * shown once right after the first account is saved — this is the point notifications
 * actually become meaningful (there's mail to be notified about), rather than firing the
 * system permission dialog unconditionally from `MainActivity.onCreate` where it could
 * interrupt any screen (e.g. the language picker) on every fresh install.
 *
 * The user's choice here is entirely theirs — [onSkip] and declining the system dialog
 * are both valid outcomes, this screen never re-prompts or blocks proceeding to the inbox.
 */
@Composable
fun NotificationPermissionScreen(
    onAllow: () -> Unit,
    onSkip: () -> Unit,
) {
    val colors = AppColors
    val strings = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 72.dp, bottom = 32.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(30.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = strings.notifPermTitle,
            style = MaterialTheme.typography.headlineSmall.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
            ),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = strings.notifPermSubtext,
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textSecondary),
        )

        Spacer(Modifier.weight(1f))

        MailPrimaryButton(
            text = strings.notifPermAllow,
            onClick = onAllow,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))

        MailGhostButton(
            text = strings.notifPermSkip,
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        )
    }
}
