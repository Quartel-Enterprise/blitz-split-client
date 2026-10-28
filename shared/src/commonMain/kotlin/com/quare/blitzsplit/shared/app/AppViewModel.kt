package com.quare.blitzsplit.shared.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.account.domain.usecase.ObserveReturningBrowserSignIn
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveThemePreference
import com.quare.blitzsplit.core.sync.domain.usecase.StartSync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val observeProfile: ObserveProfile,
    val navigator: Navigator,
    private val observeAccount: ObserveAccount,
    private val observeReturningBrowserSignIn: ObserveReturningBrowserSignIn,
    startSync: StartSync,
    observeThemePreference: ObserveThemePreference,
) : ViewModel() {
    val uiState: StateFlow<AppUiState>
        field = MutableStateFlow(AppUiState.LOADING)

    val theme: StateFlow<ThemePreference> = observeThemePreference()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreference.SYSTEM)

    val stateWatcher: FlowWatcher<AppUiState> = FlowWatcher(uiState)
    val backStackWatcher: FlowWatcher<List<Route>> = FlowWatcher(navigator.backStack)
    val themeWatcher: FlowWatcher<ThemePreference> = FlowWatcher(theme)

    init {
        startSync()
        resolveStartRoute()
        returnToWelcomeOnceErased()
    }

    private fun resolveStartRoute() {
        viewModelScope.launch {
            val hasProfile = observeProfile().first() != null
            navigator.replaceAll(if (hasProfile) Route.Groups else Route.Welcome)
            uiState.value = AppUiState.READY
            returnToProfileOnBrowserSignIn()
        }
    }

    /*
     * Why: the web reloads the page on a browser callback, so the app starts again on the groups. With a
     * profile and no recovery code sheet asking for it, the link was started from the profile, which
     * comes back to show it under way and how it ends.
     */
    private fun returnToProfileOnBrowserSignIn() {
        observeReturningBrowserSignIn()
            .filterNotNull()
            .onEach {
                val isOnGroups = navigator.backStack.value.lastOrNull() == Route.Groups
                if (isOnGroups &&
                    !observeAccount().first().recoveryCodePrompt.isShown
                ) {
                    navigator.navigate(Route.Profile)
                }
            }.launchIn(viewModelScope)
    }

    // Why: signing out, now or once synced, erases the profile wherever the app is, and only the welcome is left.
    private fun returnToWelcomeOnceErased() {
        observeProfile()
            .map { profile -> profile != null }
            .runningFold(false to false) { (_, had), has -> had to has }
            .drop(1)
            .onEach { (had, has) -> if (had && !has) navigator.replaceAll(Route.Welcome) }
            .launchIn(viewModelScope)
    }
}
