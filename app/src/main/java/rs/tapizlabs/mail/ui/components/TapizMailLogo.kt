package rs.tapizlabs.mail.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Tapiz Mail "Niti" mark: a horizontal ink bar crossed by an "M" accent
 * thread (peak-valley-peak monogram), consistent with the abstract
 * cross-stroke glyphs used across the ecosystem (LMS "T", Admin "A", Boards
 * columns). Ported from the launcher icon foreground
 * (`res/drawable/ic_launcher_foreground.xml`), on a 48-unit design grid: bar
 * "M6 24 H42", accent "M10,34 L10,14 L24,28 L38,14 L38,34".
 *
 * With [tile]=true (launcher/hero context): ink bar + halo (tile-colour) +
 * sand/terracotta accent thread — the full duotone mark on a background
 * tile. With [tile]=false (About/bare-glyph context): a single monochrome
 * stroke in [glyphColor] for both bar and accent when [mono] is true,
 * matching the ecosystem's `tone="mono"` convention; otherwise a duotone
 * bare glyph (ink bar + [accentColor] thread).
 *
 * @param glyphColor stroke color used for both strokes when `!tile && mono`.
 * @param accentColor the "M" thread's own color when duotone — Mail's
 *   sand/terracotta brand accent (`#C9A227`), not the ecosystem purple.
 */
@Composable
fun TapizMailLogo(
    size: Dp,
    modifier: Modifier = Modifier,
    tile: Boolean = true,
    mono: Boolean = false,
    glyphColor: Color = AppColors.primary,
    accentColor: Color = Color(0xFFC9A227),
) {
    val tileColor = AppColors.cardSubtle

    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val u = s / 48f // scale: design is on a 48-unit grid
        val strokeW = 9f * u

        if (tile) {
            drawRoundRect(
                color = tileColor,
                size = Size(s, s),
                cornerRadius = CornerRadius(11.5f * u, 11.5f * u),
            )
        }

        val bar = Path().apply {
            moveTo(6f * u, 24f * u)
            lineTo(42f * u, 24f * u)
        }
        val accent = Path().apply {
            moveTo(10f * u, 34f * u)
            lineTo(10f * u, 14f * u)
            lineTo(24f * u, 28f * u)
            lineTo(38f * u, 14f * u)
            lineTo(38f * u, 34f * u)
        }

        if (tile) {
            drawPath(bar, color = Color(0xFF221C30), style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(accent, color = tileColor, style = Stroke(width = 10.5f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(accent, color = accentColor, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else if (mono) {
            drawPath(bar, color = glyphColor, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(accent, color = glyphColor, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            drawPath(bar, color = Color(0xFF221C30), style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(accent, color = accentColor, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
