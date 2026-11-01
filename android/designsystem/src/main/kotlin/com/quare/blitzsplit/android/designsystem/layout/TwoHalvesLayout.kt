package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/*
 * Why: a screen the canvas splits on a book's hinge (flows 1 to 4: Boas-vindas, Perfil, Recuperar, Convidar): each
 * half on its own side, the gap centered on the hinge and never narrower than its band (the 72dp of flows 3 and 4 is
 * the 24dp band plus 24dp each side). Off a book, or before the hinge is known, two equal halves around the gap, so a
 * preview at 841×673 draws the same.
 */
@Composable
fun TwoHalvesLayout(
    start: @Composable BoxScope.() -> Unit,
    end: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    gap: Dp = HingeBand,
) {
    HalvesLayout(
        split = rememberFoldLayout().bookSplit,
        isSideBySide = true,
        gap = gap,
        first = start,
        second = end,
        modifier = modifier,
    )
}
