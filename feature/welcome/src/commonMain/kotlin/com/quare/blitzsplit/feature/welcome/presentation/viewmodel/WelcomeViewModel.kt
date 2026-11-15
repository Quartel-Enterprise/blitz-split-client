package com.quare.blitzsplit.feature.welcome.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.DismissExpiredAccountNotice
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.InactivityExpiry
import com.quare.blitzsplit.core.model.LegalPage
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.OpenLegalPage
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.signin.ProviderSignIn
import com.quare.blitzsplit.core.sync.signin.ProviderSignInController
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeError
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiAction
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiEvent
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class WelcomeViewModel(
    private val navigator: Navigator,
    private val openLegalPage: OpenLegalPage,
    private val dismissExpiredAccountNotice: DismissExpiredAccountNotice,
    observeAccount: ObserveAccount,
    providerSignIn: ProviderSignIn,
    connectivityObserver: NetworkConnectivityObserver,
    trackEvent: TrackEvent,
) : TrackedViewModel<WelcomeUiEvent>(trackEvent) {
    val uiState: StateFlow<WelcomeUiState>
        field = MutableStateFlow(
            WelcomeUiState(
                isOnline = true,
                connectingProvider = null,
                error = null,
                showsExpiredAccountNotice = false,
            ),
        )

    val uiAction: SharedFlow<WelcomeUiAction>
        field = MutableSharedFlow<WelcomeUiAction>()

    val stateWatcher: FlowWatcher<WelcomeUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<WelcomeUiAction> = FlowWatcher(uiAction)

    private val signIn = ProviderSignInController(
        providerSignIn = providerSignIn,
        navigator = navigator,
        scope = viewModelScope,
        connectingProvider = { uiState.value.connectingProvider },
        showConnecting = ::showConnecting,
        launchNativeSignIn = { provider -> uiAction.emit(WelcomeUiAction.LaunchNativeSignIn(provider)) },
    )

    init {
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
        observeAccount()
            .map { account -> account.inactivityExpiry == InactivityExpiry.NOTICE }
            .distinctUntilChanged()
            .onEach { isShown -> uiState.update { it.copy(showsExpiredAccountNotice = isShown) } }
            .launchIn(viewModelScope)
        signIn.observeBrowserSignIn(isShown = { navigator.backStack.value.lastOrNull() == Route.Welcome })
    }

    override fun handleEvent(event: WelcomeUiEvent) {
        when (event) {
            is WelcomeUiEvent.OnProviderClicked -> handleProviderClicked(event.provider)
            is WelcomeUiEvent.OnIdTokenReceived -> handleIdToken(event)
            WelcomeUiEvent.OnNativeSignInFailed -> signIn.onNativeSignInFailed()
            WelcomeUiEvent.OnNativeSignInCancelled -> signIn.onNativeSignInCancelled()
            WelcomeUiEvent.OnUseWithoutAccountClicked -> navigator.navigate(Route.ProfileSetup)
            WelcomeUiEvent.OnUsedOnAnotherDeviceClicked -> navigator.navigate(Route.Restore)
            WelcomeUiEvent.OnErrorDismissed -> uiState.update { it.copy(error = null) }
            WelcomeUiEvent.OnExpiredAccountNoticeDismissed -> handleExpiredAccountNoticeDismissed()
            WelcomeUiEvent.OnPrivacyPolicyClicked -> openLegalPage(LegalPage.PRIVACY_POLICY, ::openUrl)
            WelcomeUiEvent.OnTermsClicked -> openLegalPage(LegalPage.TERMS_OF_USE, ::openUrl)
        }
    }

    /*
     * Why: hidden at once, not only once the account's write comes back, so a UI whose dialog reads the state
     * (SwiftUI's alert binding, Radix's open) never shows it again in between.
     */
    private fun handleExpiredAccountNoticeDismissed() {
        uiState.update { it.copy(showsExpiredAccountNotice = false) }
        viewModelScope.launch { dismissExpiredAccountNotice() }
    }

    private fun openUrl(url: String) {
        viewModelScope.launch { uiAction.emit(WelcomeUiAction.OpenUrl(url)) }
    }

    private fun handleProviderClicked(provider: AccountProvider) {
        if (!uiState.value.areAccountButtonsEnabled) return
        signIn.signIn(provider)
    }

    private fun handleIdToken(event: WelcomeUiEvent.OnIdTokenReceived) {
        signIn.onIdTokenReceived(
            provider = event.provider,
            idToken = event.idToken,
            rawNonce = event.rawNonce,
            fullName = event.fullName,
            authorizationCode = event.authorizationCode,
        )
    }

    private fun showConnecting(
        provider: AccountProvider?,
        hasFailed: Boolean,
    ) {
        uiState.update {
            it.copy(
                connectingProvider = provider,
                error = WelcomeError.SIGN_IN_FAILED.takeIf { hasFailed },
            )
        }
    }
}
