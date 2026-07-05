package rs.tapizlabs.mail.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.theme.AppColors

data class SegmentedOption(val icon: ImageVector, val label: String)

/**
 * Segmented picker with a sliding highlight — options render as icon + label on a single
 * row inside a [MailCard], and the accent pill slides between them instead of snapping.
 * Used where a [MailDropdownField] would be overkill for a small, fixed option set (e.g.
 * theme: System/Light/Dark). Mirrors `tapiz-boards` Android's `SegmentedPickerCard`.
 */
@Composable
fun SegmentedPickerCard(
    options: List<SegmentedOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    val colors = AppColors
    val rowHeight = 40.dp

    MailCard(modifier = modifier.fillMaxWidth()) {
        if (header != null) {
            Box(modifier = Modifier.padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 4.dp)) {
                header()
            }
        }
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .height(rowHeight),
        ) {
            val gap = 8.dp
            val optionWidth = (maxWidth - gap * (options.size - 1)) / options.size
            val pillOffset by animateDpAsState(
                targetValue = (optionWidth + gap) * selectedIndex,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
                label = "segmented_pill",
            )

            Box(
                Modifier
                    .offset(x = pillOffset)
                    .width(optionWidth)
                    .height(rowHeight)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.accentSoft),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                options.forEachIndexed { index, option ->
                    val selected = index == selectedIndex
                    val interactionSource = remember { MutableInteractionSource() }
                    val tint by animateColorAsState(
                        targetValue = if (selected) colors.primary else colors.textMuted,
                        animationSpec = tween(160),
                        label = "segmented_tint",
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(rowHeight)
                            .clip(RoundedCornerShape(11.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onSelect(index) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(option.icon, contentDescription = option.label, tint = tint, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = tint,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
