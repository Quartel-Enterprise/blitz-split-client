package com.quare.blitzsplit.android.feature.groupstats

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.hypot

private const val FULL_TURN = 360f
private const val START_ANGLE = -90f
private const val SLICE_GAP_DEGREES = 2f
private const val PERCENT = 100f
private const val RING_FRACTION = 0.16f
internal const val RING_DIMMED_ALPHA = 0.4f
private const val SWEEP_MILLIS = 600
private const val HALF = 2f
private const val HOLE_FRACTION = 0.4f
private val popOut = 6.dp
private val centerGap = 10.dp

// Why: one slice of a ring: its rounded share, all of a ring's adding up to 100, and its data tone.
internal data class StatsRingSlice(
    val percent: Int,
    val color: Color,
)

/*
 * Why: the ring of "Por categoria" and "Por pessoa": slices with a thin gap between them, sweeping in when sweepKey
 * changes. The picked slice pops out and the others fade; a mouse picks by hovering, and a tap or a click lands on
 * onTap. Shares that are all 0 leave a bare neutral ring. The center is the caller's, the description TalkBack's.
 */
@Composable
internal fun StatsRingDisplay(
    slices: List<StatsRingSlice>,
    selected: Int?,
    sweepKey: Any,
    description: String,
    size: Dp,
    onPick: (index: Int?) -> Unit,
    onTap: (index: Int, isMouse: Boolean) -> Unit,
    center: @Composable () -> Unit,
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    // Why: kept as states and read while drawing, so a slice popping out redraws the ring without recomposing it.
    val pops = slices.indices.map { index ->
        animateFloatAsState(if (selected == index) 1f else 0f, label = "slice")
    }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(sweepKey) {
        sweep.snapTo(0f)
        sweep.animateTo(1f, tween(SWEEP_MILLIS))
    }
    val percents = slices.map { it.percent }
    // Why: the gestures keep running across recompositions, so they call the latest callbacks, not the first ones.
    val currentOnPick by rememberUpdatedState(onPick)
    val currentOnTap by rememberUpdatedState(onTap)
    Box(
        modifier = Modifier
            .size(size)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(percents) {
                    detectPickTaps { offset, isMouse ->
                        // Why: this.size is the pointer area's, in pixels; a bare size is the ring's Dp parameter.
                        findSliceAt(offset, this.size.width.toFloat(), percents)?.let { currentOnTap(it, isMouse) }
                    }
                }.onMouseHover(
                    key = percents,
                    onMove = { position -> currentOnPick(findSliceAt(position, this.size.width.toFloat(), percents)) },
                    onExit = { currentOnPick(null) },
                ),
        ) {
            val margin = popOut.toPx()
            val stroke = findRingStroke(this.size.minDimension, margin)
            val ringInset = margin + stroke / HALF
            if (percents.none { it > 0 }) {
                drawArc(
                    color = track,
                    startAngle = START_ANGLE,
                    sweepAngle = FULL_TURN,
                    useCenter = false,
                    topLeft = Offset(ringInset, ringInset),
                    size = Size(this.size.width - ringInset * HALF, this.size.height - ringInset * HALF),
                    style = Stroke(width = stroke),
                )
                return@Canvas
            }
            var angle = START_ANGLE
            val gap = if (percents.count { it > 0 } > 1) SLICE_GAP_DEGREES else 0f
            slices.forEachIndexed { index, slice ->
                val sweepAngle = slice.percent / PERCENT * FULL_TURN * sweep.value
                if (sweepAngle > gap) {
                    // Why: the picked slice grows outward only, its inner edge staying on the ring.
                    val extra = margin * pops[index].value
                    val inset = ringInset - extra / HALF
                    drawArc(
                        color = if (selected == null || selected == index) {
                            slice.color
                        } else {
                            slice.color.copy(alpha = RING_DIMMED_ALPHA)
                        },
                        startAngle = angle,
                        sweepAngle = sweepAngle - gap,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(this.size.width - inset * HALF, this.size.height - inset * HALF),
                        style = Stroke(width = stroke + extra),
                    )
                }
                angle += sweepAngle
            }
        }
        // Why: kept inside the hole with centerGap on each side, so a long name ends in an ellipsis before it touches the ring.
        val hole = size - (popOut + findRingStroke(size.value, popOut.value).dp) * HALF
        Box(
            modifier = Modifier.width((hole - centerGap * HALF).coerceAtLeast(0.dp)),
            contentAlignment = Alignment.Center,
        ) {
            center()
        }
    }
}

// Why: the ring's thickness for its side and the pop-out margin around it, in pixels to draw it and in dp for its hole.
private fun findRingStroke(
    side: Float,
    margin: Float,
): Float = (side - margin * HALF) * RING_FRACTION

/*
 * Why: the slice under a point of the ring, by its angle from the top clockwise; null deep in the hole (a touch just
 * inside the ring still picks its slice), outside it, or on a ring with nothing in it.
 */
private fun findSliceAt(
    point: Offset,
    side: Float,
    percents: List<Int>,
): Int? {
    if (percents.none { it > 0 }) return null
    val radius = side / HALF
    val dx = point.x - radius
    val dy = point.y - radius
    val distance = hypot(dx, dy)
    if (distance > radius || distance < radius * HOLE_FRACTION) return null
    val degrees = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
    val fromTop = (degrees - START_ANGLE + FULL_TURN) % FULL_TURN
    var end = 0f
    percents.forEachIndexed { index, percent ->
        end += percent / PERCENT * FULL_TURN
        if (percent > 0 && fromTop < end) return index
    }
    return percents.indexOfLast { it > 0 }
}

// Why: the ring's middle, its share over its name, fading from one pick to the next.
@Composable
internal fun StatsRingLabel(
    key: Any,
    percent: Int,
    label: String,
) {
    Crossfade(
        targetState = key to (percent to label),
        label = "center",
    ) { (_, shown) ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.group_stats_category_percent, shown.first),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                softWrap = false,
                // Why: only the name is held to the hole; the share stays whole at the largest text sizes.
                modifier = Modifier.wrapContentWidth(unbounded = true),
            )
            Text(
                text = shown.second,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
