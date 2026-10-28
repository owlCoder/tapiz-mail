package rs.tapizlabs.mail.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.components.MailGhostButton
import rs.tapizlabs.mail.ui.components.MailPrimaryButton
import rs.tapizlabs.mail.ui.components.MailSheet
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.theme.AppColors

/** Non-destructive confirmation; dismiss/back/scrim never opens the link. */
@Composable
internal fun OpenEmailLinkSheet(target: String?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val colors = AppColors
    MailSheet(visible = target != null, onDismiss = onDismiss) {
        Text(strings.detailOpenLinkTitle, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(strings.detailOpenLinkMessage, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
        Spacer(Modifier.height(12.dp))
        SelectionContainer {
            Text(target.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MailGhostButton(strings.settingsCancel, onDismiss, Modifier.weight(1f), icon = Icons.Outlined.Close)
            MailPrimaryButton(strings.detailOpenLink, onConfirm, Modifier.weight(1f), icon = Icons.AutoMirrored.Outlined.OpenInNew)
        }
    }
}
