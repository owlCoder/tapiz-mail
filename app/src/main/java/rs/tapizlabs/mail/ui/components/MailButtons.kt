package rs.tapizlabs.mail.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * "Signal" direction button — flat, no press-edge/shadow gimmicks (that was the old
 * Tapiz brand's signature, not part of this design). Matches
 * design_handoff_tapiz_mail_android/design-reference.html's plain filled buttons.
 */
@Composable
fun MailPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = 44.dp,
    containerColorOverride: Color? = null,
    contentColorOverride: Color? = null,
) {
    val colors = AppColors
    val containerColor = containerColorOverride ?: colors.primary
    val contentColor = contentColorOverride ?: colors.onPrimary

    Button(
        onClick = { if (!loading) onClick() },
        modifier = modifier.height(height),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.6f),
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = contentColor,
                strokeWidth = 2.dp,
            )
        } else {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text = text, fontWeight = FontWeight.Bold)
        }
    }
}

/** Ghost/secondary variant — outline at rest, same flat no-shadow treatment. */
@Composable
fun MailGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 44.dp,
) {
    val colors = AppColors

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colors.primary,
            disabledContentColor = colors.primary.copy(alpha = 0.4f),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) colors.primary else colors.primary.copy(alpha = 0.4f),
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.Medium)
    }
}
