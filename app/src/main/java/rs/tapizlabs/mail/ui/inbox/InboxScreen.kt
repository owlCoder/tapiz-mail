package rs.tapizlabs.mail.ui.inbox

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.outlined.AllInbox
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.tapizlabs.mail.data.local.entity.SwipeAction
import rs.tapizlabs.mail.ui.components.CategoryChipsRow
import rs.tapizlabs.mail.ui.components.MailConfirmDialog
import rs.tapizlabs.mail.ui.components.MailDateFormat
import rs.tapizlabs.mail.ui.components.MailGhostButton
import rs.tapizlabs.mail.ui.components.MailIconChip
import rs.tapizlabs.mail.ui.components.MailPulseSpinner
import rs.tapizlabs.mail.ui.components.MailSheet
import rs.tapizlabs.mail.ui.components.SkeletonMessageList
import rs.tapizlabs.mail.ui.components.SwipeableMessageRow
import rs.tapizlabs.mail.ui.i18n.LocalAppLanguage
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.i18n.Strings
import rs.tapizlabs.mail.ui.i18n.toLocale
import rs.tapizlabs.mail.ui.model.AccountSummaryUi
import rs.tapizlabs.mail.ui.model.CategoryChipUi
import rs.tapizlabs.mail.ui.model.MessageListItemUi
import rs.tapizlabs.mail.ui.search.SearchScreen
import rs.tapizlabs.mail.ui.theme.AppColors
import java.time.LocalDate
import java.util.Locale

