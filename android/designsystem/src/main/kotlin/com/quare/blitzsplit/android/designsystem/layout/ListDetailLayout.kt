package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val tabletListPaneWidth = 520.dp

// Why: the gap between the list and the detail, the hinge band of an unfolded book.
val ListDetailPaneSpacing: Dp = HingeBand

/*
 * Why: the canvas splits Grupos and Saldos into a list and its detail only on a tablet lying down (1280×800) and an
 * unfolded book (841×673, the tabletop lying down alike), the book's halves on the hinge it reports, so a Galaxy Z
 * Fold held as a book (673×841, hinge down the middle) splits too. A phone lying down (915×412) keeps its two columns
 * of one screen, a tablet or a foldable upright one centered column.
 */
@Immutable
data class ListDetailLayout(
    // Why: the rail and the list, 520dp on a tablet, the half before the hinge on a book.
    val listPaneWidth: Dp,
    // Why: a book's halves are narrow, so the panes keep the tighter margins of the canvas.
    val isBook: Boolean,
)

// Why: null while the window draws one pane, as a phone, a phone lying down and anything upright do.
@Composable
fun rememberListDetailLayout(): ListDetailLayout? {
    val fold = rememberFoldLayout()
    val layoutDirection = LocalLayoutDirection.current
    return remember(fold, layoutDirection) { listDetailLayoutOf(fold, layoutDirection) }
}

private fun listDetailLayoutOf(
    fold: FoldLayout,
    layoutDirection: LayoutDirection,
): ListDetailLayout? {
    val split = fold.bookSplit
    if (split != null) {
        val window = fold.windowBounds
        return ListDetailLayout(
            listPaneWidth = if (layoutDirection == LayoutDirection.Ltr) {
                split.bandStart - window.left
            } else {
                window.right - split.bandEnd
            },
            isBook = true,
        )
    }
    return if (fold.formFactor == FormFactor.TabletLandscape) {
        ListDetailLayout(
            listPaneWidth = tabletListPaneWidth,
            isBook = false,
        )
    } else {
        null
    }
}

/*
 * Why: a group's screen drawn in the pane beside the home's list. It lays out as the one centered column of a tablet
 * upright, whatever the window, and drops what the list beside it already has: the back arrow and "Rachar conta".
 */
val LocalInDetailPane = staticCompositionLocalOf { false }
