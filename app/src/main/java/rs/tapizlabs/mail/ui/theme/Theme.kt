package rs.tapizlabs.mail.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun MailTheme(
    themePref: ThemePref = ThemePref.System,
    skin: MailSkin = MailSkin.Ocean,
    content: @Composable () -> Unit,
) {
    val isDark = when (themePref) {
        ThemePref.System -> isSystemInDarkTheme()
        ThemePref.Light -> false
        ThemePref.Dark -> true
    }

    val colors by remember(isDark, skin) {
        mutableStateOf(tapizColors(skin, isDark))
    }

    val materialColorScheme = if (isDark) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.accentSoft,
            onPrimaryContainer = colors.primaryBright,
            secondary = colors.mint,
            onSecondary = colors.onPrimary,
            tertiary = colors.coral,
            background = colors.canvasTop,
            onBackground = colors.textPrimary,
            surface = colors.card,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceSolid,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.stroke,
            outlineVariant = colors.stroke,
            error = colors.coral,
            onError = colors.onPrimary,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.accentSoft,
            onPrimaryContainer = colors.primaryBright,
            secondary = colors.mint,
            onSecondary = colors.onPrimary,
            tertiary = colors.coral,
            background = colors.canvasTop,
            onBackground = colors.textPrimary,
            surface = colors.card,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceSolid,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.stroke,
            outlineVariant = colors.stroke,
            error = colors.coral,
            onError = colors.onPrimary,
        )
    }

    // Keep the system status/navigation bar icons in contrast with the *app*
    // theme (not the OS setting): light (white) icons on the dark canvas, dark
    // icons on the light one.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !isDark
            controller.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(LocalTapizColors provides colors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = MailTypography,
            content = content,
        )
    }
}
