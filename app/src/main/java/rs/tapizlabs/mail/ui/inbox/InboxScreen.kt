package rs.tapizlabs.mail.ui.inbox

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import rs.tapizlabs.mail.data.local.entity.SwipeAction
import rs.tapizlabs.mail.ui.components.CategoryChipsRow
import rs.tapizlabs.mail.ui.components.SkeletonMessageList
import rs.tapizlabs.mail.ui.components.SwipeableMessageRow
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.i18n.Strings
import rs.tapizlabs.mail.ui.model.AccountSummaryUi
import rs.tapizlabs.mail.ui.model.CategoryChipUi
import rs.tapizlabs.mail.ui.search.SearchScreen
import rs.tapizlabs.mail.ui.theme.AppColors
import java.time.Instant
import java.time.ZoneId

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
    val uiState by viewModel.uiState.collectAsState()
    val colors = AppColors
    val strings = LocalStrings.current
    var showSearch by rememberSaveable { mutableStateOf(false) }

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
                onSelectAccount = viewModel::selectAccount,
                onAddAccount = onAddAccount,
                onSearch = { showSearch = true },
                onSettings = onSettings,
                strings = strings,
            )

            if (!uiState.isLoading) {
                UnreadCountHeader(count = uiState.messages.count { !it.isRead }, strings = strings)
            }

            val pseudoChips = listOf(
                CategoryChipUi(id = null, name = strings.inboxChipInbox, count = uiState.inboxCount, colorIndex = 0),
                CategoryChipUi(id = PSEUDO_CATEGORY_DRAFTS, name = strings.inboxChipDrafts, count = uiState.draftsCount, colorIndex = 0),
                CategoryChipUi(id = PSEUDO_CATEGORY_TRASH, name = strings.inboxChipTrash, count = uiState.trashCount, colorIndex = 0),
            )
            CategoryChipsRow(
                categories = pseudoChips + uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onSelectCategory = viewModel::selectCategory,
            )

            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                state = pullState,
                modifier = Modifier.fillMaxSize(),
            ) {
                // Crossfade keyed on the selected chip (Inbox/Drafts/Trash/category) so
                // switching between them fades the list content instead of an abrupt cut —
                // isLoading/empty/list are all included in the same crossfaded surface.
                Crossfade(
                    targetState = uiState.selectedCategoryId,
                    animationSpec = tween(180),
                    label = "inbox_category_crossfade",
                ) {
                    when {
                        uiState.isLoading -> InboxLoadingState()
                        uiState.messages.isEmpty() -> InboxEmptyState(strings)
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            itemsIndexed(uiState.messages, key = { _, message -> message.id }) { index, message ->
                                val previousLabel = uiState.messages.getOrNull(index - 1)?.let { dayLabel(it.sentAt, strings) }
                                val label = dayLabel(message.sentAt, strings)
                                if (label != previousLabel) {
                                    DaySectionLabel(text = label)
                                }
                                SwipeableMessageRow(
                                    message = message,
                                    onClick = {
                                        if (uiState.selectedCategoryId == PSEUDO_CATEGORY_DRAFTS) {
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
                                        // the Inbox instead of applying the normal swipe config.
                                        if (uiState.isTrashSelected) {
                                            viewModel.restoreMessage(message.id)
                                        } else {
                                            viewModel.applySwipeAction(message.id, uiState.swipeRightAction)
                                        }
                                    },
                                    leftAction = if (uiState.isTrashSelected) SwipeAction.DELETE else uiState.swipeLeftAction,
                                    rightAction = if (uiState.isTrashSelected) SwipeAction.MARK_UNREAD else uiState.swipeRightAction,
                                    modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                                )
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
                .padding(20.dp)
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.primary)
                .clickable(onClick = onCompose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Compose",
                tint = colors.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        }

        // Full-screen overlay above the inbox content (Gmail-style search: no NavHost
        // route/back-stack entry, the inbox just stays mounted underneath).
        AnimatedVisibility(
            visible = showSearch,
            enter = slideInVertically(tween(220, easing = FastOutSlowInEasing)) { -it } +
                fadeIn(tween(220, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(tween(160, easing = LinearOutSlowInEasing)) { -it } +
                fadeOut(tween(160, easing = LinearOutSlowInEasing)),
            modifier = Modifier.fillMaxSize(),
        ) {
            SearchScreen(
                onOpenMessage = onOpenMessage,
                onBack = { showSearch = false },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun InboxTopBar(
    accounts: List<AccountSummaryUi>,
    selectedAccountId: String?,
    onSelectAccount: (String?) -> Unit,
    onAddAccount: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    strings: Strings,
) {
    val colors = AppColors
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val interactionSource = remember { MutableInteractionSource() }
    val tileShape = RoundedCornerShape(11.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { menuExpanded = true },
                ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(tileShape)
                        .background(colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (selectedAccount?.displayName?.firstOrNull()?.uppercaseChar() ?: '?').toString(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = colors.onPrimary,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
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
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = colors.card,
                shadowElevation = 0.dp,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.border(width = 1.dp, color = colors.stroke, shape = RoundedCornerShape(14.dp)),
            ) {
                DropdownMenuItem(
                    text = { Text(strings.inboxAllAccounts, color = colors.textPrimary) },
                    leadingIcon = { Icon(Icons.Filled.MailOutline, contentDescription = null, tint = colors.textPrimary) },
                    onClick = {
                        menuExpanded = false
                        onSelectAccount(null)
                    },
                )
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(account.displayName, color = colors.textPrimary) },
                        onClick = {
                            menuExpanded = false
                            onSelectAccount(account.id)
                        },
                    )
                }
                HorizontalDivider(color = colors.stroke)
                DropdownMenuItem(
                    text = { Text(strings.inboxAddAccount, color = colors.primary) },
                    onClick = {
                        menuExpanded = false
                        onAddAccount()
                    },
                )
            }
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
                contentDescription = "Search",
                tint = colors.textPrimary,
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        // Reference shows a 34x34 tinted tile with a 3-line "sliders" icon here — wired
        // to Settings since there's no bottom-nav tab for it in this no-tab-bar IA.
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(tileShape)
                .background(colors.cardSubtle)
                .clickable(onClick = onSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Big "N unread" headline + subcopy directly below the top bar — matches the
 * reference's "24 unread" / "Sorted automatically by your rules" block. */
@Composable
private fun UnreadCountHeader(count: Int, strings: Strings) {
    val colors = AppColors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
        Text(
            text = if (count == 0) strings.inboxNoUnread else strings.inboxUnreadCount(count),
            style = MaterialTheme.typography.headlineSmall.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = strings.inboxUnreadSubtext,
            style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary),
        )
    }
}

@Composable
private fun InboxEmptyState(strings: Strings) {
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
            text = strings.inboxNoMessages,
            style = MaterialTheme.typography.titleMedium.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = strings.inboxNoMessagesSubtext,
            style = MaterialTheme.typography.bodySmall.copy(color = colors.textMuted),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
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

private fun dayLabel(epochMillis: Long, strings: Strings): String {
    val zone = ZoneId.systemDefault()
    val then = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    val today = Instant.now().atZone(zone).toLocalDate()
    return when (then) {
        today -> strings.inboxToday
        today.minusDays(1) -> strings.inboxYesterday
        else -> then.toString()
    }
}
