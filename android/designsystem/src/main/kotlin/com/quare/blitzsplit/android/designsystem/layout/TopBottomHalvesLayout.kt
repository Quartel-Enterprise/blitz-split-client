package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/*
 * Why: a screen the canvas draws its own way on a tabletop (half folded, hinge across): what is read or scanned in
 * the top half, the controls in the bottom one, within thumb reach. It needs a bounded height (a full screen screen,
 * never inside a vertical scroll); each half scrolls on its own. Off a tabletop, two equal halves around the gap.
 */
@Composable
fun TopBottomHalvesLayout(
    top: @Composable BoxScope.() -> Unit,
    bottom: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    gap: Dp = HingeBand,
) {
    HalvesLayout(
        split = rememberFoldLayout().tabletopSplit,
        isSideBySide = false,
        gap = gap,
        first = top,
        second = bottom,
        modifier = modifier,
    )
}
