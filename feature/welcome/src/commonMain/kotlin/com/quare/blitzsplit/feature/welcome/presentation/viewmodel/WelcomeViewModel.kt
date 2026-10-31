package com.quare.blitzsplit.feature.welcome.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import software.quare.connectivity.NetworkConnectivityObserver

class WelcomeViewModel(
    private val navigator: Navigator,
    providerSignIn: ProviderSignIn,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<WelcomeUiState>
        field = MutableStateFlow(
            WelcomeUiState(
                isOnline = true,
                connectingProvider = null,
                error = null,
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
        signIn.observeBrowserSignIn(isShown = { navigator.backStack.value.lastOrNull() == Route.Welcome })
    }

    fun onEvent(event: WelcomeUiEvent) {
        when (event) {
            is WelcomeUiEvent.OnProviderClicked -> handleProviderClicked(event.provider)
            is WelcomeUiEvent.OnIdTokenReceived -> handleIdToken(event)
            WelcomeUiEvent.OnNativeSignInFailed -> signIn.onNativeSignInFailed()
            WelcomeUiEvent.OnNativeSignInCancelled -> signIn.onNativeSignInCancelled()
            WelcomeUiEvent.OnUseWithoutAccountClicked -> navigator.navigate(Route.ProfileSetup)
            WelcomeUiEvent.OnUsedOnAnotherDeviceClicked -> navigator.navigate(Route.Restore)
            WelcomeUiEvent.OnErrorDismissed -> uiState.update { it.copy(error = null) }
        }
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
