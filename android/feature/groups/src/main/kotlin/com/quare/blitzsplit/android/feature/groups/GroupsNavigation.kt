package com.quare.blitzsplit.android.feature.groups

import com.quare.blitzsplit.android.designsystem.component.HomeBars
import com.quare.blitzsplit.android.designsystem.component.SplitBillRailHeader
import com.quare.blitzsplit.android.designsystem.component.homeBarsKindOf
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiState

/*
 * Why: the search open in Grupos' place selects Search, the bar's last item, and Grupos then leaves it for the groups;
 * otherwise Grupos is selected and Search opens it.
 */
private fun eventOf(
    tab: HomeTab,
    isSearching: Boolean,
): GroupsUiEvent? = when (tab) {
    HomeTab.HOME -> GroupsUiEvent.OnHomeTabClicked
    HomeTab.GROUPS -> GroupsUiEvent.OnSearchTabLeft.takeIf { isSearching }
    HomeTab.BALANCES -> GroupsUiEvent.OnBalancesTabClicked
    HomeTab.CONTACTS -> GroupsUiEvent.OnContactsTabClicked
}

// Why: Grupos' bar or rail, the rail with "Rachar conta" on top.
internal fun groupsBarsOf(
    state: GroupsUiState,
    onEvent: (GroupsUiEvent) -> Unit,
    usesRail: Boolean,
    collapsedFraction: () -> Float,
): HomeBars {
    val onSplitBillClick = { onEvent(GroupsUiEvent.OnSplitBillClicked) }.takeIf { state.showsSplitBillButton }
    return HomeBars(
        kind = homeBarsKindOf(usesRail),
        selected = HomeTab.GROUPS.takeUnless { state.isSearching },
        onTabClick = { tab -> eventOf(tab, state.isSearching)?.let(onEvent) },
        onSearchClick = { onEvent(GroupsUiEvent.OnSearchOpened) },
        railHeader = { isShort -> SplitBillRailHeader(isShort, onSplitBillClick) },
        collapsedFraction = collapsedFraction,
    )
}
