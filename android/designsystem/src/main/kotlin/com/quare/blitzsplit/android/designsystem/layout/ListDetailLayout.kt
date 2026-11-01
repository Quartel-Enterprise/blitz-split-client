package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val expandedWidth = 840.dp
private val compactHeight = 480.dp
private val bookLongestSide = 960.dp
private val tabletListPaneWidth = 520.dp

// Why: the gap between the list and the detail, the hinge band of an unfolded book.
val ListDetailPaneSpacing: Dp = 24.dp

/*
 * Why: the canvas splits Grupos and Saldos into a list and its detail only on an expanded window that isn't a phone
 * lying down: a tablet lying down (1280×800) and an unfolded book (841×673, the tabletop alike). A phone lying down
 * (915×412) keeps its two columns of one screen, a tablet or a foldable upright one centered column.
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
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return remember(size, density) {
        with(density) {
            val width = size.width.toDp()
            val height = size.height.toDp()
            // Why: a large tablet upright can pass 840dp of width, and still keeps its one centered column.
            if (width < expandedWidth || height < compactHeight || width <= height) return@remember null
            val isBook = width < bookLongestSide
            ListDetailLayout(
                listPaneWidth = if (isBook) (width - ListDetailPaneSpacing) / 2 else tabletListPaneWidth,
                isBook = isBook,
            )
        }
    }
}

/*
 * Why: a group's screen drawn in the pane beside the home's list. It lays out as the one centered column of a tablet
 * upright, whatever the window, and drops what the list beside it already has: the back arrow and "Rachar conta".
 */
val LocalInDetailPane = staticCompositionLocalOf { false }
