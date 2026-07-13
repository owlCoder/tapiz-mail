package rs.tapizlabs.mail.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.i18n.Strings
import rs.tapizlabs.mail.ui.theme.AppColors
import rs.tapizlabs.mail.ui.theme.MailSkin
import rs.tapizlabs.mail.ui.theme.swatchFor

/**
 * Skin picker — a 2-column grid of labeled [SkinTile]s, one per [MailSkin], each previewing
 * the skin's surface + accent/signal in the current light/dark mode. Ported 1:1 from
 * tapiz-lms/apps/android's `SkinPickerCard` (the ecosystem's shared pattern), wrapped in a
 * [MailCard] instead of `GlassCard` to match this app's existing component names.
 */
@Composable
fun SkinPickerCard(selected: MailSkin, onSelect: (MailSkin) -> Unit, strings: Strings, modifier: Modifier = Modifier) {
    MailCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MailSkin.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { skin ->
                        SkinTile(
                            skin = skin,
                            selected = skin == selected,
                            onSelect = { onSelect(skin) },
                            strings = strings,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** One labeled skin row — a surface square holding two vertical accent/signal pills, the
 * skin name, and a check when active. Ported 1:1 from tapiz-lms/apps/android's `SkinTile`. */
@Composable
private fun SkinTile(skin: MailSkin, selected: Boolean, onSelect: () -> Unit, strings: Strings, modifier: Modifier = Modifier) {
    val colors = AppColors
    val swatch = swatchFor(skin, colors.isDark)
    val ring by animateColorAsState(if (selected) colors.primary else colors.stroke, label = "skinRing")
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(if (selected) 2.dp else 1.dp, ring, RoundedCornerShape(12.dp))
            .background(if (selected) colors.accentSoft else colors.cardSubtle)
            .clickable(onClick = onSelect)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .border(1.dp, colors.stroke, RoundedCornerShape(9.dp))
                .background(swatch.surface),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(Modifier.size(width = 6.dp, height = 12.dp).clip(RoundedCornerShape(50)).background(swatch.accent))
                Box(Modifier.size(width = 6.dp, height = 12.dp).clip(RoundedCornerShape(50)).background(swatch.signal))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            skinLabel(skin, strings),
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) colors.primary else colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
        }
    }
}

private fun skinLabel(skin: MailSkin, strings: Strings): String = when (skin) {
    MailSkin.Default -> strings.skinDefault
    MailSkin.Ocean -> strings.skinOcean
    MailSkin.Forest -> strings.skinForest
    MailSkin.Rose -> strings.skinRose
    MailSkin.Graphite -> strings.skinGraphite
    MailSkin.Sand -> strings.skinSand
    MailSkin.Crimson -> strings.skinCrimson
    MailSkin.Aurora -> strings.skinAurora
}
