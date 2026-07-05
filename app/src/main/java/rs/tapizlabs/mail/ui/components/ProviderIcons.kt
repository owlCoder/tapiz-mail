package rs.tapizlabs.mail.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Provider brand colors (fixed, not theme tokens — same convention as the Gmail
 * red / Outlook blue / LinkedIn blue referenced in the design handoff).
 */
object ProviderBrandColors {
    val Gmail = Color(0xFFEA4335)
    val Outlook = Color(0xFF0364B8)
}

/** Provider badge size used across Add Account / Choose Provider rows (matches
 * the reference's 22x22 provider-color chip). */
val ProviderIconSize = 22.dp

/**
 * Letter badge for a mail provider — a rounded-square swatch in the provider's brand
 * color with a single bold initial, same convention the reference itself uses for
 * Google ("G"), LinkedIn ("in") and Apple's silhouette on the Inbox mock. Avoids
 * reproducing the actual trademarked Gmail/Outlook logos.
 */
@Composable
fun ProviderBadge(letter: String, color: Color, modifier: Modifier = Modifier, size: Dp = ProviderIconSize) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3.6f))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.55f).sp,
        )
    }
}
