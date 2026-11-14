package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/*
 * Why: the side margins of the form's column, which a row that scrolls sideways (Dividir's modes) reaches into, so it
 * scrolls to the column's edge rather than stopping short of it (beta feedback: no cut on either side).
 */
@Immutable
data class FormBleed(
    val start: Dp,
    val end: Dp,
) {
    companion object {
        val None = FormBleed(
            start = 0.dp,
            end = 0.dp,
        )

        fun of(
            padding: PaddingValues,
            layoutDirection: LayoutDirection,
        ): FormBleed = FormBleed(
            start = padding.calculateStartPadding(layoutDirection),
            end = padding.calculateEndPadding(layoutDirection),
        )
    }
}

val LocalFormBleed = staticCompositionLocalOf { FormBleed.None }

// Why: lays the content out wider than its slot by `bleed` on each side, into the margins around it.
fun Modifier.horizontalBleed(bleed: FormBleed): Modifier = layout { measurable, constraints ->
    val start = bleed.start.roundToPx()
    val end = bleed.end.roundToPx()
    val extra = start + end
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra,
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else Constraints.Infinity,
        ),
    )
    layout(
        width = (placeable.width - extra).coerceAtLeast(0),
        height = placeable.height,
    ) { placeable.placeRelative(-start, 0) }
}
