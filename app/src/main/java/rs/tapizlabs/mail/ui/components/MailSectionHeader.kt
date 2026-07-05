package rs.tapizlabs.mail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.theme.AppColors

/** Icon-chip + title section header — one consistent pattern across Settings sections
 * (per design guideline: don't mix icon-chip headers with plain uppercase labels).
 * [subtitle], when provided, renders as a small muted line under the title — e.g. a one-line
 * explanation of what the group configures. */
@Composable
fun MailSectionHeader(title: String, icon: ImageVector, modifier: Modifier = Modifier, subtitle: String? = null) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        MailIconChip(icon = icon, size = 30.dp, iconScale = 0.6f)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.textPrimary,
                fontWeight = FontWeight.Bold,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textMuted,
                )
            }
        }
    }
}

/** Small tinted square icon container reused for list rows / section headers
 * (36-40dp row-level, 28-30dp compact per the sizing spec). */
@Composable
fun MailIconChip(icon: ImageVector, size: Dp = 40.dp, iconScale: Float = 0.5f) {
    val colors = AppColors
    Box(
        modifier = Modifier
            .size(size)
            .background(color = colors.accentSoft, shape = RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(size * iconScale),
        )
    }
}
