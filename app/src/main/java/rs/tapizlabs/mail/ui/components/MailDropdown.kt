package rs.tapizlabs.mail.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.theme.AppColors

/** Generic labeled dropdown used for security/interval/swipe-action pickers across
 * Add-Account and Settings. Shares [MailTextField]'s exact `OutlinedTextField` recipe
 * (height, border, label/leading-icon colors) instead of a hand-rolled Row, so a
 * dropdown sitting next to a text field in the same row lines up pixel-for-pixel. */
@Composable
fun <T> MailDropdownField(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AppColors
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(160),
        label = "dropdown_arrow_rotation",
    )

    OutlinedTextField(
        value = optionLabel(selected),
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        label = { Text(text = label, color = colors.textMuted) },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { expanded = true },
            ),
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.rotate(arrowRotation),
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.stroke,
            unfocusedBorderColor = colors.stroke,
            disabledBorderColor = colors.stroke.copy(alpha = 0.5f),
            focusedLabelColor = colors.textMuted,
            unfocusedLabelColor = colors.textMuted,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedContainerColor = colors.cardSubtle,
            unfocusedContainerColor = colors.cardSubtle,
            disabledContainerColor = colors.cardSubtle,
            disabledTextColor = colors.textPrimary.copy(alpha = 0.6f),
        ),
    )

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(optionLabel(option), style = MaterialTheme.typography.bodyMedium) },
                onClick = {
                    onSelect(option)
                    expanded = false
                },
            )
        }
    }
}
