package com.quare.blitzsplit.feature.watchpairing.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.StartScreenViews
import com.quare.blitzsplit.core.sync.domain.usecase.StartSync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/*
 * Why: the watch app's root, as AppViewModel is the phone's: it starts the watch's own sync, opens the balances once
 * the watch has the account's profile and the connect screens otherwise, and goes back to them when a sign out (the
 * phone's, or a session the server refused) erased the watch's copy.
 */
class WatchRootViewModel(
    navigator: Navigator,
    observeProfile: ObserveProfile,
    startScreenViews: StartScreenViews,
    startSync: StartSync,
) : ViewModel() {
    // Why: false until the first screen is chosen, so the watch never flashes the connect screen of a paired watch.
    val isReady: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val readyWatcher: FlowWatcher<Boolean> = FlowWatcher(isReady)

    init {
        startSync()
        viewModelScope.launch {
            val hasProfile = observeProfile().first() != null
            navigator.replaceAll(if (hasProfile) Route.WatchBalances else Route.WatchConnect)
            isReady.value = true
            startScreenViews()
        }
        observeProfile()
            .map { it != null }
            .distinctUntilChanged()
            .drop(1)
            .filter { hasProfile -> !hasProfile }
            .onEach { navigator.replaceAll(Route.WatchConnect) }
            .launchIn(viewModelScope)
    }
}
