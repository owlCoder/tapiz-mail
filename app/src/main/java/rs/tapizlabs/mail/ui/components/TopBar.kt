package rs.tapizlabs.mail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Top-bar back button — a round chip (cardSubtle fill, hairline border, soft shadow), same
 * structural recipe as `BackArrowButton` in tapiz-lms/apps/android and tapiz-boards/android, so
 * the arrow reads as a button rather than a bare unstyled `IconButton`. Only the touch/press
 * feedback stays Mail's own default ripple (Mail's own button family uses a flat/signal-edge
 * press style, not the scale/lift used elsewhere — this chip only borrows the *shape*, not the
 * interaction, keeping Mail's deliberate press-style difference intact).
 */
@Composable
fun BackArrowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppColors.textPrimary,
) {
    val colors = AppColors
    Box(
        modifier = modifier
            .size(42.dp)
            .shadow(elevation = 2.dp, shape = CircleShape, spotColor = colors.shadow, ambientColor = colors.shadow)
            .clip(CircleShape)
            .background(colors.cardSubtle)
            .border(1.dp, colors.stroke, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = tint,
        )
    }
}
