package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.DpRect

/*
 * Why: the fold a foldable reports (androidx.window's FoldingFeature) where it crosses the app's window, on screen, so
 * a dialog's own window lines up with it too. Flat and half opened tell the book apart from the tabletop.
 */
@Immutable
data class FoldHinge(
    val bounds: DpRect,
    val isVertical: Boolean,
    val isFlat: Boolean,
    val isSeparating: Boolean,
)
