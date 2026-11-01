package com.quare.blitzsplit.android.feature.groups

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Why: the canvas' sizes per layout. A phone lying down, a book and a tabletop are short, so the faces, the balance
 * and the cards shrink and the shortcuts lay their icon beside the label.
 */
@Immutable
internal data class HomeMetrics(
    val isCompact: Boolean,
    val groupCardWidth: Dp,
    val groupCardHeight: Dp,
    // Why: the side margin of a column, inside which the rows of faces and cards scroll edge to edge.
    val gutter: Dp,
) {
    val faceSize: Dp
        get() = if (isCompact) 48.dp else 56.dp
}
