package rs.tapizlabs.mail.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class TapizColors(
    val isDark: Boolean,
    val canvasTop: Color,
    val canvasBottom: Color,
    val card: Color,
    val heroPanel: Color,
    val surfaceSolid: Color,
    val cardSubtle: Color,
    /** Input-field / dropdown fill. A faint cool-tinted surface that blends into the app's
     * blue-ish canvas + card palette (NOT a flat neutral gray, which read as a hard cut-out
     * against the surrounding surfaces). Shared by `MailTextField` and `MailDropdownField`. */
    val inputBackground: Color,
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

// "Signal" direction. Every value below is computed directly from the oklch()
// calls in design_handoff_tapiz_mail_android/design-reference.html — that HTML
// file is the single source of truth for this rebrand. Do NOT re-derive colors
// from the README (its hex approximations are wrong, e.g. it claims primary ≈
// #3D2FA8 but oklch(45% 0.18 265) actually renders #2249B7 — a blue-indigo,
// not violet).
val DarkColors = TapizColors(
    isDark = true,
    canvasTop = Color(0xFF13161D), // oklch(20% 0.015 265)
    canvasBottom = Color(0xFF13161D),
    card = Color(0xFF21242B), // neutral generic card/sheet surface — rgba(255,255,255,.06) over canvasTop
    heroPanel = Color(0xFF1D2842), // Onboarding hero panel only — oklch(28% 0.05 265)
    surfaceSolid = Color(0xFF1D2842),
    cardSubtle = Color(0x11FFFFFF), // rgba(255,255,255,.06-.07)
    inputBackground = Color(0xFF191D26), // faint cool tint above canvasTop, blends w/ card
    stroke = Color(0x14FFFFFF), // rgba(255,255,255,.08)
    shadow = Color(0x66000000),
    primary = Color(0xFF5888FC), // oklch(65% 0.18 265)
    primaryBright = Color(0xFF5888FC),
    onPrimary = Color(0xFFFFFFFF), // white — dark-mode `primary` (#5888FC) is a mid-light blue,
    // so near-black text/icons on it (the old oklch(15% 0.01 265)) read as low-contrast/muddy;
    // white gives a cleaner, higher-contrast pairing consistent with the rest of dark-mode text.
    accentSoft = Color(0x1E5888FC),
    signal = Color(0xFFA3E635),
    coral = Color(0xFFE0655A),
    mint = Color(0xFF34D399),
    amber = Color(0xFFD0A92D), // star accent oklch(75% 0.14 90)
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xBFFFFFFF), // rgba(255,255,255,.75)
    textMuted = Color(0x80FFFFFF), // rgba(255,255,255,.5)
    categoryTints = listOf(
        Color(0xFF5888FC), Color(0xFFA3E635), Color(0xFFD0A92D),
        Color(0xFFE0655A), Color(0xFF34D399), Color(0xFFC084FC),
    ),
)

val LightColors = TapizColors(
    isDark = false,
    canvasTop = Color(0xFFF5F9FE), // oklch(98% 0.008 260)
    canvasBottom = Color(0xFFF5F9FE),
    card = Color(0xFFFFFFFF),
    heroPanel = Color(0xFF2249B7), // unused directly — light hero uses `primary`; kept for parity
    surfaceSolid = Color(0xFFFFFFFF),
    cardSubtle = Color(0xFFE5EBF9), // oklch(94% 0.02 265)
    inputBackground = Color(0xFFEDF1FA), // faint cool tint, blends w/ canvasTop + white cards
    stroke = Color(0xFFE8EBF2), // oklch(94% 0.01 265)
    shadow = Color(0x1E1E145A),
    primary = Color(0xFF2249B7), // oklch(45% 0.18 265)
    primaryBright = Color(0xFF5E8AF0), // oklch(65% 0.16 265)
    onPrimary = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFE5EBF9),
    signal = Color(0xFF4D7C0F),
    coral = Color(0xFFDC2626),
    mint = Color(0xFF059669),
    amber = Color(0xFFC87B00), // star accent oklch(65% 0.15 70)
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF666666),
    textMuted = Color(0xFF999999),
    categoryTints = listOf(
        Color(0xFF2249B7), Color(0xFF4D7C0F), Color(0xFFC87B00),
        Color(0xFFDC2626), Color(0xFF059669), Color(0xFF9333EA),
    ),
)

val LocalTapizColors = compositionLocalOf<TapizColors> { DarkColors }

val AppColors: TapizColors
    @androidx.compose.runtime.Composable
    get() = LocalTapizColors.current
