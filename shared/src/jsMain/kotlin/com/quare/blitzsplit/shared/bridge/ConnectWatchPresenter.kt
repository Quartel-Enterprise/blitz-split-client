@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.connectwatch.presentation.model.ConnectWatchUiEvent
import com.quare.blitzsplit.feature.connectwatch.presentation.viewmodel.ConnectWatchViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

class JsConnectWatchState(
    // Why: up to six letters and digits, upper case, without the space the page draws between the groups of three.
    val code: String,
    val codeLength: Int,
    val isConnecting: Boolean,
    /*
     * Why: ConnectWatchError's name in lower case ("invalid_or_expired", "too_many_attempts", "needs_account",
     * "failed"), null without one.
     */
    val error: String?,
    val isConnected: Boolean,
    val canConnect: Boolean,
    val isCodeWrong: Boolean,
    val showsOfflineNotice: Boolean,
)

// Why: Profile › Connect watch (15.14), the code a watch shows when its phone is out of reach.
class ConnectWatchPresenter {
    private val holder = ViewModelHolder(ConnectWatchViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsConnectWatchState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsConnectWatchState(
                code = state.code,
                codeLength = state.codeLength,
                isConnecting = state.isConnecting,
                error = state.error?.name?.lowercase(),
                isConnected = state.isConnected,
                canConnect = state.canConnect,
                isCodeWrong = state.isCodeWrong,
                showsOfflineNotice = state.showsOfflineNotice,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun changeCode(text: String) {
        viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged(text))
    }

    fun connect() {
        viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)
    }

    fun close() {
        viewModel.onEvent(ConnectWatchUiEvent.OnCloseClicked)
    }

    fun dispose() {
        holder.clear()
    }
}
