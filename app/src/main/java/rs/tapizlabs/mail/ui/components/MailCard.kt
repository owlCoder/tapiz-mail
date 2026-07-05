package rs.tapizlabs.mail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.theme.AppColors

/** Shared card surface (16dp radius, 1dp stroke) — optionally clickable as a whole row. */
@Composable
fun MailCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = AppColors
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.card)
            .border(width = 1.dp, color = colors.stroke, shape = shape)
            .let {
                if (onClick != null) {
                    it.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                } else {
                    it
                }
            },
    ) {
        content()
    }
}
