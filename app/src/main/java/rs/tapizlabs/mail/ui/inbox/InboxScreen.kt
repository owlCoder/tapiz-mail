package rs.tapizlabs.mail.ui.inbox

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.outlined.ExpandMore
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import rs.tapizlabs.mail.data.local.entity.SwipeAction
import rs.tapizlabs.mail.ui.components.CategoryChipsRow
import rs.tapizlabs.mail.ui.components.SkeletonMessageList
import rs.tapizlabs.mail.ui.components.SwipeableMessageRow
import rs.tapizlabs.mail.ui.model.AccountSummaryUi
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Inbox — the app's home tab. Top bar is deliberately minimal (account switcher only): search
 * and settings are their own bottom-nav destinations per the IA decision, so they must not be
 * duplicated here.
 *
 * @param onOpenMessage navigate to Mail Detail for the given message id.
 * @param onAddAccount navigate to Add-Account (owned by another agent) — invoked from the
 * account switcher's "Add account" entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onOpenMessage: (messageId: String) -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        InboxTopBar(
            accounts = uiState.accounts,
            selectedAccountId = uiState.selectedAccountId,
            onSelectAccount = viewModel::selectAccount,
            onAddAccount = onAddAccount,
        )

        if (uiState.categories.isNotEmpty()) {
            CategoryChipsRow(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onSelectCategory = viewModel::selectCategory,
            )
            HorizontalDivider(color = AppColors.stroke.copy(alpha = 0.5f))
        }

        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            state = pullState,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                uiState.isLoading -> InboxLoadingState()
                uiState.messages.isEmpty() -> InboxEmptyState()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        SwipeableMessageRow(
                            message = message,
                            onClick = { onOpenMessage(message.id) },
                            onToggleStar = { viewModel.toggleStar(message.id, message.isStarred) },
                            onSwipeLeft = {
                                viewModel.applySwipeAction(message.id, SwipeAction.ARCHIVE)
                            },
                            onSwipeRight = {
                                viewModel.applySwipeAction(message.id, SwipeAction.MARK_READ)
                            },
                        )
                        HorizontalDivider(color = AppColors.stroke.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxTopBar(
    accounts: List<AccountSummaryUi>,
    selectedAccountId: String?,
    onSelectAccount: (String?) -> Unit,
    onAddAccount: () -> Unit,
) {
    val colors = AppColors
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.accentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (selectedAccount?.displayName?.firstOrNull()?.uppercaseChar() ?: '?').toString(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = selectedAccount?.displayName ?: "All accounts",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (selectedAccount != null) {
                        Text(
                            text = selectedAccount.emailAddress,
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = "Switch account",
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp),
                )
            }

            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("All accounts") },
                    leadingIcon = { Icon(Icons.Filled.MailOutline, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onSelectAccount(null)
                    },
                )
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(account.displayName) },
                        onClick = {
                            menuExpanded = false
                            onSelectAccount(account.id)
                        },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Add account") },
                    onClick = {
                        menuExpanded = false
                        onAddAccount()
                    },
                )
            }
        }
    }
}

@Composable
private fun InboxEmptyState() {
    val colors = AppColors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.MailOutline,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No messages",
            style = MaterialTheme.typography.titleMedium.copy(color = colors.textPrimary),
        )
        Text(
            text = "New mail will show up here once an account is synced.",
            style = MaterialTheme.typography.bodySmall.copy(color = colors.textMuted),
        )
    }
}

@Composable
private fun InboxLoadingState() {
    SkeletonMessageList(modifier = Modifier.fillMaxSize().padding(top = 4.dp))
}

