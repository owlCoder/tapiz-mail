package rs.tapizlabs.mail.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import rs.tapizlabs.mail.ui.theme.AppColors

/**
 * Shared shimmer phase — every skeleton on screen reads from ONE infinite transition
 * instead of spinning up its own, so a whole loading list stays in sync rather than
 * showing unsynced shimmer bars. Ported from the same recipe as tapiz-lms's
 * `ui/components/Skeleton.kt`.
 */
private val LocalShimmerPhase = compositionLocalOf<Float?> { null }

@Composable
private fun rememberSharedShimmerPhase(): Float {
    val provided = LocalShimmerPhase.current
    if (provided != null) return provided
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Restart),
        label = "shimmerX",
    )
    return x
}

/** Drives one shimmer animation for everything inside [content]. */
@Composable
fun SkeletonHost(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Restart),
        label = "shimmerX",
    )
    CompositionLocalProvider(LocalShimmerPhase provides x) { content() }
}

/** Animated shimmer fill — the basis for skeleton placeholders shown while mail/data
 * loads, instead of a bare spinner over an empty list. */
@Composable
fun Modifier.shimmer(shape: Shape = RoundedCornerShape(8.dp)): Modifier {
    val c = AppColors
    // Two adjacent surface shades, already theme/skin-reactive via AppColors —
    // closest match to the previous hardcoded Ink & Ember shimmer tones.
    val base = c.cardSubtle
    val highlight = c.stroke

    val x = rememberSharedShimmerPhase()
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(x * 200f, 0f),
        end = Offset((x + 1f) * 200f, 0f),
    )
    return this.clip(shape).background(brush)
}

/** A single skeleton bar. */
@Composable
fun SkeletonBar(modifier: Modifier = Modifier, height: Int = 14) {
    Box(modifier.height(height.dp).shimmer())
}

/** Skeleton stand-in for one inbox row (avatar + subject/snippet lines) — mirrors
 * [MessageListItem]'s layout so the loading state doesn't jump when real rows arrive. */
@Composable
fun SkeletonMessageRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(44.dp).shimmer(CircleShape))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBar(Modifier.fillMaxWidth(0.55f))
            SkeletonBar(Modifier.fillMaxWidth(0.85f), height = 12)
            SkeletonBar(Modifier.fillMaxWidth(0.4f), height = 11)
        }
    }
}

/** A column of [count] skeleton rows, all sharing one shimmer clock — used while the
 * Inbox/Search list has no cached rows to show yet. */
@Composable
fun SkeletonMessageList(count: Int = 6, modifier: Modifier = Modifier) {
    SkeletonHost {
        Column(modifier.fillMaxWidth()) {
            repeat(count) { SkeletonMessageRow() }
        }
    }
}
