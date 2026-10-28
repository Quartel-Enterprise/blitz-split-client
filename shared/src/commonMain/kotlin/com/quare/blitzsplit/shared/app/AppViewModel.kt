package com.quare.blitzsplit.shared.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.account.domain.usecase.ObserveRelaunchingBrowserSignIn
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveThemePreference
import com.quare.blitzsplit.core.sync.domain.usecase.StartSync
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch

class AppViewModel(
    private val observeProfile: ObserveProfile,
    val navigator: Navigator,
    private val observeAccount: ObserveAccount,
    private val observeRelaunchingBrowserSignIn: ObserveRelaunchingBrowserSignIn,
    private val startSync: StartSync,
    private val observeThemePreference: ObserveThemePreference,
) : ViewModel() {
    val uiState: StateFlow<AppUiState>
        field = MutableStateFlow(AppUiState.LOADING)

    val theme: StateFlow<ThemePreference>
        field = MutableStateFlow(ThemePreference.SYSTEM)

    private val isThemeKnown = CompletableDeferred<Unit>()

    val stateWatcher: FlowWatcher<AppUiState> = FlowWatcher(uiState)
    val backStackWatcher: FlowWatcher<List<Route>> = FlowWatcher(navigator.backStack)
    val themeWatcher: FlowWatcher<ThemePreference> = FlowWatcher(theme)

    init {
        followTheme()
        start()
        returnToWelcomeOnceErased()
    }

    /*
     * Why: the prompt is read before the sync starts, which settles a snoozed or reviewed prompt and
     * mirrors a returning session as saved, so it still tells where a browser sign in was started.
     */
    private fun start() {
        viewModelScope.launch {
            val startPrompt = observeAccount().first().recoveryCodePrompt
            startSync()
            val hasProfile = observeProfile().first() != null
            navigator.replaceAll(if (hasProfile) Route.Groups else Route.Welcome)
            isThemeKnown.await()
            uiState.value = AppUiState.READY
            if (startPrompt != RecoveryCodePrompt.DUE) returnToProfileOnRelaunchingSignIn()
        }
    }

    // Why: the first frame waits for the chosen theme, so a Light or Dark one never flashes the system's.
    private fun followTheme() {
        observeThemePreference()
            .onEach { preference ->
                theme.value = preference
                isThemeKnown.complete(Unit)
            }.launchIn(viewModelScope)
    }

    /*
     * Why: a browser callback that starts the app again (the web reloads the page) lands on the groups.
     * Unless the recovery code sheet was asking for it there, the link was started from the profile,
     * which comes back to show it under way and how it ends.
     */
    private fun returnToProfileOnRelaunchingSignIn() {
        observeRelaunchingBrowserSignIn()
            .filterNotNull()
            .onEach {
                if (navigator.backStack.value.lastOrNull() == Route.Groups) navigator.navigate(Route.Profile)
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
