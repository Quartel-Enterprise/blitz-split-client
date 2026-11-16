@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiAction
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiEvent
import com.quare.blitzsplit.feature.restore.presentation.viewmodel.RestoreViewModel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

class JsRestoreWord(
    val text: String,
    val isDeadEnd: Boolean,
)

class JsRestoreState(
    val words: Array<JsRestoreWord>,
    // Why: -1 while no field has the focus.
    val focusedWordIndex: Int,
    val suggestions: Array<String>,
    val canRestore: Boolean,
    val isRestoring: Boolean,
    val areAccountButtonsEnabled: Boolean,
    val showsOfflineNotice: Boolean,
    val connectingProvider: String?,
    val error: String?,
)

class RestorePresenter {
    private val holder = ViewModelHolder(RestoreViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsRestoreState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsRestoreState(
                words = state.words.map { JsRestoreWord(it.text, it.isDeadEnd) }.toTypedArray(),
                focusedWordIndex = state.focusedWordIndex ?: -1,
                suggestions = state.suggestions.toTypedArray(),
                canRestore = state.canRestore,
                isRestoring = state.isRestoring,
                areAccountButtonsEnabled = state.areAccountButtonsEnabled,
                showsOfflineNotice = state.showsOfflineNotice,
                connectingProvider = state.connectingProvider?.code,
                error = state.error?.name,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeOpenQrScanner(onOpen: (Boolean) -> Unit): JsWatch {
        val opens = viewModel.uiAction.filterIsInstance<RestoreUiAction.OpenQrScanner>().map { true }
        val watch = FlowWatcher(opens).observe(onOpen)
        return JsWatch(watch::cancel)
    }

    fun observeFocusWord(onFocus: (Int) -> Unit): JsWatch {
        val focuses = viewModel.uiAction.filterIsInstance<RestoreUiAction.FocusWord>().map { it.index }
        val watch = FlowWatcher(focuses).observe(onFocus)
        return JsWatch(watch::cancel)
    }

    fun changeWord(
        index: Int,
        text: String,
    ) {
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(index, text))
    }

    fun focusWord(index: Int) {
        viewModel.onEvent(RestoreUiEvent.OnWordFocused(index))
    }

    fun pickSuggestion(word: String) {
        viewModel.onEvent(RestoreUiEvent.OnSuggestionClicked(word))
    }

    fun restore() {
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)
    }

    fun scanQr() {
        viewModel.onEvent(RestoreUiEvent.OnScanQrClicked)
    }

    fun qrScanned(content: String) {
        viewModel.onEvent(RestoreUiEvent.OnQrScanned(content))
    }

    fun clickProvider(providerCode: String) {
        viewModel.onEvent(RestoreUiEvent.OnProviderClicked(providerCode.toAccountProvider()))
    }

    fun startFresh() {
        viewModel.onEvent(RestoreUiEvent.OnStartFreshClicked)
    }

    fun back() {
        viewModel.onEvent(RestoreUiEvent.OnBackClicked)
    }

    fun dismissError() {
        viewModel.onEvent(RestoreUiEvent.OnErrorDismissed)
    }

    fun dispose() {
        holder.clear()
    }
}
