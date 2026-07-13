package rs.tapizlabs.mail.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.tapizlabs.mail.ui.components.MailCard
import rs.tapizlabs.mail.ui.components.MailPickerSheet
import rs.tapizlabs.mail.ui.components.MailSectionHeader
import rs.tapizlabs.mail.ui.components.PickerSheetOption
import rs.tapizlabs.mail.ui.components.SegmentedOption
import rs.tapizlabs.mail.ui.components.SegmentedPickerCard
import rs.tapizlabs.mail.ui.i18n.AppLanguage
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.i18n.Strings
import rs.tapizlabs.mail.ui.onboarding.displayName
import rs.tapizlabs.mail.ui.theme.AppColors
import rs.tapizlabs.mail.ui.theme.MailSkin
import rs.tapizlabs.mail.ui.theme.ThemePref
import rs.tapizlabs.mail.ui.theme.swatchFor

/**
 * "Appearance & language" settings group — theme and app-language pickers, grouped into their
 * own sub-screen (see [MailSettingsScreen] for the sibling "Mail" group — same Telegram-style
 * pattern: the main Settings list only shows entry points).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AppColors
    val strings = LocalStrings.current

    var showLanguagePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsAppearanceLanguageSection, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = colors.canvasTop,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ThemeSection(selected = state.themePref, onSelect = viewModel::setTheme, strings = strings)
            SkinSection(selected = state.skinPref, onSelect = viewModel::setSkin, strings = strings)
            LanguageSection(
                selected = state.languagePref,
                strings = strings,
                onOpenPicker = { showLanguagePicker = true },
            )
        }
    }

    // MailSheet/MailPickerSheet must render outside the scrollable Column above — nested
    // inside a Modifier.verticalScroll() parent, its BoxWithConstraints.fillMaxSize() gets
    // an infinite height constraint and crashes ("Vertically scrollable component was
    // measured with an infinity maximum height constraints").
    MailPickerSheet(
        visible = showLanguagePicker,
        title = strings.settingsLanguage,
        options = AppLanguage.entries.map { PickerSheetOption(it, displayName(it, strings)) },
        selected = state.languagePref,
        onSelect = viewModel::setLanguage,
        onDismiss = { showLanguagePicker = false },
    )
}

private val themeOrder = listOf(ThemePref.System, ThemePref.Light, ThemePref.Dark)

@Composable
internal fun ThemeSection(selected: ThemePref, onSelect: (ThemePref) -> Unit, strings: Strings) {
    SegmentedPickerCard(
        options = listOf(
            SegmentedOption(Icons.Outlined.Brightness6, strings.settingsThemeSystem),
            SegmentedOption(Icons.Outlined.LightMode, strings.settingsThemeLight),
            SegmentedOption(Icons.Outlined.DarkMode, strings.settingsThemeDark),
        ),
        selectedIndex = themeOrder.indexOf(selected),
        onSelect = { onSelect(themeOrder[it]) },
        header = {
            MailSectionHeader(
                title = strings.settingsAppearanceSection,
                icon = Icons.Outlined.Palette,
                subtitle = strings.settingsAppearanceSectionSubtitle,
            )
        },
    )
}

@Composable
internal fun SkinSection(selected: MailSkin, onSelect: (MailSkin) -> Unit, strings: Strings) {
    MailCard(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 10.dp)) {
            MailSectionHeader(
                title = strings.settingsSkinSection,
                icon = Icons.Outlined.Palette,
                subtitle = strings.settingsSkinSectionSubtitle,
            )
        }
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            SkinPickerGrid(selected = selected, onSelect = onSelect, strings = strings)
        }
    }
}

/**
 * Skin picker — a 2-column grid of labeled [SkinTile]s, one per [MailSkin], each previewing
 * the skin's surface + accent/signal in the current light/dark mode. Ported 1:1 from
 * tapiz-lms/apps/android-admin's `SkinPickerGrid` (the ecosystem's validated pattern).
 */
@Composable
private fun SkinPickerGrid(selected: MailSkin, onSelect: (MailSkin) -> Unit, strings: Strings) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MailSkin.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { skin ->
                    SkinTile(
                        skin = skin,
                        selected = skin == selected,
                        onSelect = { onSelect(skin) },
                        strings = strings,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** One labeled skin row — a surface square holding two vertical accent/signal pills, the
 * skin name, and a check when active. Ported 1:1 from android-admin's `SkinTile`. */
@Composable
private fun SkinTile(skin: MailSkin, selected: Boolean, onSelect: () -> Unit, strings: Strings, modifier: Modifier = Modifier) {
    val colors = AppColors
    val swatch = swatchFor(skin, colors.isDark)
    val ring by animateColorAsState(if (selected) colors.primary else colors.stroke, label = "skinRing")
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(if (selected) 2.dp else 1.dp, ring, RoundedCornerShape(12.dp))
            .background(if (selected) colors.primary.copy(alpha = if (colors.isDark) 0.12f else 0.10f) else colors.cardSubtle)
            .clickable(onClick = onSelect)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, colors.stroke, RoundedCornerShape(8.dp))
                .background(swatch.surface),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(Modifier.size(width = 6.dp, height = 12.dp).clip(RoundedCornerShape(50)).background(swatch.accent))
                Box(Modifier.size(width = 6.dp, height = 12.dp).clip(RoundedCornerShape(50)).background(swatch.signal))
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            skinLabel(skin, strings),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.primary else colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
        }
    }
}

private fun skinLabel(skin: MailSkin, strings: Strings): String = when (skin) {
    MailSkin.Default -> strings.skinDefault
    MailSkin.Ocean -> strings.skinOcean
    MailSkin.Forest -> strings.skinForest
    MailSkin.Rose -> strings.skinRose
    MailSkin.Graphite -> strings.skinGraphite
    MailSkin.Sand -> strings.skinSand
    MailSkin.Crimson -> strings.skinCrimson
    MailSkin.Aurora -> strings.skinAurora
}

@Composable
internal fun LanguageSection(selected: AppLanguage, onOpenPicker: () -> Unit, strings: Strings) {
    val colors = AppColors

    MailCard(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 10.dp)) {
            MailSectionHeader(
                title = strings.settingsLanguageSection,
                icon = Icons.Outlined.Language,
                subtitle = strings.settingsLanguageSectionSubtitle,
            )
        }
        HorizontalDivider(color = colors.stroke)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenPicker)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = strings.settingsLanguage, color = colors.textMuted)
            Text(text = displayName(selected, strings), color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}
