package rs.tapizlabs.mail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.data.local.entity.SwipeAction
import rs.tapizlabs.mail.ui.model.MessageListItemUi
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Wraps [MessageListItem] with a Material3 `SwipeToDismissBox`, exposing directional swipe
 * callbacks. The actual action fired per direction (delete/mark-read) is user-configurable in
 * Settings — this component only reports "left" or "right" was swiped and resets itself; it
 * never hardcodes what a direction means.
 *
 * [leftAction]/[rightAction] drive the background icon/color shown while dragging so the
 * drag preview always matches what will actually happen (e.g. a mark-read swipe shows a
 * neutral read-mail glyph, not the same destructive red delete icon on both sides) — without
 * this, MARK_READ/MARK_UNREAD swipes looked identical to DELETE and users couldn't tell the
 * configured action had actually fired for a non-destructive action.
 */
@Composable
fun SwipeableMessageRow(
    message: MessageListItemUi,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    leftAction: SwipeAction,
    rightAction: SwipeAction,
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
            // configured action (delete/mark-read) is applied by the caller, not by
            // removing the row from a swipe gesture alone.
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
            val activeAction = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> rightAction
                SwipeToDismissBoxValue.EndToStart -> leftAction
                SwipeToDismissBoxValue.Settled -> null
            }
            val tint = swipeActionTint(activeAction, colors)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tint.copy(alpha = 0.16f))
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment,
            ) {
                swipeActionIcon(activeAction)?.let { icon ->
                    Icon(imageVector = icon, contentDescription = null, tint = tint)
                }
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

private fun swipeActionIcon(action: SwipeAction?): ImageVector? = when (action) {
    SwipeAction.DELETE -> Icons.Outlined.Delete
    SwipeAction.MARK_READ -> Icons.Outlined.MarkEmailRead
    SwipeAction.MARK_UNREAD -> Icons.Outlined.MarkEmailUnread
    SwipeAction.NONE, null -> null
}

private fun swipeActionTint(action: SwipeAction?, colors: rs.tapizlabs.mail.ui.theme.TapizColors): Color = when (action) {
    SwipeAction.DELETE -> colors.coral
    SwipeAction.MARK_READ, SwipeAction.MARK_UNREAD -> colors.primary
    SwipeAction.NONE, null -> colors.textMuted
}
