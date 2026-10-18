@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.recoverycode.presentation.model.RecoveryCodeUiAction
import com.quare.blitzsplit.feature.recoverycode.presentation.model.RecoveryCodeUiEvent
import com.quare.blitzsplit.feature.recoverycode.presentation.viewmodel.RecoveryCodeViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.koin.mp.KoinPlatform

class JsRecoveryCodeState(
    val formattedCode: String?,
    val isPromptDue: Boolean,
    val areAccountButtonsEnabled: Boolean,
    val connectingProvider: String?,
    val hasSignInFailed: Boolean,
    val isConnectExpanded: Boolean,
)

// Why: on the web "save to the password manager" becomes downloading the code as a file.
class JsRecoveryCodeAction(
    val type: String,
    val formattedCode: String,
)

class RecoveryCodePresenter {
    private val holder = ViewModelHolder(RecoveryCodeViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsRecoveryCodeState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsRecoveryCodeState(
                formattedCode = state.formattedCode,
                isPromptDue = state.isPromptDue,
                areAccountButtonsEnabled = state.areAccountButtonsEnabled,
                connectingProvider = state.connectingProvider?.code,
                hasSignInFailed = state.hasSignInFailed,
                isConnectExpanded = state.isConnectExpanded,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeActions(onAction: (JsRecoveryCodeAction) -> Unit): JsWatch {
        val actions = viewModel.uiAction.mapNotNull { action ->
            when (action) {
                is RecoveryCodeUiAction.CopyToClipboard -> JsRecoveryCodeAction("copy", action.formattedCode)
                is RecoveryCodeUiAction.SaveToPasswordManager -> JsRecoveryCodeAction("download", action.formattedCode)
                is RecoveryCodeUiAction.LaunchNativeSignIn -> null
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun copy() {
        viewModel.onEvent(RecoveryCodeUiEvent.OnCopyClicked)
    }

    fun download() {
        viewModel.onEvent(RecoveryCodeUiEvent.OnSaveToPasswordManagerClicked)
    }

    fun connectInstead() {
        viewModel.onEvent(RecoveryCodeUiEvent.OnConnectInsteadClicked)
    }

    fun clickProvider(providerCode: String) {
        viewModel.onEvent(RecoveryCodeUiEvent.OnProviderClicked(providerCode.toAccountProvider()))
    }

    fun remindLater() {
        viewModel.onEvent(RecoveryCodeUiEvent.OnRemindLaterClicked)
    }

    fun saved() {
        viewModel.onEvent(RecoveryCodeUiEvent.OnSavedClicked)
    }

    fun dispose() {
        holder.clear()
    }
}
