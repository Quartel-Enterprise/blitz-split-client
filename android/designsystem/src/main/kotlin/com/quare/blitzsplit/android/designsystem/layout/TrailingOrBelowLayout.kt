package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp

/*
 * Why: a row's text and what trails it (an amount, a button) side by side while the text keeps room to read, the
 * trailing part under the text once it doesn't. In a narrow pane (a phone lying down, the group beside the list) the
 * amount and the ⋮ used to squeeze the member's name and "This device" to a letter per line (beta feedback of
 * 2026-11-14). Side by side the text takes all the width the trailing part leaves and the two center on each other;
 * stacked both start at the start edge. The text needs `minMainWidth`, or less when all of it fits in less.
 */
@Composable
fun TrailingOrBelowLayout(
    main: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    minMainWidth: Dp = 160.dp,
    spacing: Dp = 12.dp,
    belowSpacing: Dp = 4.dp,
) {
    Layout(
        contents = listOf(main, trailing),
        modifier = modifier,
    ) { (mainMeasurables, trailingMeasurables), constraints ->
        val maxWidth = constraints.maxWidth
        val trailingMeasurable = trailingMeasurables.firstOrNull()
        val mainMeasurable = mainMeasurables.first()
        if (trailingMeasurable == null) {
            val placeable = mainMeasurable.measure(constraints.copy(minHeight = 0))
            return@Layout layout(
                constraints.constrainWidth(placeable.width),
                constraints.constrainHeight(placeable.height),
            ) {
                placeable.placeRelative(0, 0)
            }
        }
        val spacingPx = spacing.roundToPx()
        val trailingWidth = trailingMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        val mainWanted = minOf(mainMeasurable.maxIntrinsicWidth(Constraints.Infinity), minMainWidth.roundToPx())
        val isSideBySide = maxWidth == Constraints.Infinity ||
            maxWidth - trailingWidth - spacingPx >= mainWanted
        if (isSideBySide) {
            val trailingPlaceable = trailingMeasurable.measure(Constraints(maxWidth = trailingWidth))
            val mainMaxWidth = if (maxWidth == Constraints.Infinity) {
                Constraints.Infinity
            } else {
                (maxWidth - trailingPlaceable.width - spacingPx).coerceAtLeast(0)
            }
            val mainPlaceable = mainMeasurable.measure(
                Constraints(
                    minWidth = if (mainMaxWidth == Constraints.Infinity) 0 else mainMaxWidth,
                    maxWidth = mainMaxWidth,
                ),
            )
            val width = constraints.constrainWidth(mainPlaceable.width + spacingPx + trailingPlaceable.width)
            val height = constraints.constrainHeight(maxOf(mainPlaceable.height, trailingPlaceable.height))
            layout(width, height) {
                mainPlaceable.placeRelative(0, (height - mainPlaceable.height) / 2)
                trailingPlaceable.placeRelative(
                    width - trailingPlaceable.width,
                    (height - trailingPlaceable.height) / 2,
                )
            }
        } else {
            val childConstraints = Constraints(maxWidth = maxWidth)
            val mainPlaceable = mainMeasurable.measure(childConstraints.copy(minWidth = maxWidth))
            val trailingPlaceable = trailingMeasurable.measure(childConstraints)
            val below = belowSpacing.roundToPx()
            layout(maxWidth, constraints.constrainHeight(mainPlaceable.height + below + trailingPlaceable.height)) {
                mainPlaceable.placeRelative(0, 0)
                trailingPlaceable.placeRelative(0, mainPlaceable.height + below)
            }
        }
    }
}
