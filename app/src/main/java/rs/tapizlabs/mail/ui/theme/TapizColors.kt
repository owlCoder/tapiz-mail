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

/** The 8 ecosystem skins (mirrors `TAPIZ_SKIN_IDS` in tapiz-design-system/src/skins.ts and
 * `AdminSkin` in tapiz-lms/apps/android-admin). Default for Tapiz Mail is [Ocean]. */
enum class MailSkin { Default, Ocean, Forest, Rose, Graphite, Sand, Crimson, Aurora }

private fun hex(v: Long) = Color(v or 0xFF000000L)

/** Raw primitive ramp for a skin+mode, ported from theme.css (same source as AdminColors). */
private data class SkinRamp(
    val ink100: Color,
    val ink200: Color,
    val ink300: Color,
    val border: Color,
    val txt1: Color,
    val txt2: Color,
    val txt3: Color,
    val primary300: Color,
    val primary500: Color,
    val signal400: Color,
)

private fun skinRamp(skin: MailSkin, dark: Boolean): SkinRamp = when (skin) {
    // default — Ink & Ember: near-black purple ink, purple accent, ember signal.
    MailSkin.Default -> if (dark) SkinRamp(
        ink100 = hex(0x110e1a), ink200 = hex(0x171321), ink300 = hex(0x1f1c2e), border = hex(0x2a2638),
        txt1 = hex(0xffffff), txt2 = hex(0xd1d0d3), txt3 = hex(0x8a8791),
        primary300 = hex(0xa989f5), primary500 = hex(0x7759c2), signal400 = hex(0xfc6d26),
    ) else SkinRamp(
        ink100 = hex(0xf7f6f9), ink200 = hex(0xffffff), ink300 = hex(0xf2f1f5), border = hex(0xe8e7eb),
        txt1 = hex(0x171321), txt2 = hex(0x45424d), txt3 = hex(0x827e8a),
        primary300 = hex(0x7759c2), primary500 = hex(0x533c8c), signal400 = hex(0xe24329),
    )
    // ocean — deep blue ink, sky-blue accent, teal signal.
    MailSkin.Ocean -> if (dark) SkinRamp(
        ink100 = hex(0x0d111c), ink200 = hex(0x121724), ink300 = hex(0x1a2030), border = hex(0x242b3c),
        txt1 = hex(0xffffff), txt2 = hex(0xd0d3da), txt3 = hex(0x878e9c),
        primary300 = hex(0x7fb5f2), primary500 = hex(0x3b7fd4), signal400 = hex(0x17b3a3),
    ) else SkinRamp(
        ink100 = hex(0xf5f7fa), ink200 = hex(0xffffff), ink300 = hex(0xeef2f7), border = hex(0xe2e8f0),
        txt1 = hex(0x101725), txt2 = hex(0x414d61), txt3 = hex(0x7c8598),
        primary300 = hex(0x2f6ab6), primary500 = hex(0x1e4271), signal400 = hex(0x0f9184),
    )
    // forest — green-tinted ink, emerald accent, amber signal.
    MailSkin.Forest -> if (dark) SkinRamp(
        ink100 = hex(0x0d130e), ink200 = hex(0x121a14), ink300 = hex(0x19231b), border = hex(0x223026),
        txt1 = hex(0xffffff), txt2 = hex(0xd0d6d1), txt3 = hex(0x869089),
        primary300 = hex(0x7ed6a3), primary500 = hex(0x35a468), signal400 = hex(0xf59e0b),
    ) else SkinRamp(
        ink100 = hex(0xf5f8f5), ink200 = hex(0xffffff), ink300 = hex(0xeef3ee), border = hex(0xe1e9e2),
        txt1 = hex(0x121a14), txt2 = hex(0x414d43), txt3 = hex(0x7b8780),
        primary300 = hex(0x2b8a57), primary500 = hex(0x1b5637), signal400 = hex(0xb45309),
    )
    // rose — warm plum ink, rose accent, gold signal.
    MailSkin.Rose -> if (dark) SkinRamp(
        ink100 = hex(0x180e14), ink200 = hex(0x1f1319), ink300 = hex(0x291b22), border = hex(0x35252d),
        txt1 = hex(0xffffff), txt2 = hex(0xd5cfd2), txt3 = hex(0x8f8890),
        primary300 = hex(0xf290b8), primary500 = hex(0xcc5382), signal400 = hex(0xe0a80c),
    ) else SkinRamp(
        ink100 = hex(0xf9f6f8), ink200 = hex(0xffffff), ink300 = hex(0xf4eef1), border = hex(0xeae1e6),
        txt1 = hex(0x1c1218), txt2 = hex(0x4c4148), txt3 = hex(0x877e84),
        primary300 = hex(0xcc5382), primary500 = hex(0x8e3758), signal400 = hex(0x854d0e),
    )
    // graphite — neutral gray ink, steel accent, ember signal.
    MailSkin.Graphite -> if (dark) SkinRamp(
        ink100 = hex(0x0e0e11), ink200 = hex(0x141418), ink300 = hex(0x1c1c21), border = hex(0x26262c),
        txt1 = hex(0xffffff), txt2 = hex(0xd2d2d5), txt3 = hex(0x88888d),
        primary300 = hex(0xa8b3c2), primary500 = hex(0x6f7d92), signal400 = hex(0xfc6d26),
    ) else SkinRamp(
        ink100 = hex(0xf6f6f7), ink200 = hex(0xffffff), ink300 = hex(0xf0f0f2), border = hex(0xe4e4e7),
        txt1 = hex(0x131316), txt2 = hex(0x434349), txt3 = hex(0x84838a),
        primary300 = hex(0x5c687c), primary500 = hex(0x3a424f), signal400 = hex(0xe24329),
    )
    // sand — warm desert ink, terracotta accent, teal signal.
    MailSkin.Sand -> if (dark) SkinRamp(
        ink100 = hex(0x17120d), ink200 = hex(0x1e1812), ink300 = hex(0x291f17), border = hex(0x362a1f),
        txt1 = hex(0xffffff), txt2 = hex(0xd6d1cb), txt3 = hex(0x8f887e),
        primary300 = hex(0xeda87c), primary500 = hex(0xc96f38), signal400 = hex(0x14b8a6),
    ) else SkinRamp(
        ink100 = hex(0xfaf7f3), ink200 = hex(0xffffff), ink300 = hex(0xf5f0e9), border = hex(0xebe3d7),
        txt1 = hex(0x1c150e), txt2 = hex(0x4c443a), txt3 = hex(0x877c6d),
        primary300 = hex(0xab5c2e), primary500 = hex(0x6b391e), signal400 = hex(0x0d9488),
    )
    // crimson — warm dark ink, ruby accent, gold signal.
    MailSkin.Crimson -> if (dark) SkinRamp(
        ink100 = hex(0x190e0f), ink200 = hex(0x201315), ink300 = hex(0x2b1a1d), border = hex(0x382427),
        txt1 = hex(0xffffff), txt2 = hex(0xd5cfd0), txt3 = hex(0x8f8688),
        primary300 = hex(0xf28b8b), primary500 = hex(0xd24a4a), signal400 = hex(0xe0a80c),
    ) else SkinRamp(
        ink100 = hex(0xf9f6f6), ink200 = hex(0xffffff), ink300 = hex(0xf5eeee), border = hex(0xebe1e1),
        txt1 = hex(0x1c1213), txt2 = hex(0x4c4142), txt3 = hex(0x877e7f),
        primary300 = hex(0xb83b3b), primary500 = hex(0x742525), signal400 = hex(0x854d0e),
    )
    // aurora — blue-green ink, mint accent, violet signal.
    MailSkin.Aurora -> if (dark) SkinRamp(
        ink100 = hex(0x0d1314), ink200 = hex(0x121a1b), ink300 = hex(0x192324), border = hex(0x223031),
        txt1 = hex(0xffffff), txt2 = hex(0xd0d6d6), txt3 = hex(0x86908f),
        primary300 = hex(0x6fd9c4), primary500 = hex(0x21a58d), signal400 = hex(0x9d7bef),
    ) else SkinRamp(
        ink100 = hex(0xf4f8f8), ink200 = hex(0xffffff), ink300 = hex(0xecf3f2), border = hex(0xdfe9e8),
        txt1 = hex(0x101a19), txt2 = hex(0x414d4c), txt3 = hex(0x7c8887),
        primary300 = hex(0x1b8a76), primary500 = hex(0x12564b), signal400 = hex(0x6a45c0),
    )
}

