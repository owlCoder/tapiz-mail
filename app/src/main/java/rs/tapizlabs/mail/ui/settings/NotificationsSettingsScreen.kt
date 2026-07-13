package rs.tapizlabs.mail.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.tapizlabs.mail.ui.components.MailCard
import rs.tapizlabs.mail.ui.components.MailSectionHeader
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * "Notifications" settings group — an on/off toggle for new-mail notifications plus a
 * secondary sound toggle (disabled unless notifications are on; defaults to off — see
 * [rs.tapizlabs.mail.core.local.PrefsStore.notificationSoundEnabledPref]), both gating
 * [rs.tapizlabs.mail.sync.NewMailNotifier]. First `Switch`es in this codebase — every other
 * Settings row so far is a tap-to-open-picker pattern
 * ([rs.tapizlabs.mail.ui.components.SettingsNavRow]/`MailPickerSheet`), but binary on/off reads
 * better as a direct toggle than a picker sheet with two options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(
    onBack: () -> Unit,
    onRequestSystemPermission: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AppColors
    val strings = LocalStrings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsNotificationsSection, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MailSectionHeader(
                title = strings.settingsNotificationsSection,
                icon = Icons.Outlined.NotificationsNone,
                subtitle = strings.settingsNotificationsSectionSubtitle,
            )

            MailCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = strings.settingsNotificationsToggleLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = strings.settingsNotificationsToggleSubtext,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    }
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setNotificationsEnabled(enabled)
                            // Turning the app-level toggle back on is meaningless if the OS
                            // permission was previously denied/never granted — re-request it
                            // here (no-op on Android <13 or if already granted) instead of the
                            // user having to dig into system app-info settings themselves.
                            if (enabled) onRequestSystemPermission()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary,
                            checkedBorderColor = Color.Transparent,
                        ),
                    )
                }
            }

            MailCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = strings.settingsNotificationsSoundToggleLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = strings.settingsNotificationsSoundToggleSubtext,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    }
                    Switch(
                        checked = state.notificationSoundEnabled,
                        onCheckedChange = { enabled -> viewModel.setNotificationSoundEnabled(enabled) },
                        enabled = state.notificationsEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary,
                            checkedBorderColor = Color.Transparent,
                        ),
                    )
                }
            }

            // Background reliability — only shown while the OS can still throttle
            // us (the MIUI/HyperOS problem where sync stops until the app opens).
            val context = androidx.compose.ui.platform.LocalContext.current
            var batteryExempt by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(
                    rs.tapizlabs.mail.sync.BatteryOptimization.isIgnoringOptimizations(context),
                )
            }
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        batteryExempt = rs.tapizlabs.mail.sync.BatteryOptimization.isIgnoringOptimizations(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(obs)
                onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
            }
            if (!batteryExempt) {
                MailCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rs.tapizlabs.mail.sync.BatteryOptimization.requestExemption(context) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = strings.settingsBatteryLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = strings.settingsBatterySubtext,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                            )
                        }
                        Icon(
                            Icons.Outlined.BatteryAlert,
                            contentDescription = null,
                            tint = colors.primary,
                        )
                    }
                }
            }
        }
    }
}
