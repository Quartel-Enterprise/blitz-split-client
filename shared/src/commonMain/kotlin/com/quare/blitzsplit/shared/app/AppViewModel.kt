package com.quare.blitzsplit.shared.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveBrowserSignInReturningToProfile
import com.quare.blitzsplit.core.groups.domain.model.InviteLink
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AppViewModel(
    private val observeProfile: ObserveProfile,
    val navigator: Navigator,
    private val observeBrowserSignInReturningToProfile: ObserveBrowserSignInReturningToProfile,
    private val observeThemePreference: ObserveThemePreference,
    private val linkInbox: LinkInbox,
    startSync: StartSync,
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
        startSync()
        followTheme()
        resolveStartRoute()
        returnToProfileOnBrowserSignIn()
        returnToWelcomeOnceErased()
        openInviteLinks()
    }

    private fun resolveStartRoute() {
        viewModelScope.launch {
            val hasProfile = observeProfile().first() != null
            navigator.replaceAll(if (hasProfile) Route.Groups else Route.Welcome)
            isThemeKnown.await()
            uiState.value = AppUiState.READY
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
     * Why: a browser callback that starts the app again (the web reloads the page) lands on the groups,
     * so a link left from the profile goes back there to show it under way and how it ends.
     */
    private fun returnToProfileOnBrowserSignIn() {
        observeBrowserSignInReturningToProfile()
            .filterNotNull()
            .onEach {
                uiState.first { it == AppUiState.READY }
                if (navigator.backStack.value.lastOrNull() == Route.Groups) navigator.navigate(Route.Profile)
            }.launchIn(viewModelScope)
    }

    /*
     * Why: an invite link opens its invite over wherever the app is, once the first screen is in place, so back leaves
     * it for the groups, or the welcome without a profile. The same invite already open comes back to the top instead
     * of being pushed twice, which Navigation 3 can't hold since it keys each entry by its route.
     */
    private fun openInviteLinks() {
        linkInbox
            .observe()
            .onEach { url ->
                uiState.first { it == AppUiState.READY }
                linkInbox.take(url)
                val target = InviteLink.targetOf(url) ?: return@onEach
                val invite = Route.JoinInvite(
                    code = target.code,
                    memberCode = target.memberCode,
                    inviterCode = target.inviterCode,
                )
                // Why: the same link shared again by someone else is the same invite, whoever the sender is.
                val openIndex = navigator.backStack.value.indexOfFirst {
                    it is Route.JoinInvite && it.code == invite.code && it.memberCode == invite.memberCode
                }
                if (openIndex < 0) navigator.navigate(invite) else navigator.popTo(openIndex + 1)
            }.launchIn(viewModelScope)
    }

    // Why: signing out, now or once synced, erases the profile wherever the app is, and only the welcome is left.
    private fun returnToWelcomeOnceErased() {
        observeProfile()
            .map { profile -> profile != null }
            .distinctUntilChanged()
            .dropWhile { hasProfile -> !hasProfile }
            .filter { hasProfile -> !hasProfile }
            .onEach { navigator.replaceAll(Route.Welcome) }
            .launchIn(viewModelScope)
    }
}