/** Builds the full [TapizColors] set for a given [skin] + light/dark mode. Field names stay
 * identical to the pre-skin 1.x palette so every screen that reads [AppColors] keeps
 * compiling; only the underlying hex values change per skin (ported from theme.css, same
 * ramp source as `AdminColors.kt` in tapiz-lms/apps/android-admin). */
fun tapizColors(skin: MailSkin, dark: Boolean): TapizColors {
    val r = skinRamp(skin, dark)
    return TapizColors(
        isDark = dark,
        canvasTop = r.ink100,
        canvasBottom = r.ink100,
        card = r.ink200,
        heroPanel = if (dark) r.ink300 else r.primary500,
        surfaceSolid = if (dark) r.ink300 else r.ink200,
        cardSubtle = r.ink300,
        inputBackground = r.ink300,
        stroke = r.border,
        shadow = if (dark) Color(0x66000000) else Color(0x1E1E145A),
        primary = if (dark) r.primary300 else r.primary500,
        primaryBright = r.primary300,
        onPrimary = if (dark) Color(0xFFFFFFFF) else Color(0xFFFFFFFF),
        accentSoft = if (dark) r.primary300.copy(alpha = 0.12f) else r.ink300,
        signal = r.signal400,
        coral = if (dark) Color(0xFFE0655A) else Color(0xFFDC2626),
        mint = if (dark) Color(0xFF34D399) else Color(0xFF059669),
        amber = if (dark) Color(0xFFD0A92D) else Color(0xFFC87B00),
        textPrimary = r.txt1,
        textSecondary = r.txt2,
        textMuted = r.txt3,
        categoryTints = listOf(
            r.primary300, r.signal400,
            if (dark) Color(0xFFD0A92D) else Color(0xFFC87B00),
            if (dark) Color(0xFFE0655A) else Color(0xFFDC2626),
            if (dark) Color(0xFF34D399) else Color(0xFF059669),
            if (dark) Color(0xFFC084FC) else Color(0xFF9333EA),
        ),
    )
}

/** Swatch trio for a skin-picker preview (surface / accent / signal), per mode. Mirrors
 *  `swatchFor` in tapiz-lms/apps/android-admin's `AdminColors.kt`. */
data class MailSkinSwatch(val surface: Color, val accent: Color, val signal: Color)

fun swatchFor(skin: MailSkin, dark: Boolean): MailSkinSwatch {
    val c = tapizColors(skin, dark)
    return MailSkinSwatch(surface = c.card, accent = c.primary, signal = c.signal)
}

val DarkColors = tapizColors(MailSkin.Ocean, dark = true)
val LightColors = tapizColors(MailSkin.Ocean, dark = false)

val LocalTapizColors = compositionLocalOf<TapizColors> { DarkColors }

val AppColors: TapizColors
    @androidx.compose.runtime.Composable
    get() = LocalTapizColors.current
