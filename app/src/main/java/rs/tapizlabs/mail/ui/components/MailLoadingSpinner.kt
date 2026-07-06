package rs.tapizlabs.mail.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Tapiz brand loading spinner: a pulsing ring around the envelope glyph, instead of a
 * bare Material [androidx.compose.material3.CircularProgressIndicator]. Same pulse
 * recipe as tapiz-lms's `SessionLoadingScreen` (scale 1↔1.15, alpha 1↔0.35, 1800ms
 * reverse loop) so the brand's loading feel is consistent across apps.
 */
@Composable
fun MailPulseSpinner(modifier: Modifier = Modifier, size: Dp = 40.dp, showIcon: Boolean = true) {
    val colors = AppColors
    val transition = rememberInfiniteTransition(label = "mailPulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseScale",
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(size)
                .scale(pulseScale),
        ) {
            drawCircle(color = colors.primary.copy(alpha = pulseAlpha), style = Stroke(width = 2.5.dp.toPx()))
        }
        // Plain pulse ring, no envelope glyph — used for inline/small loading states (test-
        // connection, "load more older mail") where the brand envelope icon reads as an
        // unrelated empty circle at that size rather than a mail-themed spinner; the full
        // icon variant stays the default for larger, standalone loading moments.
        if (showIcon) {
            Box(
                modifier = Modifier
                    .size(size * 0.56f)
                    .background(colors.accentSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.MarkEmailRead,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(size * 0.32f),
                )
            }
        }
    }
}
