package rs.tapizlabs.mail.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import rs.tapizlabs.mail.R

// Variable fonts (single file, weight resolved via FontVariation.Settings) —
// Space Grotesk for display/headline/title, DM Sans for body/label, per
// design_handoff_tapiz_mail_android's "Signal" direction.
@OptIn(ExperimentalTextApi::class)
private fun spaceGrotesk(weight: FontWeight) = Font(
    resId = R.font.space_grotesk,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

@OptIn(ExperimentalTextApi::class)
private fun dmSans(weight: FontWeight) = Font(
    resId = R.font.dm_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.opticalSizing(14.sp),
    ),
)

private val SpaceGroteskSemiBold = FontFamily(spaceGrotesk(FontWeight.SemiBold))
private val SpaceGroteskBold = FontFamily(spaceGrotesk(FontWeight.Bold))
private val SpaceGroteskMedium = FontFamily(spaceGrotesk(FontWeight.Medium))
private val DMSansNormal = FontFamily(dmSans(FontWeight.Normal))
private val DMSansMedium = FontFamily(dmSans(FontWeight.Medium))
private val DMSansBold = FontFamily(dmSans(FontWeight.Bold))

val MailTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SpaceGroteskBold,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = SpaceGroteskBold,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = SpaceGroteskBold,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = SpaceGroteskSemiBold,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGroteskSemiBold,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = SpaceGroteskBold,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = SpaceGroteskBold,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceGroteskMedium,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = SpaceGroteskMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = DMSansNormal,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = DMSansNormal,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = DMSansNormal,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = DMSansBold,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = DMSansMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = DMSansMedium,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)
