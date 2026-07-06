package rs.tapizlabs.mail.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Shared bottom-sheet overlay primitive for Tapiz Mail — every sheet/overlay in this app
 * (attachment actions, filter pickers, account switcher, compose Cc/Bcc reveal handled
 * inline instead) should route through this rather than an ad-hoc `ModalBottomSheet`, so
 * behavior stays consistent per the design guideline:
 * - max ~80% viewport height, content scrolls inside once it exceeds that cap
 * - back gesture dismisses the topmost sheet, never the whole screen
 * - scrim tap = cancel (never an implicit confirm)
 *
 * Mirrors `tapiz-boards` Android's `GlassSheetOverlay` so the two apps' overlay behavior
 * reads as one family.
 */
@Composable
fun MailSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppColors
    val sheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    val interactionSource = remember { MutableInteractionSource() }

    BackHandler(enabled = visible, onBack = onDismiss)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxSheetHeight = maxHeight * 0.8f

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(120, easing = LinearOutSlowInEasing)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it } +
                fadeIn(tween(220, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(tween(120, easing = LinearOutSlowInEasing)) { it } +
                fadeOut(tween(120, easing = LinearOutSlowInEasing)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .clip(sheetShape)
                    .background(colors.card)
                    .border(width = 1.dp, color = colors.stroke, shape = sheetShape)
                    .imePadding()
                    .navigationBarsPadding(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.stroke),
                    )
                }

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 0.dp),
                ) {
                    Spacer(Modifier.height(16.dp))
                    content()
                    // Bottom breathing room below the sheet's last element — without this,
                    // content sits flush against navigationBarsPadding's edge (or the scrim
                    // on gesture-nav devices where that padding is thin), reading as clipped/
                    // cut off rather than intentionally ending there.
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