/**
 * Inbox — the app's home screen (no bottom nav bar, per
 * design_handoff_tapiz_mail_android/design-reference.html: full-bleed, no tab bar at all).
 * Header row (account avatar + email + "N accounts synced" + search/settings tiles),
 * big unread-count headline, pill category tabs, day-grouped message list, FAB.
 *
 * @param onOpenMessage navigate to Mail Detail for the given message id.
 * @param onOpenDraft navigate to Compose in edit mode for a local draft — used instead of
 * [onOpenMessage] when the Drafts pseudo-category is selected, since a draft needs editing,
 * not a read-only detail view.
 * @param onAddAccount navigate to Add-Account — invoked from the account switcher's
 * "Add account" entry.
 * @param onCompose navigate to Compose — invoked from the FAB (reference shows a plain
 * circular pencil FAB, bottom-right) — the only way to reach Compose, no tab for it.
 * Search is a local full-screen overlay (Gmail-style: no NavHost route/back-stack entry),
 * toggled by the top-bar search icon (a deliberate deviation from the reference, which shows
 * no search entry point at all; Search still needs *some* way in).
 * @param onSettings navigate to Settings — invoked from the top-bar "sliders" tile, which
 * the reference shows purely decoratively but this app wires to an actual destination
 * since there's no bottom-nav Settings tab anymore.
 *
 * Inbox/Drafts/Trash are reached via fixed pseudo-category chips in the same row as the
 * user's own categories (see [rs.tapizlabs.mail.ui.inbox.PSEUDO_CATEGORY_DRAFTS]/
 * [rs.tapizlabs.mail.ui.inbox.PSEUDO_CATEGORY_TRASH]) — deliberately the *only* entry point,
 * so there's no separate top-bar Drafts icon duplicating that access.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onOpenMessage: (messageId: String) -> Unit,
    onOpenDraft: (messageId: String) -> Unit,
    onAddAccount: () -> Unit,
    onCompose: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = AppColors
    val strings = LocalStrings.current
    val locale = LocalAppLanguage.current.toLocale()
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    var showAccountSwitcher by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showSearch) { showSearch = false }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvasTop)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            InboxTopBar(
                accounts = uiState.accounts,
                selectedAccountId = uiState.selectedAccountId,
                onOpenAccountSwitcher = { showAccountSwitcher = true },
                onSearch = { showSearch = true },
                onSettings = onSettings,
                strings = strings,
            )

            if (!uiState.isLoading) {
                InboxHeadline(uiState = uiState, strings = strings)
            }

            // Inbox/category badges are unread counts, Drafts/Trash are item counts; Sent
            // carries no badge — a total there says nothing actionable.
            val pseudoChips = listOf(
                CategoryChipUi(id = null, name = strings.inboxChipInbox, count = uiState.inboxUnreadCount, colorIndex = 0),
                CategoryChipUi(id = PSEUDO_CATEGORY_SENT, name = strings.inboxChipSent, count = 0, colorIndex = 0),
                CategoryChipUi(id = PSEUDO_CATEGORY_DRAFTS, name = strings.inboxChipDrafts, count = uiState.draftsCount, colorIndex = 0),
                CategoryChipUi(id = PSEUDO_CATEGORY_TRASH, name = strings.inboxChipTrash, count = uiState.trashCount, colorIndex = 0),
            )
            CategoryChipsRow(
                categories = pseudoChips + uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onSelectCategory = viewModel::selectCategory,
            )

            AnimatedVisibility(visible = uiState.syncFailed) {
                SyncErrorBanner(
                    strings = strings,
                    onRetry = { viewModel.refresh() },
                    onDismiss = viewModel::dismissSyncError,
                )
            }

            if (uiState.isTrashSelected && uiState.messages.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    MailGhostButton(
                        text = strings.trashEmptyAllLabel,
                        icon = Icons.Outlined.DeleteSweep,
                        onClick = { showEmptyTrashConfirm = true },
                        height = 36.dp,
                    )
                }
            }

            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = pullState,
                modifier = Modifier.fillMaxSize(),
            ) {
                // AnimatedContent keyed on the selected chip (Inbox/Drafts/Trash/category) —
                // same horizontal slide+fade as the NavHost's default screen-to-screen
                // transition (Compose push/pop), so switching chips reads as consistent with
                // the rest of the app's navigation rather than an unrelated vertical motion.
                AnimatedContent(
                    targetState = uiState.selectedCategoryId,
                    transitionSpec = {
                        (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 6 })
                            .togetherWith(
                                fadeOut(tween(140, easing = LinearOutSlowInEasing)) +
                                    slideOutHorizontally(tween(140, easing = LinearOutSlowInEasing)) { -it / 6 },
                            )
                    },
                    label = "inbox_category_transition",
                ) {
                    when {
                        uiState.isLoading -> InboxLoadingState()
                        uiState.messages.isEmpty() -> when {
                            uiState.isDraftsSelected ->
                                InboxEmptyState(title = strings.draftsEmpty, subtitle = strings.draftsEmptySubtext)
                            // Sent/Trash being empty needs no explanation about syncing.
                            uiState.isSentSelected || uiState.isTrashSelected ->
                                InboxEmptyState(title = strings.inboxNoMessages, subtitle = null)
                            else ->
                                InboxEmptyState(title = strings.inboxNoMessages, subtitle = strings.inboxNoMessagesSubtext)
                        }
                        else -> {
                        val listState = rememberLazyListState()

                        // Fires loadMore() once the user scrolls within 5 rows of the bottom —
                        // only for the main Inbox/Sent views (loadMore() itself no-ops for
                        // Drafts/Trash/user categories, see its doc), so a large mailbox's
                        // rest becomes reachable by scrolling instead of only ever showing
                        // the newest INITIAL_SYNC_LIMIT messages from first sync.
                        val shouldLoadMore by remember {
                            derivedStateOf {
                                val layoutInfo = listState.layoutInfo
                                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                lastVisible >= layoutInfo.totalItemsCount - 5
                            }
                        }
                        LaunchedEffect(shouldLoadMore, uiState.messages.size) {
                            if (shouldLoadMore) viewModel.loadMore()
                        }

                        // New mail is inserted above the current first row, and LazyColumn keeps
                        // that row anchored in place — so the new message lands just off-screen
                        // and nothing visibly happens. When the user is at (or within a few
                        // rows of) the top, follow the list up to reveal it; deeper in the
                        // list, their reading position is left alone.
                        val newestMessageId = uiState.messages.firstOrNull()?.id
                        LaunchedEffect(newestMessageId) {
                            // One frame, so the list has re-anchored after the insert before
                            // its position is read.
                            withFrameNanos { }
                            val nearTop = listState.firstVisibleItemIndex in 1..NEW_MAIL_FOLLOW_ROWS
                            if (nearTop && !listState.isScrollInProgress) listState.animateScrollToItem(0)
                        }

                        // One pass over the list per data change, instead of formatting two
                        // dates for every visible row on every recomposition while scrolling.
                        val dayHeaders = remember(uiState.messages, strings, locale) {
                            dayHeaders(uiState.messages, strings, locale)
                        }
                        val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            // Bottom padding clears the FAB, so the last row's star/snippet
                            // can be scrolled out from underneath it.
                            contentPadding = PaddingValues(
                                start = 14.dp,
                                end = 14.dp,
                                top = 4.dp,
                                bottom = 92.dp + navigationBarInset,
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            itemsIndexed(
                                items = uiState.messages,
                                key = { _, message -> message.id },
                                contentType = { _, _ -> "message" },
                            ) { index, message ->
                                dayHeaders.getOrNull(index)?.let { DaySectionLabel(text = it) }
                                SwipeableMessageRow(
                                    message = message,
                                    onClick = {
                                        if (uiState.isDraftsSelected) {
                                            onOpenDraft(message.id)
                                        } else {
                                            onOpenMessage(message.id)
                                        }
                                    },
                                    onToggleStar = { viewModel.toggleStar(message.id, message.isStarred) },
                                    onSwipeLeft = {
                                        // Inside Trash, swipe-left is the explicit "empty it"
                                        // gesture (permanent delete) rather than re-applying the
                                        // account's normal Delete-to-Trash swipe config.
                                        if (uiState.isTrashSelected) {
                                            viewModel.deleteMessage(message.id)
                                        } else {
                                            viewModel.applySwipeAction(message.id, uiState.swipeLeftAction)
                                        }
                                    },
                                    onSwipeRight = {
                                        // Inside Trash, swipe-right restores the message back to
                                        // where it came from instead of applying the swipe config.
                                        if (uiState.isTrashSelected) {
                                            viewModel.restoreMessage(message.id)
                                        } else {
                                            viewModel.applySwipeAction(message.id, uiState.swipeRightAction)
                                        }
                                    },
                                    leftAction = if (uiState.isTrashSelected) SwipeAction.DELETE else uiState.swipeLeftAction,
                                    rightAction = if (uiState.isTrashSelected) SwipeAction.MARK_UNREAD else uiState.swipeRightAction,
                                    rightIconOverride = if (uiState.isTrashSelected) Icons.Outlined.RestoreFromTrash else null,
                                    isDraft = uiState.isDraftsSelected,
                                    modifier = Modifier
                                        .animateItem(
                                            // Explicit, softer specs (matching the app's
                                            // 220ms-in/280ms-out signature feel) instead of
                                            // Compose's default fade/spring — the default
                                            // disappearance was fast enough that a single
                                            // swiped-away row read as an abrupt jump/skip
                                            // rather than a smooth removal (contrast with
                                            // "Empty trash", which removes rows one-by-one
                                            // with a gap between each, so the default timing
                                            // never looked rushed there).
                                            fadeOutSpec = tween(280, easing = LinearOutSlowInEasing),
                                            placementSpec = tween(280, easing = FastOutSlowInEasing),
                                        )
                                        .clip(RoundedCornerShape(10.dp)),
                                )
                            }
                            if (uiState.isLoadingMore) {
                                item(contentType = "loading") {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        MailPulseSpinner(size = 28.dp, showIcon = false)
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }
        }

        // Plain circular pencil FAB, bottom-right — matches the reference exactly
        // (56x56, 18dp radius square-ish shadhtml uses border-radius:18px on a square
        // box, not a full circle) rather than an extended/labeled FAB.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp)
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.primary)
                .clickable(onClick = onCompose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = strings.composeNewMessage,
                tint = colors.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        }

        // Floating search overlay above the inbox content (no NavHost route/back-stack entry,
        // the inbox stays mounted underneath). SearchScreen owns its own scrim + drop-in
        // animation now, so it's rendered unconditionally and just toggled via `visible`.
        SearchScreen(
            visible = showSearch,
            onOpenMessage = onOpenMessage,
            onDismiss = { showSearch = false },
            modifier = Modifier.fillMaxSize(),
        )

        AccountSwitcherSheet(
            visible = showAccountSwitcher,
            accounts = uiState.accounts,
            selectedAccountId = uiState.selectedAccountId,
            onSelectAccount = viewModel::selectAccount,
            onAddAccount = onAddAccount,
            onDismiss = { showAccountSwitcher = false },
            strings = strings,
        )

        MailConfirmDialog(
            visible = showEmptyTrashConfirm,
            title = strings.trashEmptyAllConfirmTitle,
            message = strings.trashEmptyAllConfirmMessage,
            confirmLabel = strings.trashEmptyAllConfirmButton,
            cancelLabel = strings.settingsCancel,
            onConfirm = {
                viewModel.emptyTrash()
                showEmptyTrashConfirm = false
            },
            onDismiss = { showEmptyTrashConfirm = false },
        )
    }
}

@Composable
private fun InboxTopBar(
    accounts: List<AccountSummaryUi>,
    selectedAccountId: String?,
    onOpenAccountSwitcher: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    strings: Strings,
) {
    val colors = AppColors
    // With a single account, "all accounts" IS that account — show it by name rather than
    // a generic "All accounts" label.
    val selectedAccount = accounts.find { it.id == selectedAccountId } ?: accounts.singleOrNull()
    val interactionSource = remember { MutableInteractionSource() }
    val tileShape = RoundedCornerShape(11.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onOpenAccountSwitcher,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // "All accounts" has no single owner to take an initial from.
            AccountInitialTile(initial = selectedAccount?.displayName?.trim()?.firstOrNull() ?: '@')
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = selectedAccount?.emailAddress ?: selectedAccount?.displayName ?: strings.inboxAllAccounts,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = strings.inboxAccountsSynced(accounts.size),
                    style = MaterialTheme.typography.labelSmall.copy(color = colors.primary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Marks the header as opening the account switcher before it's tapped — same
            // glyph as Compose's switchable "From" row.
            Icon(
                imageVector = Icons.Outlined.UnfoldMore,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.padding(horizontal = 6.dp).size(16.dp),
            )
        }

        // Search icon — a deliberate deviation from the reference (which shows no search
        // entry point at all on this screen), since Search still needs some way in now
        // that there's no bottom-nav tab for it.
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(tileShape)
                .background(colors.cardSubtle)
                .clickable(onClick = onSearch),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = strings.searchPlaceholder,
                tint = colors.textPrimary,
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(tileShape)
                .background(colors.cardSubtle)
                .clickable(onClick = onSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = strings.settingsTitle,
                tint = colors.textPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Rounded-square account avatar (owner's initial on the primary color) — the Inbox header
 * and the account switcher rows share it. */
