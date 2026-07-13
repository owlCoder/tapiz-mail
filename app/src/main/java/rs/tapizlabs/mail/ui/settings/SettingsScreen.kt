package rs.tapizlabs.mail.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.tapizlabs.mail.data.local.entity.AccountEntity
import rs.tapizlabs.mail.ui.components.MailCard
import rs.tapizlabs.mail.ui.components.MailConfirmDialog
import rs.tapizlabs.mail.ui.components.MailSectionHeader
import rs.tapizlabs.mail.ui.components.SettingsNavRow
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.i18n.Strings
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Settings — top level. Telegram-style grouping: "Accounts" is the one exception that stays
 * inline here (one tap to reach, per explicit product decision — every other group is one tap
 * away *behind* a nav row instead). Everything else is a [SettingsNavRow] entry point into its
 * own sub-screen: [MailSettingsScreen] (sync/swipe/categories), [AppearanceSettingsScreen]
 * (theme/language), [AboutScreen], [PrivacyScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onAddAccount: () -> Unit,
    onEditAccount: (accountId: String) -> Unit,
    onOpenMailSettings: () -> Unit,
    onOpenNotificationsSettings: () -> Unit,
    onOpenAppearanceSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AppColors
    val strings = LocalStrings.current

    var accountPendingRemoval by remember { mutableStateOf<AccountEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle, color = colors.textPrimary) },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AccountsSection(
                accounts = state.accounts,
                onAdd = onAddAccount,
                onEdit = onEditAccount,
                onRemove = { accountPendingRemoval = it },
                strings = strings,
            )

            SettingsNavRow(
                icon = Icons.Outlined.MailOutline,
                title = strings.settingsMailSection,
                subtitle = strings.settingsMailSectionSubtitle,
                onClick = onOpenMailSettings,
            )

            SettingsNavRow(
                icon = Icons.Outlined.NotificationsNone,
                title = strings.settingsNotificationsSection,
                subtitle = strings.settingsNotificationsSectionSubtitle,
                onClick = onOpenNotificationsSettings,
            )

            SettingsNavRow(
                icon = Icons.Outlined.Palette,
                title = strings.settingsAppearanceLanguageSection,
                subtitle = strings.settingsAppearanceLanguageSectionSubtitle,
                onClick = onOpenAppearanceSettings,
            )

            SettingsNavRow(
                icon = Icons.Outlined.PrivacyTip,
                title = strings.settingsPrivacySection,
                subtitle = strings.settingsPrivacySectionSubtitle,
                onClick = onOpenPrivacy,
            )

            SettingsNavRow(
                icon = Icons.Outlined.Info,
                title = strings.settingsAboutSection,
                subtitle = strings.settingsAppVersion(rs.tapizlabs.mail.BuildConfig.VERSION_NAME),
                onClick = onOpenAbout,
            )

            Spacer(Modifier.height(8.dp))
        }
    }

    MailConfirmDialog(
        visible = accountPendingRemoval != null,
        title = strings.settingsRemoveAccountTitle,
        message = strings.settingsRemoveAccountMessage(accountPendingRemoval?.emailAddress.orEmpty()),
        confirmLabel = strings.settingsRemove,
        cancelLabel = strings.settingsCancel,
        onConfirm = {
            accountPendingRemoval?.let(viewModel::removeAccount)
            accountPendingRemoval = null
        },
        onDismiss = { accountPendingRemoval = null },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountsSection(
    accounts: List<AccountEntity>,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onRemove: (AccountEntity) -> Unit,
    strings: Strings,
) {
    val colors = AppColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MailSectionHeader(title = strings.settingsAccountsSection, icon = Icons.Outlined.AccountCircle)

        if (accounts.isEmpty()) {
            Text(
                text = strings.settingsNoAccounts,
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // One card, rows separated by a hairline divider (matches design-reference.html's
        // Settings accounts block) rather than a separate MailCard per account — a compact
        // avatar + email + chevron row; edit opens on tap, delete is long-press (there's no
        // spare row-level slot for a second icon without reintroducing the busier old layout).
        MailCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                accounts.forEachIndexed { index, account ->
                    if (index > 0) HorizontalDivider(color = colors.stroke)
                    AccountRow(
                        account = account,
                        onClick = { onEdit(account.id) },
                        onLongClick = { onRemove(account) },
                    )
                }
                if (accounts.isNotEmpty()) HorizontalDivider(color = colors.stroke)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = onAdd)
                        .padding(horizontal = 10.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = strings.settingsAddAccount,
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountRow(account: AccountEntity, onClick: () -> Unit, onLongClick: () -> Unit) {
    val colors = AppColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color = colors.primary, shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (account.displayName.firstOrNull() ?: '?').uppercaseChar().toString(),
                color = colors.onPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Text(
            text = account.emailAddress,
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textMuted)
    }
}
