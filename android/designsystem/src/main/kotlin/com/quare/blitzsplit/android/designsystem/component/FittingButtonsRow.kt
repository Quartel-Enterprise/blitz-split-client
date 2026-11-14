package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Why: buttons side by side at equal widths while each one's label fits on one line there, else stacked, each the
 * whole width: a narrow pane (a phone lying down, beside Contacts' list) or a large font never squeezes a button
 * into a circle with its label broken letter by letter (beta feedback of 2026-11-14).
 */
@Composable
fun FittingButtonsRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier,
    ) { measurables, constraints ->
        if (measurables.isEmpty()) return@Layout layout(constraints.minWidth, constraints.minHeight) {}
        val gap = spacing.roundToPx()
        val gaps = gap * (measurables.size - 1)
        val widest = measurables.maxOf { it.maxIntrinsicWidth(constraints.maxHeight) }
        // Why: unbounded (a horizontal scroll), the row is as wide as the widest button times their count.
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else widest * measurables.size + gaps
        val fitsInRow = widest * measurables.size + gaps <= width
        if (fitsInRow) {
            val itemWidth = (width - gaps) / measurables.size
            val placeables = measurables.map {
                it.measure(
                    constraints.copy(
                        minWidth = itemWidth,
                        maxWidth = itemWidth,
                        minHeight = 0,
                    ),
                )
            }
            val height = placeables.maxOf { it.height }.coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(width, height) {
                var x = 0
                placeables.forEach { placeable ->
                    placeable.placeRelative(x, (height - placeable.height) / 2)
                    x += itemWidth + gap
                }
            }
        } else {
            val placeables = measurables.map {
                it.measure(Constraints.fixedWidth(width).copy(maxHeight = constraints.maxHeight))
            }
            val height = (placeables.sumOf { it.height } + gaps).coerceAtLeast(constraints.minHeight)
            layout(width, height) {
                var y = 0
                placeables.forEach { placeable ->
                    placeable.placeRelative(0, y)
                    y += placeable.height + gap
                }
            }
        }
    }
}
