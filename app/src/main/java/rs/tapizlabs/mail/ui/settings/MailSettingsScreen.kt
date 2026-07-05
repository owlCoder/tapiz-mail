package rs.tapizlabs.mail.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwipeLeft
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
import rs.tapizlabs.mail.data.local.entity.CategoryEntity
import rs.tapizlabs.mail.data.local.entity.SwipeAction
import rs.tapizlabs.mail.ui.components.MailCard
import rs.tapizlabs.mail.ui.components.MailPickerSheet
import rs.tapizlabs.mail.ui.components.MailSectionHeader
import rs.tapizlabs.mail.ui.components.PickerSheetOption
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.i18n.Strings
import rs.tapizlabs.mail.ui.theme.AppColors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size

/**
 * "Mail" settings group — sync interval, swipe actions, and category rules for the currently
 * selected account, grouped into their own sub-screen (Telegram-style: the main Settings list
 * only shows entry points, this is what opens when the "Mail" row is tapped). Content here is
 * the same [SyncSection]/[SwipeActionsSection]/[CategoriesSection] previously inlined directly
 * on the main Settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AppColors
    val strings = LocalStrings.current

    var showCategorySheet by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showSyncIntervalPicker by remember { mutableStateOf(false) }
    var showSwipeLeftPicker by remember { mutableStateOf(false) }
    var showSwipeRightPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsMailSection, color = colors.textPrimary) },
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.selectedAccount != null) {
                SyncSection(
                    account = state.selectedAccount!!,
                    onOpenPicker = { showSyncIntervalPicker = true },
                    strings = strings,
                )

                SwipeActionsSection(
                    leftAction = state.swipeConfig?.swipeLeftAction ?: SwipeAction.DELETE,
                    rightAction = state.swipeConfig?.swipeRightAction ?: SwipeAction.MARK_READ,
                    onOpenLeftPicker = { showSwipeLeftPicker = true },
                    onOpenRightPicker = { showSwipeRightPicker = true },
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
        }
    }

    CategoryEditorSheet(
        visible = showCategorySheet,
        category = editingCategory,
        accountId = state.selectedAccount?.id,
        viewModel = viewModel,
        strings = strings,
        onDismiss = { showCategorySheet = false },
    )

    // MailSheet/MailPickerSheet must render outside the scrollable Column above — nested
    // inside a Modifier.verticalScroll() parent, its BoxWithConstraints.fillMaxSize() gets
    // an infinite height constraint and crashes ("Vertically scrollable component was
    // measured with an infinity maximum height constraints").
    if (state.selectedAccount != null) {
        MailPickerSheet(
            visible = showSyncIntervalPicker,
            title = strings.settingsSyncIntervalLabel,
            options = listOf(15, 30, 60).map { PickerSheetOption(it, strings.settingsSyncIntervalMinutes(it)) },
            selected = state.selectedAccount!!.syncIntervalMinutes,
            onSelect = { minutes -> viewModel.updateSyncInterval(state.selectedAccount!!, minutes) },
            onDismiss = { showSyncIntervalPicker = false },
        )
        val currentLeft = state.swipeConfig?.swipeLeftAction ?: SwipeAction.DELETE
        val currentRight = state.swipeConfig?.swipeRightAction ?: SwipeAction.MARK_READ
        MailPickerSheet(
            visible = showSwipeLeftPicker,
            title = strings.settingsSwipeLeft,
            options = SwipeAction.entries.map { PickerSheetOption(it, swipeActionLabel(it, strings)) },
            selected = currentLeft,
            onSelect = { action -> viewModel.updateSwipeConfig(state.selectedAccount!!.id, action, currentRight) },
            onDismiss = { showSwipeLeftPicker = false },
        )
        MailPickerSheet(
            visible = showSwipeRightPicker,
            title = strings.settingsSwipeRight,
            options = SwipeAction.entries.map { PickerSheetOption(it, swipeActionLabel(it, strings)) },
            selected = currentRight,
            onSelect = { action -> viewModel.updateSwipeConfig(state.selectedAccount!!.id, currentLeft, action) },
            onDismiss = { showSwipeRightPicker = false },
        )
    }
}

@Composable
internal fun SyncSection(account: AccountEntity, onOpenPicker: () -> Unit, strings: Strings) {
    val colors = AppColors
    MailCard(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 10.dp)) {
            MailSectionHeader(
                title = strings.settingsSyncSection,
                icon = Icons.Outlined.Schedule,
                subtitle = strings.settingsSyncSectionSubtitle,
            )
        }
        HorizontalDivider(color = colors.stroke)
        SettingsFieldRow(
            label = strings.settingsSyncIntervalLabel,
            value = strings.settingsSyncIntervalMinutes(account.syncIntervalMinutes),
            onClick = onOpenPicker,
        )
    }
}

@Composable
internal fun SwipeActionsSection(
    leftAction: SwipeAction,
    rightAction: SwipeAction,
    onOpenLeftPicker: () -> Unit,
    onOpenRightPicker: () -> Unit,
    strings: Strings,
) {
    val colors = AppColors
    MailCard(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 10.dp)) {
            MailSectionHeader(
                title = strings.settingsSwipeActionsSection,
                icon = Icons.Outlined.SwipeLeft,
                subtitle = strings.settingsSwipeActionsSectionSubtitle,
            )
        }
        HorizontalDivider(color = colors.stroke)
        SettingsFieldRow(
            label = strings.settingsSwipeLeft,
            value = swipeActionLabel(leftAction, strings),
            onClick = onOpenLeftPicker,
        )
        HorizontalDivider(color = colors.stroke)
        SettingsFieldRow(
            label = strings.settingsSwipeRight,
            value = swipeActionLabel(rightAction, strings),
            onClick = onOpenRightPicker,
        )
    }
}

/** One tappable label/value row inside a group [MailCard] — e.g. "Swipe left" / "Delete" —
 * used so a whole settings group (header + all its rows) reads as a single bordered card,
 * matching the Accounts section's own multi-row card instead of one card per field. */
@Composable
private fun SettingsFieldRow(label: String, value: String, onClick: () -> Unit) {
    val colors = AppColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = colors.textMuted)
        Text(text = value, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
    }
}

private fun swipeActionLabel(action: SwipeAction, strings: Strings): String = when (action) {
    SwipeAction.DELETE -> strings.swipeActionDelete
    SwipeAction.MARK_READ -> strings.swipeActionMarkRead
    SwipeAction.MARK_UNREAD -> strings.swipeActionMarkUnread
    SwipeAction.NONE -> strings.swipeActionNone
}

@Composable
internal fun CategoriesSection(
    categories: List<CategoryEntity>,
    onAdd: () -> Unit,
    onEdit: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
    strings: Strings,
) {
    val colors = AppColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MailCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 14.dp, end = 6.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MailSectionHeader(
                    title = strings.settingsCategoriesSection,
                    icon = Icons.Outlined.Label,
                    subtitle = strings.settingsCategoriesSectionSubtitle,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = strings.settingsCategoriesSection, tint = colors.primary)
                }
            }
            if (categories.isEmpty()) {
                HorizontalDivider(color = colors.stroke)
                Text(
                    text = strings.settingsNoCategories,
                    color = colors.textMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(14.dp),
                )
            }
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
