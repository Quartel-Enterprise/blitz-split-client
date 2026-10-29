@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.deleteaccount.presentation.model.DeleteAccountUiAction
import com.quare.blitzsplit.feature.deleteaccount.presentation.model.DeleteAccountUiEvent
import com.quare.blitzsplit.feature.deleteaccount.presentation.viewmodel.DeleteAccountViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

class JsDeleteAccountState(
    // Why: "google" or "apple", null once the account is gone.
    val accountProvider: String?,
    val hasConfirmed: Boolean,
    val isDeleting: Boolean,
    val hasFailed: Boolean,
    val showsOfflineNotice: Boolean,
    val isConfirmEnabled: Boolean,
    val canDelete: Boolean,
)

class JsDeleteAccountAction(
    // Why: "export" with the JSON to save.
    val type: String,
    val value: String,
)

class DeleteAccountPresenter {
    private val holder = ViewModelHolder(DeleteAccountViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsDeleteAccountState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsDeleteAccountState(
                accountProvider = state.accountProvider?.code,
                hasConfirmed = state.hasConfirmed,
                isDeleting = state.isDeleting,
                hasFailed = state.hasFailed,
                showsOfflineNotice = state.showsOfflineNotice,
                isConfirmEnabled = state.isConfirmEnabled,
                canDelete = state.canDelete,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeActions(onAction: (JsDeleteAccountAction) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                is DeleteAccountUiAction.SaveExport -> JsDeleteAccountAction("export", action.json)
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(DeleteAccountUiEvent.OnBackClicked)
    }

    fun confirmChanged(isChecked: Boolean) {
        viewModel.onEvent(DeleteAccountUiEvent.OnConfirmChanged(isChecked))
    }

    fun delete() {
        viewModel.onEvent(DeleteAccountUiEvent.OnDeleteClicked)
    }

    fun export() {
        viewModel.onEvent(DeleteAccountUiEvent.OnExportClicked)
    }

    fun dispose() {
        holder.clear()
    }
}
