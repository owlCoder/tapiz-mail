package rs.tapizlabs.mail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.model.MessageListItemUi
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Wraps [MessageListItem] with a Material3 `SwipeToDismissBox`, exposing directional swipe
 * callbacks. The actual action fired per direction (archive/delete/mark-read) is
 * user-configurable in Settings (owned by another agent) — this component only reports
 * "left" or "right" was swiped and resets itself; it never hardcodes what a direction means.
 *
 * The background icon shown while dragging is a generic archive glyph on both sides since
 * this component doesn't know the configured action; screens that need a per-direction icon
 * can be revisited once the swipe-action-to-icon mapping lands in Settings.
 */
@Composable
fun SwipeableMessageRow(
    message: MessageListItemUi,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppColors

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> onSwipeLeft()
                SwipeToDismissBoxValue.StartToEnd -> onSwipeRight()
                SwipeToDismissBoxValue.Settled -> {}
            }
            // Never let the box stay dismissed — the row always snaps back; the
            // configured action (archive/delete/mark-read) is applied by the caller,
            // not by removing the row from a swipe gesture alone.
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.Settled -> Alignment.Center
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.accentSoft)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Archive,
                    contentDescription = null,
                    tint = colors.primary,
                )
            }
        },
    ) {
        MessageListItem(
            message = message,
            onClick = onClick,
            onToggleStar = onToggleStar,
            modifier = Modifier.background(colors.card),
        )
    }
}
