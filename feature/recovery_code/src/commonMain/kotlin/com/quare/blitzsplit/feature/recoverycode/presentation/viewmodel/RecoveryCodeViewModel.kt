package com.quare.blitzsplit.feature.recoverycode.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.account.domain.usecase.ConnectWithBrowser
import com.quare.blitzsplit.core.account.domain.usecase.ConnectWithIdToken
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.account.domain.usecase.ObserveReturningBrowserSignIn
import com.quare.blitzsplit.core.account.domain.usecase.UpdateRecoveryCodePrompt
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.sync.domain.usecase.CompleteSignIn
import com.quare.blitzsplit.core.utils.RecoveryCodeFormat
import com.quare.blitzsplit.feature.recoverycode.presentation.model.RecoveryCodeUiAction
import com.quare.blitzsplit.feature.recoverycode.presentation.model.RecoveryCodeUiEvent
import com.quare.blitzsplit.feature.recoverycode.presentation.model.RecoveryCodeUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class RecoveryCodeViewModel(
    private val nativeSignIn: NativeSignIn,
    private val updateRecoveryCodePrompt: UpdateRecoveryCodePrompt,
    private val connectWithIdToken: ConnectWithIdToken,
    private val connectWithBrowser: ConnectWithBrowser,
    private val completeSignIn: CompleteSignIn,
    private val observeAccount: ObserveAccount,
    private val observeReturningBrowserSignIn: ObserveReturningBrowserSignIn,
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

    private var returningProvider: AccountProvider? = null

    val stateWatcher: FlowWatcher<RecoveryCodeUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<RecoveryCodeUiAction> = FlowWatcher(uiAction)

    init {
        observeAccount()
            .onEach { account ->
                uiState.update {
                    it.copy(
                        formattedCode = account.recoveryCode?.let(RecoveryCodeFormat::format),
                        isPromptDue = account.recoveryCodePrompt == RecoveryCodePrompt.DUE,
                    )
                }
            }.launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
        observeReturningSignIn()
    }

    fun onEvent(event: RecoveryCodeUiEvent) {
        when (event) {
            RecoveryCodeUiEvent.OnCopyClicked -> emitWithCode(RecoveryCodeUiAction::CopyToClipboard)

            RecoveryCodeUiEvent.OnSaveToPasswordManagerClicked -> emitWithCode(
                RecoveryCodeUiAction::SaveToPasswordManager,
            )

            RecoveryCodeUiEvent.OnConnectInsteadClicked -> uiState.update { it.copy(isConnectExpanded = true) }

            is RecoveryCodeUiEvent.OnProviderClicked -> handleProviderClicked(event.provider)

            is RecoveryCodeUiEvent.OnIdTokenReceived -> handleIdToken(event)

            RecoveryCodeUiEvent.OnNativeSignInFailed -> finishConnecting(hasFailed = true)

            RecoveryCodeUiEvent.OnNativeSignInCancelled -> finishConnecting(hasFailed = false)

            RecoveryCodeUiEvent.OnRemindLaterClicked -> updatePrompt(RecoveryCodePrompt.SNOOZED)

            RecoveryCodeUiEvent.OnSavedClicked -> updatePrompt(RecoveryCodePrompt.SAVED)
        }
    }

    private fun emitWithCode(action: (String) -> RecoveryCodeUiAction) {
        val code = uiState.value.formattedCode ?: return
        viewModelScope.launch { uiAction.emit(action(code)) }
    }

    /*
     * Why: on the web the link's callback reloads the page and the sheet shows again until the account
     * mirror settles the prompt, so the link shows as under way meanwhile, and as failed if it ends
     * with the prompt still unsettled.
     */
    private fun observeReturningSignIn() {
        observeReturningBrowserSignIn()
            .onEach { provider ->
                if (provider != null) {
                    returningProvider = provider
                    uiState.update {
                        it.copy(
                            connectingProvider = it.connectingProvider ?: provider,
                            hasSignInFailed = false,
                            isConnectExpanded = true,
                        )
                    }
                } else if (returningProvider != null) {
                    returningProvider = null
                    finishConnecting(
                        hasFailed = observeAccount().first().recoveryCodePrompt != RecoveryCodePrompt.SAVED,
                    )
                }
            }.launchIn(viewModelScope)
    }

    private fun handleProviderClicked(provider: AccountProvider) {
        if (!uiState.value.areAccountButtonsEnabled) return
        uiState.update { it.copy(connectingProvider = provider, hasSignInFailed = false) }
        viewModelScope.launch {
            if (provider in nativeSignIn.providers) {
                uiAction.emit(RecoveryCodeUiAction.LaunchNativeSignIn(provider))
            } else {
                connectWithBrowser(provider).onFailure { finishConnecting(hasFailed = true) }
            }
        }
    }

    // Why: once Google or Apple is linked the account mirror settles the prompt, which hides the sheet.
    private fun handleIdToken(event: RecoveryCodeUiEvent.OnIdTokenReceived) {
        viewModelScope.launch {
            connectWithIdToken(
                provider = event.provider,
                idToken = event.idToken,
                rawNonce = event.rawNonce,
                fullName = event.fullName,
            ).onSuccess {
                completeSignIn()
                finishConnecting(hasFailed = false)
            }.onFailure { finishConnecting(hasFailed = true) }
        }
    }

    private fun finishConnecting(hasFailed: Boolean) {
        uiState.update { it.copy(connectingProvider = null, hasSignInFailed = hasFailed) }
    }

    private fun updatePrompt(prompt: RecoveryCodePrompt) {
        viewModelScope.launch { updateRecoveryCodePrompt(prompt) }
    }
}
