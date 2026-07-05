package rs.tapizlabs.mail.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class TapizColors(
    val isDark: Boolean,
    val canvasTop: Color,
    val canvasBottom: Color,
    val card: Color,
    val surfaceSolid: Color,
    val cardSubtle: Color,
    val stroke: Color,
    val shadow: Color,
    val primary: Color,
    val primaryBright: Color,
    val onPrimary: Color,
    val accentSoft: Color,
    val signal: Color,
    val coral: Color,
    val mint: Color,
    val amber: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    // Category chip tint pool — cycled per auto-assigned/manual category, not
    // semantic like the priority colors in Boards.
    val categoryTints: List<Color>,
)

val DarkColors = TapizColors(
    isDark = true,
    canvasTop = Color(0xFF0A0E14),
    canvasBottom = Color(0xFF0D1117),
    card = Color(0xFF161B24),
    surfaceSolid = Color(0xFF1C2333),
    cardSubtle = Color(0xFF131820),
    stroke = Color(0xFF2A3344),
    shadow = Color(0x661A1F2B),
    primary = Color(0xFF22D3EE),
    primaryBright = Color(0xFF67E8F9),
    onPrimary = Color(0xFF003344),
    accentSoft = Color(0x1A22D3EE),
    signal = Color(0xFFA3E635),
    coral = Color(0xFFE0655A),
    mint = Color(0xFF34D399),
    amber = Color(0xFFFCD34D),
    textPrimary = Color(0xFFF0F6FC),
    textSecondary = Color(0xFFB0BEC5),
    textMuted = Color(0xFF6B7A8D),
    categoryTints = listOf(
        Color(0xFF22D3EE), Color(0xFFA3E635), Color(0xFFFCD34D),
        Color(0xFFE0655A), Color(0xFF34D399), Color(0xFFC084FC),
    ),
)

val LightColors = TapizColors(
    isDark = false,
    canvasTop = Color(0xFFE8F4F8),
    canvasBottom = Color(0xFFEDF2F7),
    card = Color(0xFFFAFDFF),
    surfaceSolid = Color(0xFFF0F6FA),
    cardSubtle = Color(0xFFF4F8FC),
    stroke = Color(0xFFBFCFDE),
    shadow = Color(0x22638FAD),
    primary = Color(0xFF0891B2),
    primaryBright = Color(0xFF0E7490),
    onPrimary = Color(0xFFFFFFFF),
    accentSoft = Color(0x200891B2),
    signal = Color(0xFF4D7C0F),
    coral = Color(0xFFDC2626),
    mint = Color(0xFF059669),
    amber = Color(0xFFB45309),
    textPrimary = Color(0xFF0D1117),
    textSecondary = Color(0xFF374151),
    textMuted = Color(0xFF6B7A8D),
    categoryTints = listOf(
        Color(0xFF0891B2), Color(0xFF4D7C0F), Color(0xFFB45309),
        Color(0xFFDC2626), Color(0xFF059669), Color(0xFF9333EA),
    ),
)

val LocalTapizColors = compositionLocalOf<TapizColors> { DarkColors }

val AppColors: TapizColors
    @androidx.compose.runtime.Composable
    get() = LocalTapizColors.current
