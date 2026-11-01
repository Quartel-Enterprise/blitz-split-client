package com.quare.blitzsplit.android.feature.join

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout

// Why: how the canvas lays the invite and "Quem é você no grupo?" out at each size (3.1, 3.2).
internal enum class JoinLayout {
    // Why: a phone upright, and a tablet or a foldable upright: one centered column of at most 560dp.
    COLUMN,

    // Why: a phone lying down: a 330dp column of context on the left, the rest on the right.
    PHONE_LANDSCAPE,

    // Why: a tablet lying down: the same two columns in a 920dp container, the first one 380dp.
    TABLET_LANDSCAPE,

    // Why: a book: one column in each half, 24dp off the hinge's band on each side.
    BOOK,

    // Why: a tabletop: what is read on top, the buttons in the bottom half. Only the invite has its own board.
    TABLETOP,
}

/*
 * Why: "Quem é você" has no tabletop board of its own and is drawn like the book there (`hasTabletop` false), its
 * halves side by side as a tabletop lying down has them.
 */
@Composable
internal fun rememberJoinLayout(hasTabletop: Boolean = true): JoinLayout {
    val fold = rememberFoldLayout()
    return when {
        hasTabletop && fold.tabletopSplit != null -> JoinLayout.TABLETOP
        fold.bookSplit != null -> JoinLayout.BOOK
        fold.formFactor == FormFactor.PhoneLandscape -> JoinLayout.PHONE_LANDSCAPE
        fold.formFactor == FormFactor.TabletLandscape -> JoinLayout.TABLET_LANDSCAPE
        else -> JoinLayout.COLUMN
    }
}
