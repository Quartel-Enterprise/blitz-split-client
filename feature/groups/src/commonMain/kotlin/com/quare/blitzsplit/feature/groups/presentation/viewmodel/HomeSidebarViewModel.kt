package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityUnreadCount
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import com.quare.blitzsplit.core.groups.domain.model.UserBadge
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openHomeTab
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.groups.presentation.mapper.ActivityClock
import com.quare.blitzsplit.feature.groups.presentation.mapper.groupsWithMoneyOf
import com.quare.blitzsplit.feature.groups.presentation.mapper.rowsOf
import com.quare.blitzsplit.feature.groups.presentation.model.HomeSidebarUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.HomeSidebarUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class HomeSidebarViewModel(
    private val navigator: Navigator,
    currentTimeMillis: CurrentTimeMillis,
    currentTimeZone: CurrentTimeZone,
    observeProfile: ObserveProfile,
    observeAccount: ObserveAccount,
    observeGroups: ObserveGroups,
    observeGroupsMoney: ObserveGroupsMoney,
    observeActivityUnreadCount: ObserveActivityUnreadCount,
) : ViewModel() {
    val uiState: StateFlow<HomeSidebarUiState>
        field = MutableStateFlow(
            HomeSidebarUiState(
                displayName = "",
                avatarInitial = "",
                avatarColor = AvatarColor.default,
                photoUrl = null,
                accountProvider = null,
                openGroups = emptyList(),
                activityUnreadCount = 0,
                isLoaded = false,
            ),
        )

    val stateWatcher: FlowWatcher<HomeSidebarUiState> = FlowWatcher(uiState)

    init {
        combine(observeProfile(), observeAccount(), ::Pair)
            .onEach { (profile, account) ->
                val badge = UserBadge.of(profile, account.status)
                uiState.update {
                    it.copy(
                        displayName = badge.displayName,
                        avatarInitial = badge.avatarInitial,
                        avatarColor = badge.avatarColor,
                        photoUrl = badge.photoUrl,
                        accountProvider = badge.accountProvider,
                    )
                }
            }.launchIn(viewModelScope)
        // Why: the sidebar shows no age, so the rows need no minute ticks.
        combine(observeGroups(), observeGroupsMoney(), ::groupsWithMoneyOf)
            .onEach { items ->
                val clock = ActivityClock(
                    nowMillis = currentTimeMillis(),
                    timeZone = currentTimeZone(),
                )
                uiState.update { state ->
                    state.copy(
                        openGroups = rowsOf(items, clock).filterNot { it.isSettled },
                        isLoaded = true,
                    )
                }
            }.launchIn(viewModelScope)
        observeActivityUnreadCount()
            .onEach { count -> uiState.update { it.copy(activityUnreadCount = count) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: HomeSidebarUiEvent) {
        when (event) {
            is HomeSidebarUiEvent.OnGroupClicked -> openGroup(event.groupId)
            is HomeSidebarUiEvent.OnTabClicked -> navigator.openHomeTab(event.tab)
            HomeSidebarUiEvent.OnProfileClicked -> navigator.navigate(Route.Profile)
        }
    }

    /*
     * Why: "Em aberto" is Grupos' list, so a group opens over Grupos as its row does, whichever tab was in front, in
     * the place of whatever Grupos opened; the group already right above Grupos goes back to it, keeping its tab.
     */
    private fun openGroup(groupId: String) {
        val stack = navigator.backStack.value
        val groupsIndex = stack.indexOf(Route.Groups)
        val homeIndex = stack.indexOf(Route.Home)
        val group = Route.GroupDetail(
            groupId,
            joinedAs = null,
            tab = GroupTab.EXPENSES,
        )
        when {
            groupsIndex >= 0 && (stack.getOrNull(groupsIndex + 1) as? Route.GroupDetail)?.groupId == groupId ->
                navigator.popTo(groupsIndex + 2)

            groupsIndex >= 0 -> navigator.replaceAbove(groupsIndex + 1, group)

            homeIndex >= 0 -> {
                navigator.replaceAbove(homeIndex + 1, Route.Groups)
                navigator.navigate(group)
            }
        }
    }
}
