package rs.tapizlabs.mail.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.model.CategoryChipUi
import rs.tapizlabs.mail.ui.theme.AppColors
import kotlin.math.abs

private const val CHIP_ANIM_MS = 160

/**
 * Horizontal scrollable row of category chips (Primary + custom categories), each showing
 * name + count. Selected chip uses `accentSoft` background per the theme's convention.
 */
@Composable
fun CategoryChipsRow(
    categories: List<CategoryChipUi>,
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            CategoryChip(
                category = category,
                selected = category.id == selectedCategoryId,
                onClick = { onSelectCategory(category.id) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: CategoryChipUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = AppColors
    val tint = colors.categoryTints[abs(category.colorIndex) % colors.categoryTints.size]
    val shape = RoundedCornerShape(999.dp)
    val interactionSource = remember { MutableInteractionSource() }

    val backgroundColor by animateColorAsState(
        targetValue = if (selected) colors.accentSoft else colors.cardSubtle,
        animationSpec = tween(CHIP_ANIM_MS),
        label = "chip_background",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) tint else colors.stroke.copy(alpha = 0.6f),
        animationSpec = tween(CHIP_ANIM_MS),
        label = "chip_border",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) tint else colors.textSecondary,
        animationSpec = tween(CHIP_ANIM_MS),
        label = "chip_label",
    )
    val countColor by animateColorAsState(
        targetValue = if (selected) tint else colors.textMuted,
        animationSpec = tween(CHIP_ANIM_MS),
        label = "chip_count",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 1.5.dp else 1.dp,
        animationSpec = tween(CHIP_ANIM_MS),
        label = "chip_border_width",
    )

    Row(
        modifier = Modifier
            .clip(shape)
            .background(backgroundColor)
            .border(width = borderWidth, color = borderColor, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelMedium.copy(
                color = labelColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
        )
        Text(
            text = "${category.count}",
            style = MaterialTheme.typography.labelMedium.copy(
                color = countColor,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
