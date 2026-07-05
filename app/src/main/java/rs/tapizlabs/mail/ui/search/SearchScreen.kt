package rs.tapizlabs.mail.ui.search

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import rs.tapizlabs.mail.ui.components.MessageListItem
import rs.tapizlabs.mail.ui.model.AccountSummaryUi
import rs.tapizlabs.mail.ui.theme.AppColors

/** Search — a push destination reached from Inbox's top-bar search icon (no bottom-nav
 * tab in this app's IA). Debounced query against the local Room search
 * (`MessageDao.searchMessages`), plus simple filter chips (account, has-attachment). */
@Composable
fun SearchScreen(
    onOpenMessage: (messageId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = AppColors

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
            }
        }

        SearchField(
            query = uiState.query,
            onQueryChange = viewModel::updateQuery,
        )

        FilterChipsRow(
            accounts = uiState.accounts,
            selectedAccountId = uiState.filters.accountId,
            hasAttachmentOnly = uiState.filters.hasAttachmentOnly,
            onSelectAccount = viewModel::updateAccountFilter,
            onToggleHasAttachment = viewModel::toggleHasAttachmentFilter,
        )

        HorizontalDivider(color = colors.stroke.copy(alpha = 0.5f))

        when {
            uiState.query.isBlank() -> SearchHint()
            uiState.results.isEmpty() -> SearchEmptyResults()
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.results, key = { it.id }) { message ->
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

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val colors = AppColors

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .heightIn(min = 52.dp),
        placeholder = { Text("Search mail") },
        singleLine = true,
        leadingIcon = {
            Icon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = colors.textMuted)
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Clear", tint = colors.textMuted)
                }
            }
        } else null,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.stroke,
            focusedContainerColor = colors.cardSubtle,
            unfocusedContainerColor = colors.cardSubtle,
            cursorColor = colors.primary,
        ),
    )
}

@Composable
private fun FilterChipsRow(
    accounts: List<AccountSummaryUi>,
    selectedAccountId: String?,
    hasAttachmentOnly: Boolean,
    onSelectAccount: (String?) -> Unit,
    onToggleHasAttachment: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            label = "All accounts",
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
            label = "Has attachment",
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
                modifier = Modifier.padding(0.dp),
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
private fun SearchHint() {
    val colors = AppColors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Search across all your synced mail",
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textMuted),
        )
    }
}

@Composable
private fun SearchEmptyResults() {
    val colors = AppColors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No results",
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.textMuted),
        )
    }
}
