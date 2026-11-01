package com.quare.blitzsplit.android.feature.groups

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.FoldLayout
import com.quare.blitzsplit.android.designsystem.layout.FormFactor
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout

/*
 * Why: how the canvas draws Início at each size (F10AndroidHome and F10AndroidHomeWide): one column on a phone, one
 * centered column beside the rail upright on a tablet or a foldable, two columns beside the rail lying down, the
 * book's halves on its hinge, a tabletop's blocks above and below its hinge, and only the balance and the latest bill
 * on a Flip's cover screen.
 */
internal enum class HomeLayout(
    val usesRail: Boolean,
) {
    PHONE(usesRail = false),
    ONE_COLUMN(usesRail = true),
    TWO_COLUMNS(usesRail = true),
    BOOK(usesRail = true),
    TABLETOP(usesRail = true),
    COVER(usesRail = false),
    ;

    // Why: one column keeps the top bar with the profile; two columns put it at the top of the first column.
    val usesTopBar: Boolean
        get() = this == PHONE || this == ONE_COLUMN
}

@Composable
internal fun rememberHomeLayout(): HomeLayout {
    val fold = rememberFoldLayout()
    return remember(fold) { homeLayoutOf(fold) }
}

// Why: the screen, not the window, tells the cover screen (FoldLayout), so a phone's split screen keeps the phone's.
private fun homeLayoutOf(fold: FoldLayout): HomeLayout {
    if (fold.isOnCoverScreen) return HomeLayout.COVER
    return when (fold.formFactor) {
        FormFactor.Phone -> HomeLayout.PHONE

        FormFactor.TabletPortrait, FormFactor.FoldableUpright -> HomeLayout.ONE_COLUMN

        FormFactor.PhoneLandscape, FormFactor.TabletLandscape -> HomeLayout.TWO_COLUMNS

        FormFactor.Book -> HomeLayout.BOOK

        // Why: a tabletop that reports no hinge (none to split on) draws like the book it is the size of.
        FormFactor.Tabletop -> if (fold.tabletopSplit != null) HomeLayout.TABLETOP else HomeLayout.BOOK
    }
}

@Composable
internal fun rememberHomeMetrics(layout: HomeLayout): HomeMetrics {
    val formFactor = rememberFoldLayout().formFactor
    return remember(layout, formFactor) {
        when (layout) {
            HomeLayout.PHONE, HomeLayout.COVER -> HomeMetrics(
                isCompact = false,
                groupCardWidth = 180.dp,
                groupCardHeight = 148.dp,
                gutter = 16.dp,
            )

            HomeLayout.ONE_COLUMN -> HomeMetrics(
                isCompact = false,
                groupCardWidth = 196.dp,
                groupCardHeight = 140.dp,
                gutter = 24.dp,
            )

            HomeLayout.TWO_COLUMNS -> if (formFactor == FormFactor.TabletLandscape) {
                HomeMetrics(
                    isCompact = false,
                    groupCardWidth = 190.dp,
                    groupCardHeight = 140.dp,
                    gutter = 24.dp,
                )
            } else {
                HomeMetrics(
                    isCompact = true,
                    groupCardWidth = 160.dp,
                    groupCardHeight = 128.dp,
                    gutter = 16.dp,
                )
            }

            HomeLayout.BOOK, HomeLayout.TABLETOP -> HomeMetrics(
                isCompact = true,
                groupCardWidth = 160.dp,
                groupCardHeight = 128.dp,
                gutter = 16.dp,
            )
        }
    }
}
