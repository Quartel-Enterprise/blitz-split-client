package com.quare.blitzsplit.feature.recoverycode.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.account.domain.usecase.UpdateRecoveryCodePrompt
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.sync.connect.ProviderConnector
import com.quare.blitzsplit.core.utils.RecoveryCodeFormat
import com.quare.blitzsplit.feature.recoverycode.presentation.model.RecoveryCodeUiAction
import com.quare.blitzsplit.feature.recoverycode.presentation.model.RecoveryCodeUiEvent
import com.quare.blitzsplit.feature.recoverycode.presentation.model.RecoveryCodeUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class RecoveryCodeViewModel(
    private val updateRecoveryCodePrompt: UpdateRecoveryCodePrompt,
    private val providerConnector: ProviderConnector,
    observeAccount: ObserveAccount,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<RecoveryCodeUiState>
        field = MutableStateFlow(
            RecoveryCodeUiState(
                formattedCode = null,
                isPromptDue = false,
                isOnline = true,
                connectingProvider = null,
                hasSignInFailed = false,
                isConnectExpanded = false,
            ),
        )

    val uiAction: SharedFlow<RecoveryCodeUiAction>
        field = MutableSharedFlow<RecoveryCodeUiAction>()

    private var prompt = RecoveryCodePrompt.NOT_DUE

    val stateWatcher: FlowWatcher<RecoveryCodeUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<RecoveryCodeUiAction> = FlowWatcher(uiAction)

    init {
        observeAccount()
            .onEach { account ->
                prompt = account.recoveryCodePrompt
                uiState.update {
                    it.copy(
                        formattedCode = account.recoveryCode?.let(RecoveryCodeFormat::format),
                        isPromptDue = account.recoveryCodePrompt.isShown,
                    )
                }
            }.launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
        providerConnector.connection
            .onEach { connection ->
                uiState.update {
                    it.copy(
                        connectingProvider = connection.connectingProvider,
                        hasSignInFailed = connection.hasFailed,
                        isConnectExpanded = connection.isExpanded,
                    )
                }
            }.launchIn(viewModelScope)
        // Why: after the web reload the sheet shows again until the account mirror settles the prompt.
        viewModelScope.launch {
            providerConnector.observeBrowserSignIn { account -> account.recoveryCodePrompt == RecoveryCodePrompt.SAVED }
        }
    }

    fun onEvent(event: RecoveryCodeUiEvent) {
        when (event) {
            RecoveryCodeUiEvent.OnCopyClicked -> emitWithCode(RecoveryCodeUiAction::CopyToClipboard)

            RecoveryCodeUiEvent.OnSaveToPasswordManagerClicked -> emitWithCode(
                RecoveryCodeUiAction::SaveToPasswordManager,
            )

            RecoveryCodeUiEvent.OnConnectInsteadClicked -> providerConnector.expand()

            is RecoveryCodeUiEvent.OnProviderClicked -> handleProviderClicked(event.provider)

            is RecoveryCodeUiEvent.OnIdTokenReceived -> handleIdToken(event)

            RecoveryCodeUiEvent.OnNativeSignInFailed -> providerConnector.finishConnecting(hasFailed = true)

            RecoveryCodeUiEvent.OnNativeSignInCancelled -> providerConnector.finishConnecting(hasFailed = false)

            RecoveryCodeUiEvent.OnRemindLaterClicked -> remindLater()

            RecoveryCodeUiEvent.OnSavedClicked -> updatePrompt(RecoveryCodePrompt.SAVED)
        }
    }

    private fun emitWithCode(action: (String) -> RecoveryCodeUiAction) {
        val code = uiState.value.formattedCode ?: return
        viewModelScope.launch { uiAction.emit(action(code)) }
    }

    private fun handleProviderClicked(provider: AccountProvider) {
        if (!uiState.value.areAccountButtonsEnabled) return
        viewModelScope.launch { providerConnector.connect(provider, ::launchNativeSignIn) }
    }

    private suspend fun launchNativeSignIn(provider: AccountProvider) {
        uiAction.emit(RecoveryCodeUiAction.LaunchNativeSignIn(provider))
    }

    // Why: once Google or Apple is linked the account mirror settles the prompt, which hides the sheet.
    private fun handleIdToken(event: RecoveryCodeUiEvent.OnIdTokenReceived) {
        viewModelScope.launch {
            providerConnector.onIdTokenReceived(
                provider = event.provider,
                idToken = event.idToken,
                rawNonce = event.rawNonce,
                fullName = event.fullName,
            )
        }
    }

    // Why: a code already saved stays saved; only one never saved comes back on the next launch.
    private fun remindLater() {
        updatePrompt(if (prompt == RecoveryCodePrompt.REVIEW) RecoveryCodePrompt.SAVED else RecoveryCodePrompt.SNOOZED)
    }

    private fun updatePrompt(prompt: RecoveryCodePrompt) {
        viewModelScope.launch { updateRecoveryCodePrompt(prompt) }
    }
}
