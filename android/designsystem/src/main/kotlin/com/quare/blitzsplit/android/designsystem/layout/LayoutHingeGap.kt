package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * Why: the hinge is reported on screen, and a layout can sit anywhere in the window (after the rail, inside a
 * dialog), so it reads where it was placed and turns the band into its own coordinates, in one place for every layout
 * that splits on it (HalvesLayout, FoldDialog's half). Until it is first placed it counts from the screen's corner,
 * where a full screen layout starts anyway; reading the origin in a measure block only measures again, never
 * recomposes.
 */
@Stable
internal class LayoutHingeGap {
    private var originOnScreen by mutableStateOf(Offset.Zero)

    // Why: on the layout that splits, so the origin is the one its measure block works in.
    val modifier: Modifier = Modifier.onPlaced { coordinates -> originOnScreen = coordinates.positionOnScreen() }

    // Why: where the first half ends and the second starts along the layout, x side by side and y top and bottom.
    fun gapOf(
        density: Density,
        split: HingeSplit?,
        isSideBySide: Boolean,
        length: Int,
        gap: Dp = HingeBand,
    ): IntRange = with(density) {
        val origin = if (isSideBySide) originOnScreen.x else originOnScreen.y
        halvesGapOf(
            length = length,
            band = split?.let { it.bandStart.toPx() - origin..it.bandEnd.toPx() - origin },
            gap = gap.roundToPx(),
        )
    }
}

@Composable
internal fun rememberLayoutHingeGap(): LayoutHingeGap = remember { LayoutHingeGap() }

/*
 * Why: where the first half ends (`first`) and the second starts (`last`) along a layout of `length` px. The gap is
 * centered on the band and covers it whole, so a hinge wider than the gap still stays out of both halves; a band off
 * the layout (no hinge, a layout beside it) leaves two equal halves around the gap.
 */
private fun halvesGapOf(
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
