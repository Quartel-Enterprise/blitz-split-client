package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * Why: the hinge is reported on screen, and a layout can sit anywhere in the window (after the rail, inside a
 * dialog), so it reads where it was placed and splits on the band relative to itself. Until it is first placed it
 * counts from the screen's corner, where a full screen layout starts anyway; reading the origin in the measure block
 * only measures again, never recomposes.
 */
@Composable
internal fun HalvesLayout(
    split: HingeSplit?,
    isSideBySide: Boolean,
    gap: Dp,
    first: @Composable BoxScope.() -> Unit,
    second: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    var originOnScreen by remember { mutableStateOf(Offset.Zero) }
    Layout(
        contents = listOf({ Box(content = first) }, { Box(content = second) }),
        modifier = modifier.onPlaced { coordinates -> originOnScreen = coordinates.positionOnScreen() },
    ) { (firstMeasurables, secondMeasurables), constraints ->
        val length = if (isSideBySide) constraints.maxWidth else constraints.maxHeight
        check(length != Constraints.Infinity) { "Two halves need a bounded ${if (isSideBySide) "width" else "height"}" }
        val origin = if (isSideBySide) originOnScreen.x else originOnScreen.y
        val gapRange = halvesGapOf(
            length = length,
            band = split?.let { it.bandStart.toPx() - origin..it.bandEnd.toPx() - origin },
            gap = gap.roundToPx(),
        )
        if (isSideBySide) {
            val isLtr = layoutDirection == LayoutDirection.Ltr
            val leftWidth = gapRange.first
            val rightWidth = length - gapRange.last
            val startWidth = if (isLtr) leftWidth else rightWidth
            val endWidth = if (isLtr) rightWidth else leftWidth
            val start = firstMeasurables.single().measure(
                constraints.copy(
                    minWidth = startWidth,
                    maxWidth = startWidth,
                ),
            )
            val end = secondMeasurables.single().measure(
                constraints.copy(
                    minWidth = endWidth,
                    maxWidth = endWidth,
                ),
            )
            val height = max(start.height, end.height).coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(length, height) {
                start.place(if (isLtr) 0 else gapRange.last, 0)
                end.place(if (isLtr) gapRange.last else 0, 0)
            }
        } else {
            val topHeight = gapRange.first
            val bottomHeight = length - gapRange.last
            val top = firstMeasurables.single().measure(
                constraints.copy(
                    minHeight = topHeight,
                    maxHeight = topHeight,
                ),
            )
            val bottom = secondMeasurables.single().measure(
                constraints.copy(
                    minHeight = bottomHeight,
                    maxHeight = bottomHeight,
                ),
            )
            val width = max(top.width, bottom.width).coerceIn(constraints.minWidth, constraints.maxWidth)
            layout(width, length) {
                top.placeRelative(0, 0)
                bottom.placeRelative(0, gapRange.last)
            }
        }
    }
}

/*
 * Why: where the first half ends (`first`) and the second starts (`last`) along a layout of `length` px. The gap is
 * centered on the band and covers it whole, so a hinge wider than the gap still stays out of both halves; a band off
 * the layout (no hinge, a layout beside it) leaves two equal halves around the gap.
 */
internal fun halvesGapOf(
    length: Int,
    band: ClosedFloatingPointRange<Float>?,
    gap: Int,
): IntRange {
    if (band == null || band.endInclusive <= 0f || band.start >= length) return centeredGapOf(length, gap)
    val center = (band.start + band.endInclusive) / 2f
    val firstEnd = min(center - gap / 2f, band.start).roundToInt().coerceIn(0, length)
    val secondStart = max(center + gap / 2f, band.endInclusive).roundToInt().coerceIn(firstEnd, length)
    return firstEnd..secondStart
}

private fun centeredGapOf(
    length: Int,
    gap: Int,
): IntRange {
    val firstEnd = ((length - gap) / 2).coerceAtLeast(0)
    return firstEnd..(length - firstEnd).coerceAtLeast(firstEnd)
}
