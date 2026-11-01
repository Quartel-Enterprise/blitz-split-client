package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/*
 * Why: where a window's two halves meet, on screen: along x for the halves of a book, along y for the top and bottom
 * of a tabletop. The band is the hinge's own width, at least the canvas' 24dp hinge-gap centered on it.
 */
@Immutable
data class HingeSplit(
    val bandStart: Dp,
    val bandEnd: Dp,
)
