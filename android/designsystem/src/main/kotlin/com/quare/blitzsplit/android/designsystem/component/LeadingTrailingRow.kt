package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Why: something at the start of a line and something at its end, on one line while both fit at their full one-line
 * width, else the end on a line of its own under the start, still at the end. Judged by the full width, never a
 * text's longest word, so a label is never cut or broken to stay on the first line (beta feedback of 2026-11-14).
 */
@Composable
fun LeadingTrailingRow(
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
) {
    Layout(
        contents = listOf(leading, trailing),
        modifier = modifier,
    ) { (leadingMeasurables, trailingMeasurables), constraints ->
        val gap = spacing.roundToPx()
        val start = leadingMeasurables.single()
        val end = trailingMeasurables.single()
        val startWidth = start.maxIntrinsicWidth(constraints.maxHeight)
        val endWidth = end.maxIntrinsicWidth(constraints.maxHeight)
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else startWidth + gap + endWidth
        val loose = Constraints(
            maxWidth = width,
            maxHeight = constraints.maxHeight,
        )
        val startPlaceable = start.measure(loose)
        val endPlaceable = end.measure(loose)
        if (startWidth + gap + endWidth <= width) {
            val height = maxOf(startPlaceable.height, endPlaceable.height).coerceAtLeast(constraints.minHeight)
            layout(width, height) {
                startPlaceable.placeRelative(0, (height - startPlaceable.height) / 2)
                endPlaceable.placeRelative(width - endPlaceable.width, (height - endPlaceable.height) / 2)
            }
        } else {
            val height = (startPlaceable.height + endPlaceable.height).coerceAtLeast(constraints.minHeight)
            layout(width, height) {
                startPlaceable.placeRelative(0, 0)
                endPlaceable.placeRelative(width - endPlaceable.width, startPlaceable.height)
            }
        }
    }
}
