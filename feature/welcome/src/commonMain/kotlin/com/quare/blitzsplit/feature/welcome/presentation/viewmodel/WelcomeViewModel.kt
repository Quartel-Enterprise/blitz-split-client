package com.quare.blitzsplit.feature.welcome.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.core.sync.signin.ProviderSignIn
import com.quare.blitzsplit.core.sync.signin.SignInStep
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
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class WelcomeViewModel(
    private val providerSignIn: ProviderSignIn,
    private val navigator: Navigator,
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

    init {
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
        providerSignIn
            .observeBrowserSignIn(
                isShown = { navigator.backStack.value.lastOrNull() == Route.Welcome },
                onStep = ::handleSignInStep,
            ).launchIn(viewModelScope)
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

    private fun handleProviderClicked(provider: AccountProvider) {
        if (!uiState.value.areAccountButtonsEnabled) return
        uiState.update {
            it.copy(
                connectingProvider = provider,
                error = null,
            )
        }
        viewModelScope.launch {
            providerSignIn
                .signIn(provider, ::launchNativeSignIn)
                .onFailure { finishConnecting(error = WelcomeError.SIGN_IN_FAILED) }
        }
    }

    private suspend fun launchNativeSignIn(provider: AccountProvider) {
        uiAction.emit(WelcomeUiAction.LaunchNativeSignIn(provider))
    }

    private fun handleIdToken(event: WelcomeUiEvent.OnIdTokenReceived) {
        viewModelScope.launch {
            val step = providerSignIn.onIdTokenReceived(
                provider = event.provider,
                idToken = event.idToken,
                rawNonce = event.rawNonce,
                fullName = event.fullName,
                authorizationCode = event.authorizationCode,
            )
            handleSignInStep(step)
        }
    }

    private fun handleSignInStep(step: SignInStep) {
        when (step) {
            is SignInStep.Started -> uiState.update {
                it.copy(
                    connectingProvider = it.connectingProvider ?: step.provider,
                    error = null,
                )
            }

            is SignInStep.SignedIn -> {
                finishConnecting(error = null)
                when (step.outcome) {
                    SignInOutcome.PROFILE_RESTORED -> navigator.replaceAll(Route.Groups)
                    SignInOutcome.NEEDS_PROFILE -> navigator.navigate(Route.ProfileSetup)
                }
            }

            is SignInStep.Ended -> finishConnecting(error = WelcomeError.SIGN_IN_FAILED.takeIf { step.hasFailed })
        }
    }

    private fun finishConnecting(error: WelcomeError?) {
        uiState.update {
            it.copy(
                connectingProvider = null,
                error = error,
            )
        }
    }
}
