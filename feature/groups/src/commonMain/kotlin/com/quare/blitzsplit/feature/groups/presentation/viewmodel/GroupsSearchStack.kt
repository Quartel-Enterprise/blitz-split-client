package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/*
 * Why: Grupos' own search (state, with the first result beside the list) follows the back stack. It closes once Grupos
 * leaves it, as iOS keeps the ViewModel past Grupos and its search tab would otherwise stay in front of Atividade or a
 * tab. And "Buscar" from a page over Grupos (the web's sidebar, Android's rail beside a group) pushes Route.Search over
 * Grupos, which Grupos takes for its own once there is a group to look in, already counted by whoever opened it.
 */
internal class GroupsSearchStack(
    private val navigator: Navigator,
) {
    fun watch(
        scope: CoroutineScope,
        canSearch: Flow<Boolean>,
        isOpen: () -> Boolean,
        close: () -> Unit,
        open: () -> Unit,
    ) {
        navigator.backStack
            .onEach { stack -> if (Route.Groups !in stack && isOpen()) close() }
            .launchIn(scope)
        combine(navigator.backStack, canSearch, ::Pair)
            .onEach { (stack, isSearchable) ->
                if (!isSearchable || !stack.hasSearchOverGroups()) return@onEach
                navigator.navigateBack()
                open()
            }.launchIn(scope)
    }

    private fun List<Route>.hasSearchOverGroups(): Boolean =
        lastOrNull() == Route.Search && getOrNull(lastIndex - 1) == Route.Groups
}

// Why: a tab of the bar around the search in Grupos' place, whose Grupos leaves the search for the groups.
internal fun HomeTab.toGroupsEvent(): GroupsUiEvent = when (this) {
    HomeTab.HOME -> GroupsUiEvent.OnHomeTabClicked
    HomeTab.GROUPS -> GroupsUiEvent.OnSearchTabLeft
    HomeTab.BALANCES -> GroupsUiEvent.OnBalancesTabClicked
    HomeTab.CONTACTS -> GroupsUiEvent.OnContactsTabClicked
}
