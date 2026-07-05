package rs.tapizlabs.mail.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Tapiz brand interaction signature for primary/secondary buttons: flat at rest, and on
 * press reveals a signal-colored offset edge on the bottom+right only (Android has no
 * hover, so the press state stands in for it) — never an even all-around shadow, never a
 * translateY lift. Implemented as a signal-colored box drawn behind the button, offset
 * down+right, that fades in on press.
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
) {
    val colors = AppColors
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val edgeOffset by animateDpAsState(
        targetValue = if (isPressed && enabled && !loading) 4.dp else 0.dp,
        animationSpec = tween(120),
        label = "primary_press_edge",
    )

    Box(modifier = modifier.height(height)) {
        if (edgeOffset > 0.dp) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = edgeOffset, y = edgeOffset)
                    .clip(shape)
                    .background(colors.signal),
            )
        }

        Button(
            onClick = { if (!loading) onClick() },
            modifier = Modifier.matchParentSize(),
            interactionSource = interactionSource,
            shape = shape,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                disabledContainerColor = colors.primary.copy(alpha = 0.4f),
                disabledContentColor = colors.onPrimary.copy(alpha = 0.6f),
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
                    color = colors.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(text = text, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Ghost/secondary variant of [MailPrimaryButton] — same flat + signal-edge press signature,
 * outline at rest instead of filled. Used for Reply/Forward and other non-primary actions. */
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
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val edgeOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) 4.dp else 0.dp,
        animationSpec = tween(120),
        label = "ghost_press_edge",
    )

    Box(modifier = modifier.height(height)) {
        if (edgeOffset > 0.dp) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = edgeOffset, y = edgeOffset)
                    .clip(shape)
                    .background(colors.signal),
            )
        }

        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.matchParentSize(),
            shape = shape,
            enabled = enabled,
            interactionSource = interactionSource,
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
}
