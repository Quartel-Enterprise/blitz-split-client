package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.max

// Why: the two halves on the band, which LayoutHingeGap turns into this layout's own coordinates.
@Composable
internal fun HalvesLayout(
    split: HingeSplit?,
    isSideBySide: Boolean,
    gap: Dp,
    first: @Composable BoxScope.() -> Unit,
    second: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val hingeGap = rememberLayoutHingeGap()
    Layout(
        contents = listOf({ Box(content = first) }, { Box(content = second) }),
        modifier = modifier.then(hingeGap.modifier),
    ) { (firstMeasurables, secondMeasurables), constraints ->
        val length = if (isSideBySide) constraints.maxWidth else constraints.maxHeight
        check(length != Constraints.Infinity) { "Two halves need a bounded ${if (isSideBySide) "width" else "height"}" }
        val gapRange = hingeGap.gapOf(
            density = this,
            split = split,
            isSideBySide = isSideBySide,
            length = length,
            gap = gap,
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
