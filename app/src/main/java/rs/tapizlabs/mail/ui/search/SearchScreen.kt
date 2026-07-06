package rs.tapizlabs.mail.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import rs.tapizlabs.mail.ui.components.MessageListItem
import rs.tapizlabs.mail.ui.i18n.LocalStrings
import rs.tapizlabs.mail.ui.i18n.Strings
import rs.tapizlabs.mail.ui.model.AccountSummaryUi
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Search — rendered as a *floating* overlay above [rs.tapizlabs.mail.ui.inbox.InboxScreen]:
 * a dimmed scrim over the still-mounted inbox plus a rounded, inset panel that drops in from
 * the top and holds the search field, filter chips, and results. Deliberately NOT the old
 * full-bleed Gmail-style pane (which read as a separate screen) — this reads as an overlay
 * that belongs to the app, matching [rs.tapizlabs.mail.ui.components.MailSheet]'s scrim +
 * card family (just anchored top instead of bottom).
 *
 * Owns its own `visible`/`onDismiss` animation now, so the caller just renders it
 * unconditionally and flips [visible]; the inbox stays mounted underneath (no NavHost
 * back-stack entry). Debounced query against the local Room search
 * (`MessageDao.searchMessages`), plus simple filter chips (account, has-attachment).
 */
@Composable
fun SearchScreen(
    visible: Boolean,
    onOpenMessage: (messageId: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = AppColors
    val strings = LocalStrings.current
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Hides the keyboard before notifying the caller — otherwise the IME closing collapses
    // this screen's imePadding() at its own speed, fighting the panel's slide-out exit
    // animation and making the close transition look skipped/snapped shut instead of smooth.
    val dismiss = {
        keyboard?.hide()
        onDismiss()
    }

    BackHandler(enabled = visible, onBack = dismiss)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxPanelHeight = maxHeight * 0.88f

        // Scrim — dims the inbox behind and dismisses on tap. Same 0.32 alpha as MailSheet
        // so the two overlays feel like one system.
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(140, easing = LinearOutSlowInEasing)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = dismiss,
                    ),
            )
        }

        // Floating panel — same horizontal slide+fade + exact timings AND offset fraction
        // (it / 4, not a full-width slide) as the NavHost's enterTransition/exitTransition
        // (RootNavigation.kt push/Compose screen), so opening Search reads as the literal
        // same Compose-push transition applied to an overlay instead of a route.
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it / 4 } +
                fadeIn(tween(220, easing = FastOutSlowInEasing)),
            exit = slideOutHorizontally(tween(140, easing = LinearOutSlowInEasing)) { it / 4 } +
                fadeOut(tween(140, easing = LinearOutSlowInEasing)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        ) {
            // Full-bleed to the top + side edges (no top margin, x flush to edge); only the
            // bottom corners are rounded since the panel hangs from the very top of the screen.
            val panelShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxPanelHeight)
                    .clip(panelShape)
                    // Same canvasTop the Compose screen uses (containerColor), not `card` —
                    // `card` is white in light mode, which made this overlay read as a
                    // different, disconnected surface instead of the app's own background.
                    .background(colors.canvasTop)
                    .imePadding(),
            ) {
                // Header: back + search field, then filter chips — pinned; results scroll below.
                // statusBarsPadding lives here (not on the panel) so the card fill goes edge-to-edge
                // under the status bar while content stays clear of it.
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = dismiss) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Back",
                                tint = colors.textPrimary,
                            )
                        }
                        SearchField(
                            query = uiState.query,
                            onQueryChange = viewModel::updateQuery,
                            placeholder = strings.searchPlaceholder,
                            focusRequester = focusRequester,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    FilterChipsRow(
                        accounts = uiState.accounts,
                        selectedAccountId = uiState.filters.accountId,
                        hasAttachmentOnly = uiState.filters.hasAttachmentOnly,
                        onSelectAccount = viewModel::updateAccountFilter,
                        onToggleHasAttachment = viewModel::toggleHasAttachmentFilter,
                        strings = strings,
                    )
                }

                // Crossfade keyed on which of the three states is showing (hint/empty/results)
                // so typing a query or changing filters fades the content instead of cutting.
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
                        else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            item {
                                Text(
                                    text = strings.searchResultsCount(uiState.results.size),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = colors.textMuted,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                )
                            }
                            items(uiState.results, key = { message -> message.id }) { message ->
                                MessageListItem(
                                    message = message,
                                    onClick = { onOpenMessage(message.id) },
                                    onToggleStar = { viewModel.toggleStar(message.id, message.isStarred) },
                                    modifier = Modifier.animateItem(),
                                )
                                HorizontalDivider(color = colors.stroke.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Autofocus the field (and raise the keyboard) whenever the overlay opens, so search is
    // ready to type into immediately — an overlay that requires a second tap to focus feels
    // like a screen, not a quick action.
    androidx.compose.runtime.LaunchedEffect(visible) {
        if (visible) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
}

/** Flat "Signal" pill search field — `inputBackground` fill, no Material outline, matches
 * [rs.tapizlabs.mail.ui.components.MailTextField]'s recipe but pill-shaped (999.dp) and
 * label-less since this is a standalone search bar, not a form field. */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester,
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
                    .focusRequester(focusRequester)
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
            .padding(horizontal = 6.dp),
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

    val backgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) colors.accentSoft else colors.cardSubtle,
        animationSpec = tween(160),
        label = "filter_chip_bg",
    )
    val borderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) colors.primary else colors.stroke.copy(alpha = 0.6f),
        animationSpec = tween(160),
        label = "filter_chip_border",
    )
    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) colors.primary else colors.textSecondary,
        animationSpec = tween(160),
        label = "filter_chip_content",
    )

    Row(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
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
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = contentColor,
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
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 32.dp),
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
