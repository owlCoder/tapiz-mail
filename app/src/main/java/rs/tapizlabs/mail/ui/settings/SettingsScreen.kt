package rs.tapizlabs.mail.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwipeLeft
import androidx.compose.material3.ExperimentalMaterial3Api
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
import rs.tapizlabs.mail.data.local.entity.CategoryEntity
import rs.tapizlabs.mail.data.local.entity.SwipeAction
import rs.tapizlabs.mail.ui.components.MailCard
import rs.tapizlabs.mail.ui.components.MailConfirmDialog
import rs.tapizlabs.mail.ui.components.MailDropdownField
import rs.tapizlabs.mail.ui.components.MailIconChip
import rs.tapizlabs.mail.ui.components.MailSectionHeader
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.i18n.Strings
import rs.tapizlabs.mail.ui.theme.AppColors
import rs.tapizlabs.mail.ui.theme.ThemePref

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onAddAccount: () -> Unit,
    onEditAccount: (accountId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AppColors
    val strings = LocalStrings.current

    var accountPendingRemoval by remember { mutableStateOf<AccountEntity?>(null) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

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
            AccountsSection(
                accounts = state.accounts,
                onAdd = onAddAccount,
                onEdit = onEditAccount,
                onRemove = { accountPendingRemoval = it },
                strings = strings,
            )

            if (state.selectedAccount != null) {
                SyncSection(
                    account = state.selectedAccount!!,
                    onIntervalSelected = { minutes -> viewModel.updateSyncInterval(state.selectedAccount!!, minutes) },
                    strings = strings,
                )

                SwipeActionsSection(
                    leftAction = state.swipeConfig?.swipeLeftAction ?: SwipeAction.ARCHIVE,
                    rightAction = state.swipeConfig?.swipeRightAction ?: SwipeAction.DELETE,
                    onChange = { left, right -> viewModel.updateSwipeConfig(state.selectedAccount!!.id, left, right) },
                    strings = strings,
                )
            }

            CategoriesSection(
                categories = state.categories,
                onAdd = {
                    editingCategory = null
                    showCategorySheet = true
                },
                onEdit = {
                    editingCategory = it
                    showCategorySheet = true
                },
                onDelete = viewModel::deleteCategory,
                strings = strings,
            )

            ThemeSection(selected = state.themePref, onSelect = viewModel::setTheme, strings = strings)

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

    CategoryEditorSheet(
        visible = showCategorySheet,
        category = editingCategory,
        accountId = state.selectedAccount?.id,
        viewModel = viewModel,
        onDismiss = { showCategorySheet = false },
    )
}

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MailSectionHeader(title = strings.settingsAccountsSection, icon = Icons.Outlined.AccountCircle)
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = strings.settingsAddAccount, tint = colors.primary)
            }
        }

        if (accounts.isEmpty()) {
            Text(
                text = strings.settingsNoAccounts,
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        accounts.forEach { account ->
            MailCard(modifier = Modifier.fillMaxWidth(), onClick = { onEdit(account.id) }) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    MailIconChip(icon = Icons.Outlined.AccountCircle)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = account.displayName, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(text = account.emailAddress, color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onEdit(account.id) }) {
                        Icon(Icons.Filled.Edit, contentDescription = strings.settingsEditAccount, tint = colors.textMuted)
                    }
                    IconButton(onClick = { onRemove(account) }) {
                        Icon(Icons.Filled.Delete, contentDescription = strings.settingsRemoveAccount, tint = colors.coral)
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncSection(account: AccountEntity, onIntervalSelected: (Int) -> Unit, strings: Strings) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MailSectionHeader(title = strings.settingsSyncSection, icon = Icons.Outlined.Schedule)
        MailDropdownField(
            label = strings.settingsSyncIntervalLabel,
            options = listOf(15, 30, 60),
            selected = account.syncIntervalMinutes,
            optionLabel = { strings.settingsSyncIntervalMinutes(it) },
            onSelect = onIntervalSelected,
        )
    }
}

@Composable
private fun SwipeActionsSection(
    leftAction: SwipeAction,
    rightAction: SwipeAction,
    onChange: (SwipeAction, SwipeAction) -> Unit,
    strings: Strings,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MailSectionHeader(title = strings.settingsSwipeActionsSection, icon = Icons.Outlined.SwipeLeft)
        MailDropdownField(
            label = strings.settingsSwipeLeft,
            options = SwipeAction.entries,
            selected = leftAction,
            optionLabel = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
            onSelect = { onChange(it, rightAction) },
        )
        MailDropdownField(
            label = strings.settingsSwipeRight,
            options = SwipeAction.entries,
            selected = rightAction,
            optionLabel = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
            onSelect = { onChange(leftAction, it) },
        )
    }
}

@Composable
private fun CategoriesSection(
    categories: List<CategoryEntity>,
    onAdd: () -> Unit,
    onEdit: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
    strings: Strings,
) {
    val colors = AppColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MailSectionHeader(title = strings.settingsCategoriesSection, icon = Icons.Outlined.Label)
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = strings.settingsCategoriesSection, tint = colors.primary)
            }
        }

        if (categories.isEmpty()) {
            Text(
                text = strings.settingsNoCategories,
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        categories.forEach { category ->
            MailCard(modifier = Modifier.fillMaxWidth(), onClick = { onEdit(category) }) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = colors.categoryTints.getOrElse(category.colorIndex) { colors.primary },
                                shape = CircleShape,
                            ),
                    )
                    Text(text = category.name, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (!category.isSystemDefault) {
                        IconButton(onClick = { onDelete(category) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = colors.coral)
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.textMuted)
                }
            }
        }
    }
}

@Composable
private fun ThemeSection(selected: ThemePref, onSelect: (ThemePref) -> Unit, strings: Strings) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MailSectionHeader(title = strings.settingsAppearanceSection, icon = Icons.Outlined.Palette)
        MailDropdownField(
            label = strings.settingsTheme,
            options = listOf(ThemePref.System, ThemePref.Light, ThemePref.Dark),
            selected = selected,
            optionLabel = {
                when (it) {
                    ThemePref.System -> strings.settingsThemeSystem
                    ThemePref.Light -> strings.settingsThemeLight
                    ThemePref.Dark -> strings.settingsThemeDark
                }
            },
            onSelect = onSelect,
        )
    }
}
