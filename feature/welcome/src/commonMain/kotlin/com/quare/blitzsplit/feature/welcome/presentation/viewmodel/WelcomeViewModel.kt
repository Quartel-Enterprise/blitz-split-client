package com.quare.blitzsplit.feature.welcome.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.account.domain.usecase.ConnectWithBrowser
import com.quare.blitzsplit.core.account.domain.usecase.ConnectWithIdToken
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.core.sync.domain.usecase.CompleteSignIn
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeError
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiAction
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiEvent
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class WelcomeViewModel(
    private val nativeSignIn: NativeSignIn,
    private val connectWithIdToken: ConnectWithIdToken,
    private val connectWithBrowser: ConnectWithBrowser,
    private val observeAccountStatus: ObserveAccountStatus,
    private val completeSignIn: CompleteSignIn,
    private val connectivityObserver: NetworkConnectivityObserver,
    private val navigator: Navigator,
) : ViewModel() {
    val uiState: StateFlow<WelcomeUiState>
        field = MutableStateFlow(WelcomeUiState(isOnline = true, connectingProvider = null, error = null))

    val uiAction: SharedFlow<WelcomeUiAction>
        field = MutableSharedFlow<WelcomeUiAction>()

    /*
     * Why: the status observer leaves alone a sign in this screen completes itself, both while it runs
     * and for the Connected it produced, which Room may deliver once it has finished.
     */
    private var isSigningInDirectly = false
    private var directlyCompletedStatus: AccountStatus? = null

    val stateWatcher: FlowWatcher<WelcomeUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<WelcomeUiAction> = FlowWatcher(uiAction)

    init {
        observeConnectivity()
        observeBrowserSignIn()
    }

    fun onEvent(event: WelcomeUiEvent) {
        when (event) {
            is WelcomeUiEvent.OnProviderClicked -> handleProviderClicked(event.provider)
            is WelcomeUiEvent.OnIdTokenReceived -> handleIdToken(event)
            WelcomeUiEvent.OnNativeSignInFailed -> finishConnecting(error = WelcomeError.SIGN_IN_FAILED)
            WelcomeUiEvent.OnNativeSignInCancelled -> finishConnecting(error = null)
            WelcomeUiEvent.OnUseWithoutAccountClicked -> navigator.navigate(Route.ProfileSetup)
            WelcomeUiEvent.OnUsedOnAnotherDeviceClicked -> navigator.navigate(Route.Restore)
            WelcomeUiEvent.OnErrorDismissed -> uiState.update { it.copy(error = null) }
        }
    }

    private fun observeConnectivity() {
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    /*
     * Why: a browser sign in finishes when the OAuth callback comes back, after this ViewModel was
     * recreated on the web (the page reloads), so a Google or Apple session seen while this screen
     * is on top completes it, with a button busy for the wait: the clicked one, or after a reload the
     * session's, which is its first identity. An ID token sign in completes on its own.
     */
    private fun observeBrowserSignIn() {
        observeAccountStatus()
            .distinctUntilChanged()
            .filterIsInstance<AccountStatus.Connected>()
            .filter { status ->
                !isSigningInDirectly &&
                    status != directlyCompletedStatus &&
                    navigator.backStack.value.lastOrNull() == Route.Welcome
            }.onEach { status ->
                uiState.update { it.copy(connectingProvider = it.connectingProvider ?: status.provider, error = null) }
                finishSignIn()
            }.launchIn(viewModelScope)
    }

    private fun handleProviderClicked(provider: AccountProvider) {
        if (!uiState.value.areAccountButtonsEnabled) return
        uiState.update { it.copy(connectingProvider = provider, error = null) }
        viewModelScope.launch {
            if (provider in nativeSignIn.providers) {
                uiAction.emit(WelcomeUiAction.LaunchNativeSignIn(provider))
            } else {
                connectWithBrowser(provider).onFailure { finishConnecting(error = WelcomeError.SIGN_IN_FAILED) }
            }
        }
    }

    private fun handleIdToken(event: WelcomeUiEvent.OnIdTokenReceived) {
        isSigningInDirectly = true
        viewModelScope.launch {
            connectWithIdToken(
                provider = event.provider,
                idToken = event.idToken,
                rawNonce = event.rawNonce,
                fullName = event.fullName,
            ).onSuccess { finishDirectSignIn() }
                .onFailure { finishConnecting(error = WelcomeError.SIGN_IN_FAILED) }
            isSigningInDirectly = false
        }
    }

    private suspend fun finishDirectSignIn() {
        directlyCompletedStatus = observeAccountStatus().first()
        finishSignIn()
    }

    private suspend fun finishSignIn() {
        completeSignIn()
            .onSuccess { outcome ->
                finishConnecting(error = null)
                when (outcome) {
                    SignInOutcome.PROFILE_RESTORED -> navigator.replaceAll(Route.Groups)
                    SignInOutcome.NEEDS_PROFILE -> navigator.navigate(Route.ProfileSetup)
                }
            }.onFailure { finishConnecting(error = WelcomeError.SIGN_IN_FAILED) }
    }

    private fun finishConnecting(error: WelcomeError?) {
        uiState.update { it.copy(connectingProvider = null, error = error) }
    }
}
