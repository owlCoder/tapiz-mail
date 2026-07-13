package rs.tapizlabs.mail.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Flat app canvas (Soft / iOS look), ported 1:1 from `tapiz-lms/apps/android`'s
 * `GradientBackground`: a calm neutral base with a very subtle vertical gradient, plus two
 * barely-there primary radial glows in opposite corners — reads as a faint brand tint around
 * the edges without becoming a "mesh blob" backdrop. Used on Onboarding/Settings so Tapiz Mail
 * shares the same ecosystem canvas language as the other Ink & Ember apps.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val c = AppColors
    val baseTopGlow = if (c.isDark) 0.09f else 0.14f
    val baseBottomGlow = if (c.isDark) 0.05f else 0.10f
    val topGlow = animateFloatAsState(
        targetValue = baseTopGlow,
        animationSpec = tween(380),
        label = "backgroundGlowTop",
    ).value
    val bottomGlow = animateFloatAsState(
        targetValue = baseBottomGlow,
        animationSpec = tween(380),
        label = "backgroundGlowBottom",
    ).value
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.canvasTop, c.canvasBottom)))
            .background(
                Brush.radialGradient(
                    colors = listOf(c.primary.copy(alpha = topGlow), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = 700f,
                ),
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(c.primary.copy(alpha = bottomGlow), Color.Transparent),
                    center = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    radius = 600f,
                ),
            ),
    ) {
        content()
    }
}
