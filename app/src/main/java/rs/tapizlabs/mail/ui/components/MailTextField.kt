package rs.tapizlabs.mail.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.theme.AppColors

@Composable
fun MailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    val colors = AppColors

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, color = colors.textMuted) },
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        singleLine = singleLine,
        enabled = enabled,
        isError = isError,
        leadingIcon = leadingIcon?.let {
            { Icon(imageVector = it, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.dp)) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
        supportingText = supportingText?.let {
            { Text(text = it, color = if (isError) colors.coral else colors.textMuted, style = MaterialTheme.typography.bodySmall) }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.stroke,
            focusedLabelColor = colors.primary,
            unfocusedLabelColor = colors.textMuted,
            cursorColor = colors.primary,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedContainerColor = colors.cardSubtle,
            unfocusedContainerColor = colors.cardSubtle,
            focusedLeadingIconColor = colors.primary,
            unfocusedLeadingIconColor = colors.textMuted,
            errorBorderColor = colors.coral,
            errorLabelColor = colors.coral,
        ),
    )
}

/** Password field with a masked/show-hide toggle — used by [MailTextField] callers that need it
 * rather than baking the toggle into every password field manually. */
@Composable
fun MailPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = Icons.Outlined.Lock,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    val colors = AppColors
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, color = colors.textMuted) },
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        singleLine = true,
        isError = isError,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
        leadingIcon = leadingIcon?.let {
            { Icon(imageVector = it, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.dp)) }
        },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = colors.textMuted,
                )
            }
        },
        supportingText = supportingText?.let {
            { Text(text = it, color = if (isError) colors.coral else colors.textMuted, style = MaterialTheme.typography.bodySmall) }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.stroke,
            focusedLabelColor = colors.primary,
            unfocusedLabelColor = colors.textMuted,
            cursorColor = colors.primary,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedContainerColor = colors.cardSubtle,
            unfocusedContainerColor = colors.cardSubtle,
            focusedLeadingIconColor = colors.primary,
            unfocusedLeadingIconColor = colors.textMuted,
            errorBorderColor = colors.coral,
            errorLabelColor = colors.coral,
        ),
    )
}
