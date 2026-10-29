package com.quare.blitzsplit.feature.restore.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.RestoreWithRecoveryCode
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.core.sync.signin.ProviderSignIn
import com.quare.blitzsplit.core.sync.signin.SignInStep
import com.quare.blitzsplit.core.utils.RecoveryCodeFormat
import com.quare.blitzsplit.feature.restore.presentation.mapper.toRecoveryCodeFromQr
import com.quare.blitzsplit.feature.restore.presentation.mapper.toRestoreError
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreError
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiAction
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiEvent
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class RestoreViewModel(
    private val providerSignIn: ProviderSignIn,
    private val restoreWithRecoveryCode: RestoreWithRecoveryCode,
    private val navigator: Navigator,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<RestoreUiState>
        field = MutableStateFlow(
            RestoreUiState(
                code = "",
                isCodeComplete = false,
                isOnline = true,
                isRestoring = false,
                connectingProvider = null,
                error = null,
            ),
        )

    val uiAction: SharedFlow<RestoreUiAction>
        field = MutableSharedFlow<RestoreUiAction>()

    val stateWatcher: FlowWatcher<RestoreUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<RestoreUiAction> = FlowWatcher(uiAction)

    init {
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
        providerSignIn
            .observeBrowserSignIn(
                isShown = { navigator.backStack.value.lastOrNull() == Route.Restore },
                onStep = ::handleSignInStep,
            ).launchIn(viewModelScope)
    }

    fun onEvent(event: RestoreUiEvent) {
        when (event) {
            is RestoreUiEvent.OnCodeChanged -> updateCode(event.code)
            RestoreUiEvent.OnRestoreClicked -> restore()
            RestoreUiEvent.OnScanQrClicked -> emitAction(RestoreUiAction.OpenQrScanner)
            is RestoreUiEvent.OnQrScanned -> handleQrScanned(event.content)
            is RestoreUiEvent.OnProviderClicked -> handleProviderClicked(event.provider)
            is RestoreUiEvent.OnIdTokenReceived -> handleIdToken(event)
            RestoreUiEvent.OnNativeSignInFailed -> finishConnecting(error = RestoreError.SIGN_IN_FAILED)
            RestoreUiEvent.OnNativeSignInCancelled -> finishConnecting(error = null)
            RestoreUiEvent.OnStartFreshClicked -> navigator.navigate(Route.ProfileSetup)
            RestoreUiEvent.OnBackClicked -> navigator.navigateBack()
            RestoreUiEvent.OnErrorDismissed -> uiState.update { it.copy(error = null) }
        }
    }

    private fun updateCode(code: String) {
        uiState.update {
            it.copy(
                code = code,
                isCodeComplete = RecoveryCodeFormat.normalize(code) != null,
                error = null,
            )
        }
    }

    private fun handleQrScanned(content: String) {
        updateCode(content.toRecoveryCodeFromQr())
        restore()
    }

    private fun restore() {
        val state = uiState.value
        if (!state.canRestore) return
        uiState.update {
            it.copy(
                isRestoring = true,
                error = null,
            )
        }
        viewModelScope.launch {
            providerSignIn
                .signInDirectly { restoreWithRecoveryCode(state.code) }
                .onSuccess(::handleSignInStep)
                .onFailure { throwable -> uiState.update { it.copy(error = throwable.toRestoreError()) } }
            uiState.update { it.copy(isRestoring = false) }
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
                .onFailure { finishConnecting(error = RestoreError.SIGN_IN_FAILED) }
        }
    }

    private suspend fun launchNativeSignIn(provider: AccountProvider) {
        uiAction.emit(RestoreUiAction.LaunchNativeSignIn(provider))
    }

    private fun handleIdToken(event: RestoreUiEvent.OnIdTokenReceived) {
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

            is SignInStep.Ended -> finishConnecting(error = RestoreError.SIGN_IN_FAILED.takeIf { step.hasFailed })
        }
    }

    private fun finishConnecting(error: RestoreError?) {
        uiState.update {
            it.copy(
                connectingProvider = null,
                error = error,
            )
        }
    }

    private fun emitAction(action: RestoreUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }
}
