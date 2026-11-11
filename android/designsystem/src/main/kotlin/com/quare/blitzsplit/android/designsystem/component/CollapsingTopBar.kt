package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.animation.core.animate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.quare.blitzsplit.core.model.HomeTab

/*
 * Why: on a phone upright the top bar, and the home's bottom bar with it (HomeBars.collapsedFraction), leave on a
 * scroll down and come back on a scroll up (docs/product/home-and-shell.md); anywhere wider the screen keeps its own
 * behavior (otherwise), as the rail stays. A screen entered again (a tab switched to, a screen gone back to) starts
 * with its bars out. The bars start leaving only while the content scrolls, and once gone any drag down brings them
 * back, even over content that no longer scrolls: judged with the bars out, a list that fits only because they left
 * would bounce them back in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberCollapsingTopBarScrollBehavior(
    collapses: Boolean,
    canScroll: () -> Boolean,
    otherwise: @Composable (TopAppBarState) -> TopAppBarScrollBehavior = { state ->
        TopAppBarDefaults.pinnedScrollBehavior(state)
    },
): TopAppBarScrollBehavior {
    // Why: saved, so a wider layout's bar keeps how far it folded across a rotation, as Material's own state does.
    val state = rememberTopAppBarState()
    val currentCanScroll by rememberUpdatedState(canScroll)
    // Why: before the first frame, so a screen entered again never flashes its bars folded.
    remember(state) {
        if (collapses) state.heightOffset = 0f
    }
    // Why: whether the bars could leave in the last composition, so leaving the phone's layout brings them back.
    val wasCollapsing = remember { booleanArrayOf(collapses) }
    LaunchedEffect(collapses) {
        if (wasCollapsing[0] && !collapses && state.heightOffset != 0f) {
            animate(
                initialValue = state.heightOffset,
                targetValue = 0f,
            ) { value, _ -> state.heightOffset = value }
        }
        wasCollapsing[0] = collapses
    }
    return if (collapses) {
        TopAppBarDefaults.enterAlwaysScrollBehavior(
            state = state,
            canScroll = { state.heightOffset < 0f || currentCanScroll() },
        )
    } else {
        otherwise(state)
    }
}

// Why: the scroll reaches the bars only while they can leave, so a wider layout's bar never tints on a scroll.
@OptIn(ExperimentalMaterial3Api::class)
fun Modifier.collapsingBarsScroll(
    scrollBehavior: TopAppBarScrollBehavior,
    collapses: Boolean,
): Modifier = if (collapses) nestedScroll(scrollBehavior.nestedScrollConnection) else this

/*
 * Why: the bars of a home tab with one list (Balances, Contacts): the tab selected, Search, "Split a bill" atop the
 * rail, and the bottom bar leaving with the screen's top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
fun homeTabBarsOf(
    usesRail: Boolean,
    selected: HomeTab,
    onTabClick: (HomeTab) -> Unit,
    onSearchClick: () -> Unit,
    onSplitBillClick: (() -> Unit)?,
    scrollBehavior: TopAppBarScrollBehavior,
): HomeBars = HomeBars(
    kind = homeBarsKindOf(usesRail),
    selected = selected,
    onTabClick = onTabClick,
    onSearchClick = onSearchClick,
    railHeader = { isShort -> SplitBillRailHeader(isShort, onSplitBillClick) },
    collapsedFraction = { scrollBehavior.state.collapsedFraction },
)
