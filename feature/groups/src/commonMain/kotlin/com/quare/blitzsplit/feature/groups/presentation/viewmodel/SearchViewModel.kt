package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.groups.presentation.mapper.ActivityClock
import com.quare.blitzsplit.feature.groups.presentation.mapper.GroupWithMoney
import com.quare.blitzsplit.feature.groups.presentation.mapper.findResultsIn
import com.quare.blitzsplit.feature.groups.presentation.mapper.groupsWithMoneyOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.rowsOf
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearch
import com.quare.blitzsplit.feature.groups.presentation.model.SearchUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.SearchUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/*
 * Why: "Buscar" (5.2) over the home tab it was opened from (Route.Search): the same search as Grupos', over the groups,
 * people and bills on the device, the settled groups included, offline too. Like ⌘K, what it finds opens in its place,
 * so back goes to the tab.
 */
class SearchViewModel(
    private val navigator: Navigator,
    currentTimeMillis: CurrentTimeMillis,
    currentTimeZone: CurrentTimeZone,
    observeGroups: ObserveGroups,
    observeGroupsMoney: ObserveGroupsMoney,
    trackEvent: TrackEvent,
) : TrackedViewModel<SearchUiEvent>(trackEvent) {
    val uiState: StateFlow<SearchUiState>
        field = MutableStateFlow(SearchUiState(search = GroupsSearch.closed.copy(isOpen = true)))

    val stateWatcher: FlowWatcher<SearchUiState> = FlowWatcher(uiState)

    private var groups = emptyList<GroupWithMoney>()

    // Why: the home's order, the most recent activity first, which the groups found keep.
    private var rows = emptyList<GroupRow>()

    init {
        // Why: what the search found changes with the device's data, a sync's included.
        combine(observeGroups(), observeGroupsMoney(), ::groupsWithMoneyOf)
            .onEach { items ->
                // Why: the search shows no age, so the rows need no minute ticks.
                val clock = ActivityClock(
                    nowMillis = currentTimeMillis(),
                    timeZone = currentTimeZone(),
                )
                groups = items
                rows = rowsOf(items, clock)
                search { it }
                // Why: as Grupos' own search, it closes once a sync leaves no group to look in.
                if (items.isEmpty()) handleEvent(SearchUiEvent.OnClosed)
            }.launchIn(viewModelScope)
    }

    override fun handleEvent(event: SearchUiEvent) {
        when (event) {
            is SearchUiEvent.OnQueryChanged -> search { it.copy(query = event.query) }

            is SearchUiEvent.OnScopeChanged -> search { it.copy(scope = event.scope) }

            is SearchUiEvent.OnGroupClicked -> openInstead(groupRouteOf(event.groupId, GroupTab.EXPENSES))

            is SearchUiEvent.OnPersonClicked -> openInstead(groupRouteOf(event.groupId, GroupTab.MEMBERS))

            is SearchUiEvent.OnBillClicked -> openInstead(
                groupRouteOf(event.groupId, GroupTab.EXPENSES),
                Route.BillDetail(event.groupId, event.expenseId, focusCommentId = null),
            )

            SearchUiEvent.OnClosed -> if (isOnTop()) navigator.navigateBack()
        }
    }

    private fun search(change: (GroupsSearch) -> GroupsSearch) {
        uiState.update { state -> state.copy(search = change(state.search).findResultsIn(groups, rows)) }
    }

    /*
     * Why: in one step, so a UI never draws the tab between the search and what it opened; only from the search on
     * top, so a second tap before the first one's screen showed doesn't replace that screen.
     */
    private fun openInstead(vararg routes: Route) {
        if (!isOnTop()) return
        navigator.replaceAbove(navigator.backStack.value.lastIndex, routes.toList())
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == Route.Search

    private fun groupRouteOf(
        groupId: String,
        tab: GroupTab,
    ): Route = Route.GroupDetail(
        groupId,
        joinedAs = null,
        tab = tab,
    )
}
