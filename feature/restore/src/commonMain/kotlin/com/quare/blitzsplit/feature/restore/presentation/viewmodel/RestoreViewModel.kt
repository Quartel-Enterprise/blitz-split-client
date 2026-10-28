package com.quare.blitzsplit.feature.restore.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.account.domain.usecase.ConnectWithBrowser
import com.quare.blitzsplit.core.account.domain.usecase.ConnectWithIdToken
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.account.domain.usecase.ObserveReturningBrowserSignIn
import com.quare.blitzsplit.core.account.domain.usecase.RestoreWithRecoveryCode
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.core.sync.domain.usecase.CompleteSignIn
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class RestoreViewModel(
    private val nativeSignIn: NativeSignIn,
    private val restoreWithRecoveryCode: RestoreWithRecoveryCode,
    private val connectWithIdToken: ConnectWithIdToken,
    private val connectWithBrowser: ConnectWithBrowser,
    private val observeAccountStatus: ObserveAccountStatus,
    private val observeReturningBrowserSignIn: ObserveReturningBrowserSignIn,
    private val completeSignIn: CompleteSignIn,
    private val connectivityObserver: NetworkConnectivityObserver,
    private val navigator: Navigator,
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

    /*
     * Why: the status observer leaves alone a sign in this screen completes itself, both while it runs
     * and for the Connected it produced, which Room may deliver once it has finished.
     */
    private var isSigningInDirectly = false
    private var directlyCompletedStatus: AccountStatus? = null
    private var returningProvider: AccountProvider? = null

    val stateWatcher: FlowWatcher<RestoreUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<RestoreUiAction> = FlowWatcher(uiAction)

    init {
        observeConnectivity()
        observeBrowserSignIn()
        observeReturningSignIn()
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

    private fun observeConnectivity() {
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    private fun observeBrowserSignIn() {
        observeAccountStatus()
            .distinctUntilChanged()
            .filterIsInstance<AccountStatus.Connected>()
            .filter { status ->
                !isSigningInDirectly &&
                    status != directlyCompletedStatus &&
                    navigator.backStack.value.lastOrNull() == Route.Restore
            }.onEach { status ->
                uiState.update { it.copy(connectingProvider = it.connectingProvider ?: status.provider, error = null) }
                finishSignIn()
            }.launchIn(viewModelScope)
    }

    private fun updateCode(code: String) {
        uiState.update {
            it.copy(code = code, isCodeComplete = RecoveryCodeFormat.normalize(code) != null, error = null)
        }
    }

    private fun handleQrScanned(content: String) {
        updateCode(content.toRecoveryCodeFromQr())
        restore()
    }

    private fun restore() {
        val state = uiState.value
        if (!state.canRestore) return
        uiState.update { it.copy(isRestoring = true, error = null) }
        isSigningInDirectly = true
        viewModelScope.launch {
            restoreWithRecoveryCode(state.code)
                .onSuccess { finishDirectSignIn() }
                .onFailure { throwable -> uiState.update { it.copy(error = throwable.toRestoreError()) } }
            uiState.update { it.copy(isRestoring = false) }
            isSigningInDirectly = false
        }
    }

    /*
     * Why: on the web the callback reloads the page, so the sign in it brings back shows as under way
     * from the first frame, and as failed if it ends without the account connecting.
     */
    private fun observeReturningSignIn() {
        observeReturningBrowserSignIn()
            .filter { navigator.backStack.value.lastOrNull() == Route.Restore }
            .onEach { provider ->
                if (provider != null) {
                    returningProvider = provider
                    uiState.update { it.copy(connectingProvider = it.connectingProvider ?: provider, error = null) }
                } else if (returningProvider != null) {
                    returningProvider = null
                    val isConnected = observeAccountStatus().first() is AccountStatus.Connected
                    if (!isConnected) finishConnecting(error = RestoreError.SIGN_IN_FAILED)
                }
            }.launchIn(viewModelScope)
    }

    private fun handleProviderClicked(provider: AccountProvider) {
        if (!uiState.value.areAccountButtonsEnabled) return
        uiState.update { it.copy(connectingProvider = provider, error = null) }
        viewModelScope.launch {
            if (provider in nativeSignIn.providers) {
                uiAction.emit(RestoreUiAction.LaunchNativeSignIn(provider))
            } else {
                connectWithBrowser(provider).onFailure { finishConnecting(error = RestoreError.SIGN_IN_FAILED) }
            }
        }
    }

    private fun handleIdToken(event: RestoreUiEvent.OnIdTokenReceived) {
        isSigningInDirectly = true
        viewModelScope.launch {
            connectWithIdToken(
                provider = event.provider,
                idToken = event.idToken,
                rawNonce = event.rawNonce,
                fullName = event.fullName,
            ).onSuccess { finishDirectSignIn() }
                .onFailure { finishConnecting(error = RestoreError.SIGN_IN_FAILED) }
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
            }.onFailure { finishConnecting(error = RestoreError.SIGN_IN_FAILED) }
    }

    private fun finishConnecting(error: RestoreError?) {
        uiState.update { it.copy(connectingProvider = null, error = error) }
    }

    private fun emitAction(action: RestoreUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }
}
