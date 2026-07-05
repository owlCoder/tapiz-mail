package rs.tapizlabs.mail.ui.search

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import rs.tapizlabs.mail.ui.components.MessageListItem
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.i18n.Strings
import rs.tapizlabs.mail.ui.model.AccountSummaryUi
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Search — rendered as a full-screen overlay directly over [rs.tapizlabs.mail.ui.inbox.InboxScreen]
 * (like Gmail's search: the inbox stays mounted underneath, no NavHost back-stack entry) rather
 * than a pushed destination — see [rs.tapizlabs.mail.ui.inbox.InboxScreen]'s `showSearch` state,
 * which is the sole caller. Debounced query against the local Room search
 * (`MessageDao.searchMessages`), plus simple filter chips (account, has-attachment).
 */
@Composable
fun SearchScreen(
    onOpenMessage: (messageId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = AppColors
    val strings = LocalStrings.current

    Column(modifier = modifier.fillMaxSize().background(colors.canvasTop)) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = colors.textPrimary)
            }
            SearchField(
                query = uiState.query,
                onQueryChange = viewModel::updateQuery,
                placeholder = strings.searchPlaceholder,
                modifier = Modifier.weight(1f),
            )
        }

        FilterChipsRow(
            accounts = uiState.accounts,
            selectedAccountId = uiState.filters.accountId,
            hasAttachmentOnly = uiState.filters.hasAttachmentOnly,
            onSelectAccount = viewModel::updateAccountFilter,
            onToggleHasAttachment = viewModel::toggleHasAttachmentFilter,
            strings = strings,
        )

        HorizontalDivider(color = colors.stroke.copy(alpha = 0.5f))

        // Crossfade keyed on which of the three states is showing (hint/empty/results) so
        // typing a query or changing filters fades the content instead of an abrupt cut —
        // matches the same pattern used for the Inbox's category chip switches.
        val searchStateKey = when {
            uiState.query.isBlank() -> "hint"
            uiState.results.isEmpty() -> "empty"
            else -> "results"
        }
        Crossfade(
            targetState = searchStateKey,
            animationSpec = tween(180),
            label = "search_state_crossfade",
        ) {
            when (it) {
                "hint" -> SearchHint(strings.searchHint)
                "empty" -> SearchEmptyResults(strings.searchNoResults)
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            text = strings.searchResultsCount(uiState.results.size),
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textMuted, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                    items(uiState.results, key = { message -> message.id }) { message ->
                        MessageListItem(
                            message = message,
                            onClick = { onOpenMessage(message.id) },
                            onToggleStar = { viewModel.toggleStar(message.id, message.isStarred) },
                        )
                        HorizontalDivider(color = colors.stroke.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

/** Flat "Signal" pill search field — `inputBackground` fill, no Material outline, matches
 * [MailTextField]'s recipe but pill-shaped (999.dp) and label-less since this is a standalone
 * search bar, not a form field. Replaces the earlier `OutlinedTextField`, which read as a
 * generic Material field inconsistent with the rest of the app's flat surfaces. */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = AppColors
    val shape = RoundedCornerShape(999.dp)
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) colors.primary else colors.stroke

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(colors.inputBackground)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = if (isFocused) colors.primary else colors.textMuted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium.copy(color = colors.textMuted),
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusEvent { isFocused = it.isFocused },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primary),
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Outlined.Close, contentDescription = "Clear", tint = colors.textMuted)
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    accounts: List<AccountSummaryUi>,
    selectedAccountId: String?,
    hasAttachmentOnly: Boolean,
    onSelectAccount: (String?) -> Unit,
    onToggleHasAttachment: () -> Unit,
    strings: Strings,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            label = strings.searchAllAccounts,
            selected = selectedAccountId == null,
            onClick = { onSelectAccount(null) },
        )
        accounts.forEach { account ->
            FilterChip(
                label = account.displayName,
                selected = selectedAccountId == account.id,
                onClick = { onSelectAccount(account.id) },
            )
        }
        FilterChip(
            label = strings.searchHasAttachment,
            selected = hasAttachmentOnly,
            onClick = onToggleHasAttachment,
            icon = Icons.Outlined.AttachFile,
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val colors = AppColors
    val shape = RoundedCornerShape(999.dp)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .clip(shape)
            .background(if (selected) colors.accentSoft else colors.cardSubtle)
            .border(
                width = 1.dp,
                color = if (selected) colors.primary else colors.stroke.copy(alpha = 0.6f),
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.primary else colors.textMuted,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (selected) colors.primary else colors.textSecondary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun SearchHint(text: String) {
    SearchStatePlaceholder(icon = Icons.Outlined.Search, text = text)
}

@Composable
private fun SearchEmptyResults(text: String) {
    SearchStatePlaceholder(icon = Icons.Outlined.SearchOff, text = text)
}

/** Icon-in-tinted-square + message — matches [rs.tapizlabs.mail.ui.inbox.InboxScreen]'s
 * empty-state recipe so Search's placeholder states aren't a bare line of muted text while
 * every other empty state in the app has this same visual treatment. */
@Composable
private fun SearchStatePlaceholder(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    val colors = AppColors
    Column(
        modifier = Modifier
            .fillMaxSize()
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
            Icon(imageVector = icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textMuted),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
