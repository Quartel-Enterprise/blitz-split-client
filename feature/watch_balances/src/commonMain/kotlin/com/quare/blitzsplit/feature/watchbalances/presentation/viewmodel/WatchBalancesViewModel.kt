package com.quare.blitzsplit.feature.watchbalances.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveLastSyncedAt
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.sync.domain.usecase.SignOutWhenSynced
import com.quare.blitzsplit.feature.watchbalances.presentation.mapper.WatchBalancesMapper
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchBalancesUiEvent
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchBalancesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import software.quare.connectivity.NetworkConnectivityObserver

/*
 * Why: the watch's "Balances" (15.3), from the watch's own Room: the overall balance on top, each open group under it
 * and the last sync at the end. It opens with what the watch has and follows each sync.
 */
class WatchBalancesViewModel(
    private val navigator: Navigator,
    private val signOutWhenSynced: SignOutWhenSynced,
    observePeopleBalances: ObservePeopleBalances,
    observeGroupsMoney: ObserveGroupsMoney,
    observeFirstDownload: ObserveFirstDownload,
    observeLastSyncedAt: ObserveLastSyncedAt,
    connectivityObserver: NetworkConnectivityObserver,
    trackEvent: TrackEvent,
) : TrackedViewModel<WatchBalancesUiEvent>(trackEvent) {
    val uiState: StateFlow<WatchBalancesUiState>
        field = MutableStateFlow(
            WatchBalancesUiState(
                isLoaded = false,
                totals = emptyList(),
                groups = emptyList(),
                isOnline = true,
                queuedChangeCount = 0,
                lastSyncedAt = null,
                isConfirmingSignOut = false,
            ),
        )

    val stateWatcher: FlowWatcher<WatchBalancesUiState> = FlowWatcher(uiState)

    init {
        combine(
            observePeopleBalances(),
            observeGroupsMoney(),
            observeFirstDownload(),
        ) { balances, money, firstDownload ->
            uiState.update {
                it.copy(
                    isLoaded = firstDownload == null,
                    totals = WatchBalancesMapper.totalsOf(balances),
                    groups = WatchBalancesMapper.groupsOf(balances, money),
                    queuedChangeCount = WatchBalancesMapper.queuedCountOf(money),
                )
            }
        }.launchIn(viewModelScope)
        observeLastSyncedAt()
            .onEach { syncedAt -> uiState.update { it.copy(lastSyncedAt = syncedAt) } }
            .launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    /*
     * Why: the watch alone signs out (the phone keeps its session), once what it queued went up, so a payment recorded
     * offline isn't lost; the root then shows the connect screens.
     */
    private fun signOut() {
        uiState.update { it.copy(isConfirmingSignOut = false) }
        signOutWhenSynced()
    }

    // Why: ✓ and ✕ only answer the question while it shows.
    override fun isAccepted(event: WatchBalancesUiEvent): Boolean =
        if (event == WatchBalancesUiEvent.OnSignOutConfirmed || event == WatchBalancesUiEvent.OnSignOutDismissed) {
            uiState.value.isConfirmingSignOut
        } else {
            true
        }

    override fun handleEvent(event: WatchBalancesUiEvent) {
        when (event) {
            is WatchBalancesUiEvent.OnGroupClicked -> navigator.navigate(
                Route.WatchGroup(event.groupId, event.currency),
            )

            WatchBalancesUiEvent.OnSignOutClicked -> uiState.update { it.copy(isConfirmingSignOut = true) }

            WatchBalancesUiEvent.OnSignOutConfirmed -> signOut()

            WatchBalancesUiEvent.OnSignOutDismissed -> uiState.update { it.copy(isConfirmingSignOut = false) }
        }
    }
}