@Composable
private fun AccountInitialTile(initial: Char) {
    val colors = AppColors
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial.uppercaseChar().toString(),
            style = MaterialTheme.typography.titleSmall.copy(
                color = colors.onPrimary,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/** Account switcher on the shared [MailSheet] (scrim tap/back = cancel, 80% height cap) —
 * "All accounts" (only with more than one), each account, then the single "Add account"
 * action. */
@Composable
private fun AccountSwitcherSheet(
    visible: Boolean,
    accounts: List<AccountSummaryUi>,
    selectedAccountId: String?,
    onSelectAccount: (String?) -> Unit,
    onAddAccount: () -> Unit,
    onDismiss: () -> Unit,
    strings: Strings,
) {
    val colors = AppColors
    MailSheet(visible = visible, onDismiss = onDismiss) {
        Text(
            text = strings.settingsAccountsSection,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        if (accounts.size > 1) {
            AccountSwitcherRow(
                leading = { MailIconChip(icon = Icons.Outlined.AllInbox, size = 36.dp) },
                title = strings.inboxAllAccounts,
                subtitle = null,
                selected = selectedAccountId == null,
                onClick = {
                    onSelectAccount(null)
                    onDismiss()
                },
            )
        }
        accounts.forEach { account ->
            AccountSwitcherRow(
                leading = { AccountInitialTile(initial = account.displayName.trim().firstOrNull() ?: '@') },
                title = account.displayName,
                subtitle = account.emailAddress,
                selected = account.id == selectedAccountId || accounts.size == 1,
                onClick = {
                    onSelectAccount(account.id)
                    onDismiss()
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        MailGhostButton(
            text = strings.inboxAddAccount,
            icon = Icons.Filled.Add,
            onClick = {
                onDismiss()
                onAddAccount()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AccountSwitcherRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = AppColors
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = colors.primary)
        }
    }
}

/** Big headline + subcopy directly below the top bar — "N unread" for the Inbox and user
 * categories (matches the reference's "24 unread" / "Sorted automatically by your rules"
 * block); the Sent/Drafts/Trash views name themselves and show their item count instead,
 * since "no unread" says nothing useful about a list of sent mail or drafts. */
@Composable
private fun InboxHeadline(uiState: InboxUiState, strings: Strings) {
    val colors = AppColors
    val isPseudoView = uiState.isSentSelected || uiState.isDraftsSelected || uiState.isTrashSelected
    val title = when {
        uiState.isSentSelected -> strings.inboxChipSent
        uiState.isDraftsSelected -> strings.inboxChipDrafts
        uiState.isTrashSelected -> strings.inboxChipTrash
        else -> {
            val unread = uiState.messages.count { !it.isRead }
            if (unread == 0) strings.inboxNoUnread else strings.inboxUnreadCount(unread)
        }
    }
    val subtitle = if (isPseudoView) strings.inboxMessagesCount(uiState.messages.size) else strings.inboxUnreadSubtext
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary),
        )
    }
}

/** Shown under the chips after a user-initiated refresh fails — the cached list stays
 * usable underneath, this only explains why nothing new arrived and offers a retry. */
@Composable
private fun SyncErrorBanner(strings: Strings, onRetry: () -> Unit, onDismiss: () -> Unit) {
    val colors = AppColors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(shape)
            .background(colors.coral.copy(alpha = 0.12f))
            .border(width = 1.dp, color = colors.coral.copy(alpha = 0.35f), shape = shape)
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = colors.coral,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = strings.inboxSyncFailed,
            style = MaterialTheme.typography.bodySmall.copy(color = colors.textPrimary),
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRetry)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = strings.inboxRetry,
                style = MaterialTheme.typography.labelLarge.copy(color = colors.primary, fontWeight = FontWeight.SemiBold),
            )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = strings.composeCancel,
                tint = colors.textMuted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun InboxEmptyState(title: String, subtitle: String?) {
    val colors = AppColors
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Must stay scrollable even with no content — PullToRefreshBox detects the
            // pull gesture via nested scroll from its child, so a non-scrollable empty
            // state would silently swallow the swipe and refresh would never fire.
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MailOutline,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = colors.textMuted),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun InboxLoadingState() {
    SkeletonMessageList(modifier = Modifier.fillMaxSize().padding(top = 4.dp))
}

/** "Today"/"Yesterday" section label above a run of same-day messages — reference:
 * 10.5px bold uppercase, tracked out, muted color. */
@Composable
private fun DaySectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            color = AppColors.textSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 10.5.sp,
            letterSpacing = 0.5.sp,
        ),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
    )
}

/** How many rows from the top still counts as "at the top" for following newly arrived
 * mail into view (see the list's `newestMessageId` effect). */
private const val NEW_MAIL_FOLLOW_ROWS = 3

/** Section label for each row, or null where a row continues the previous row's day —
 * index-aligned with [messages]. */
private fun dayHeaders(messages: List<MessageListItemUi>, strings: Strings, locale: Locale): List<String?> {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    var previousDay: LocalDate? = null
    return messages.map { message ->
        val day = MailDateFormat.localDate(message.sentAt)
        if (day == previousDay) return@map null
        previousDay = day
        when (day) {
            today -> strings.inboxToday
            yesterday -> strings.inboxYesterday
            else -> MailDateFormat.shortDate(day, locale)
        }
    }
}
