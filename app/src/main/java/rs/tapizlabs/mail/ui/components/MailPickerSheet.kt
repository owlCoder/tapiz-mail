package rs.tapizlabs.mail.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.theme.AppColors

data class PickerSheetOption<T>(val value: T, val label: String)

/**
 * Bottom-sheet option list with a checkmark on the selected row — the generic "pick one of N"
 * pattern for option sets too large for a [SegmentedPickerCard] row (e.g. 5 languages), used
 * instead of a Material `DropdownMenu` popup so it matches the rest of the app's flat/card
 * "Signal" surfaces rather than the default dark, square-cornered dropdown look.
 */
@Composable
fun <T> MailPickerSheet(
    visible: Boolean,
    title: String,
    options: List<PickerSheetOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppColors

    MailSheet(visible = visible, onDismiss = onDismiss) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        options.forEach { option ->
            val isSelected = option.value == selected
            val interactionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            onSelect(option.value)
                            onDismiss()
                        },
                    )
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
                if (isSelected) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = colors.primary)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
