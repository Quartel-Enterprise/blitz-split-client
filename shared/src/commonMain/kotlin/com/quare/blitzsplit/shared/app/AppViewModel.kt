package com.quare.blitzsplit.shared.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveBrowserSignInReturningToProfile
import com.quare.blitzsplit.core.activity.domain.usecase.MarkActivityRead
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillLink
import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.PushLink
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.openHomeTab
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveThemePreference
import com.quare.blitzsplit.core.sync.domain.usecase.StartSync
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
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
    private val markActivityRead: MarkActivityRead,
    private val observeGroup: ObserveGroup,
    private val syncNow: SyncNow,
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
            navigator.replaceAll(if (hasProfile) Route.Home else Route.Welcome)
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
     * Why: a browser callback that starts the app again (the web reloads the page) lands on Início,
     * so a link left from the profile goes back there to show it under way and how it ends.
     */
    private fun returnToProfileOnBrowserSignIn() {
        observeBrowserSignInReturningToProfile()
            .filterNotNull()
            .onEach {
                uiState.first { it == AppUiState.READY }
                if (navigator.backStack.value.lastOrNull() == Route.Home) navigator.navigate(Route.Profile)
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
                PushLink.of(url)?.let { link -> return@onEach openPushLink(link) }
                SharedBillLink.codeOf(url)?.let { code -> return@onEach openSharedBill(code) }
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

    // Why: a bill's link (7.3h) opens its page as an invite does, the same one coming back to the top.
    private fun openSharedBill(code: String) {
        val openIndex = navigator.backStack.value.indexOfFirst { it is Route.SharedBill && it.code == code }
        if (openIndex < 0) navigator.navigate(Route.SharedBill(code)) else navigator.popTo(openIndex + 1)
    }

    /*
     * Why: a push notification's tap opens its tab, then what it is about once its group is on the device, after a
     * sync when it isn't yet (a group the person was just added to), unless the person moved on meanwhile. The event
     * is read, as its tap in Atividade makes it. Without a profile there is no home, and nothing opens.
     */
    private suspend fun openPushLink(link: PushLink) {
        link.eventId?.let { eventId -> viewModelScope.launch { markActivityRead(eventId) } }
        if (Route.Home !in navigator.backStack.value) return
        navigator.openHomeTab(link.homeTab)
        val groupId = link.groupId ?: return
        val tabTop = navigator.backStack.value
        if (observeGroup(groupId).first() == null) {
            syncNow()
            if (navigator.backStack.value != tabTop || observeGroup(groupId).first() == null) return
        }
        link.routesAbove.forEach(navigator::navigate)
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
