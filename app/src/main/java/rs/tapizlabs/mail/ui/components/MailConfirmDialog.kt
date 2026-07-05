package rs.tapizlabs.mail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Destructive-confirmation sheet built on the shared [MailSheet] primitive — used for
 * "remove account", "delete category" etc. Distinct `onConfirm`/`onDismiss` callbacks
 * so scrim-tap/back (dismiss) never gets treated as confirm.
 */
@Composable
fun MailConfirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppColors

    MailSheet(visible = visible, onDismiss = onDismiss, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.coral.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = colors.coral,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(color = colors.textPrimary, fontWeight = FontWeight.Bold),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textMuted),
        )

        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            MailGhostButton(
                text = cancelLabel,
                icon = Icons.Outlined.Close,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            MailPrimaryButton(
                text = confirmLabel,
                icon = Icons.Filled.CheckCircle,
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
