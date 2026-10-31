package com.quare.blitzsplit.feature.restore.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.RestoreWithRecoveryCode
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.sync.signin.ProviderSignIn
import com.quare.blitzsplit.core.sync.signin.ProviderSignInController
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
    private val restoreWithRecoveryCode: RestoreWithRecoveryCode,
    private val navigator: Navigator,
    providerSignIn: ProviderSignIn,
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

    private val signIn = ProviderSignInController(
        providerSignIn = providerSignIn,
        navigator = navigator,
        scope = viewModelScope,
        connectingProvider = { uiState.value.connectingProvider },
        showConnecting = ::showConnecting,
        launchNativeSignIn = { provider -> uiAction.emit(RestoreUiAction.LaunchNativeSignIn(provider)) },
    )

    init {
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
        signIn.observeBrowserSignIn(isShown = { navigator.backStack.value.lastOrNull() == Route.Restore })
    }

    fun onEvent(event: RestoreUiEvent) {
        when (event) {
            is RestoreUiEvent.OnCodeChanged -> updateCode(event.code)
            RestoreUiEvent.OnRestoreClicked -> restore()
            RestoreUiEvent.OnScanQrClicked -> emitAction(RestoreUiAction.OpenQrScanner)
            is RestoreUiEvent.OnQrScanned -> handleQrScanned(event.content)
            is RestoreUiEvent.OnProviderClicked -> handleProviderClicked(event.provider)
            is RestoreUiEvent.OnIdTokenReceived -> handleIdToken(event)
            RestoreUiEvent.OnNativeSignInFailed -> signIn.onNativeSignInFailed()
            RestoreUiEvent.OnNativeSignInCancelled -> signIn.onNativeSignInCancelled()
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
            signIn
                .signInDirectly { restoreWithRecoveryCode(state.code) }
                .onFailure { throwable -> uiState.update { it.copy(error = throwable.toRestoreError()) } }
            uiState.update { it.copy(isRestoring = false) }
        }
    }

    private fun handleProviderClicked(provider: AccountProvider) {
        if (!uiState.value.areAccountButtonsEnabled) return
        signIn.signIn(provider)
    }

    private fun handleIdToken(event: RestoreUiEvent.OnIdTokenReceived) {
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
                error = RestoreError.SIGN_IN_FAILED.takeIf { hasFailed },
            )
        }
    }

    private fun emitAction(action: RestoreUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }
}
