package rs.tapizlabs.mail.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.tapizlabs.mail.ui.components.BackArrowButton
import rs.tapizlabs.mail.ui.components.MailCard
import rs.tapizlabs.mail.ui.components.MailPickerSheet
import rs.tapizlabs.mail.ui.components.MailSectionHeader
import rs.tapizlabs.mail.ui.components.PickerSheetOption
import rs.tapizlabs.mail.ui.components.SegmentedOption
import rs.tapizlabs.mail.ui.components.SegmentedPickerCard
import rs.tapizlabs.mail.ui.components.SkinPickerCard
import rs.tapizlabs.mail.ui.i18n.AppLanguage
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.i18n.Strings
import rs.tapizlabs.mail.ui.onboarding.displayName
import rs.tapizlabs.mail.ui.theme.AppColors
import rs.tapizlabs.mail.ui.theme.MailSkin
import rs.tapizlabs.mail.ui.theme.ThemePref

/**
 * "Appearance & language" settings group — theme and app-language pickers, grouped into their
 * own sub-screen (see [MailSettingsScreen] for the sibling "Mail" group — same Telegram-style
 * pattern: the main Settings list only shows entry points). Canvas is [GradientBackground]
 * (ported from tapiz-lms/apps/android) instead of a flat fill, matching the rest of the
 * ecosystem's Ink & Ember screens.
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
                navigationIcon = { BackArrowButton(onBack, modifier = Modifier.padding(start = 8.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MailSectionHeader(
            title = strings.settingsSkinSection,
            icon = Icons.Outlined.Palette,
            subtitle = strings.settingsSkinSectionSubtitle,
        )
        SkinPickerCard(selected = selected, onSelect = onSelect, strings = strings)
    }
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
