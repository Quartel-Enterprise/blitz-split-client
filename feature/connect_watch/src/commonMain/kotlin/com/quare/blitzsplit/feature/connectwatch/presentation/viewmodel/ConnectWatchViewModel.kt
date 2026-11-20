package com.quare.blitzsplit.feature.connectwatch.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.watch.domain.model.ExpiredWatchPairingException
import com.quare.blitzsplit.core.watch.domain.model.InvalidWatchPairingCodeException
import com.quare.blitzsplit.core.watch.domain.model.TooManyWatchPairingAttemptsException
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCodeFormat
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingNeedsAccountException
import com.quare.blitzsplit.core.watch.domain.usecase.ApproveWatchPairing
import com.quare.blitzsplit.feature.connectwatch.presentation.model.ConnectWatchError
import com.quare.blitzsplit.feature.connectwatch.presentation.model.ConnectWatchUiEvent
import com.quare.blitzsplit.feature.connectwatch.presentation.model.ConnectWatchUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

/*
 * Why: Profile › Connect watch (15.14): the code a watch shows when its phone is out of reach, typed on any device
 * with the account (phone, iPad, Mac, the web), as signing in on a TV.
 */
class ConnectWatchViewModel(
    private val navigator: Navigator,
    private val approveWatchPairing: ApproveWatchPairing,
    connectivityObserver: NetworkConnectivityObserver,
    trackEvent: TrackEvent,
) : TrackedViewModel<ConnectWatchUiEvent>(trackEvent) {
    val uiState: StateFlow<ConnectWatchUiState>
        field = MutableStateFlow(
            ConnectWatchUiState(
                code = "",
                isOnline = true,
                isConnecting = false,
                error = null,
                isConnected = false,
            ),
        )

    val stateWatcher: FlowWatcher<ConnectWatchUiState> = FlowWatcher(uiState)

    init {
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: ConnectWatchUiEvent) {
        when (event) {
            is ConnectWatchUiEvent.OnCodeChanged -> changeCode(event.text)
            ConnectWatchUiEvent.OnConnectClicked -> connect()
            ConnectWatchUiEvent.OnCloseClicked -> navigator.navigateBack()
        }
    }

    // Why: a tap while Connect is disabled (the web's Enter, a double tap) neither counts nor sends anything.
    override fun isAccepted(event: ConnectWatchUiEvent): Boolean =
        event != ConnectWatchUiEvent.OnConnectClicked || uiState.value.canConnect

    // Why: a new character clears the error, so Connect comes back for the next try.
    private fun changeCode(text: String) {
        val code = WatchPairingCodeFormat.normalize(text)
        uiState.update { state ->
            if (state.isConnecting || state.isConnected ||
                code == state.code
            ) {
                state
            } else {
                state.copy(
                    code = code,
                    error = null,
                )
            }
        }
    }

    private fun connect() {
        val code = uiState.value.code
        uiState.update { it.copy(isConnecting = true) }
        viewModelScope.launch {
            val result = approveWatchPairing(code)
            val error = result.exceptionOrNull()?.toConnectWatchError()
            trackEvent("connect_watch_clicked", mapOf("result" to (error?.name?.lowercase() ?: "connected")))
            uiState.update {
                it.copy(
                    isConnecting = false,
                    error = error,
                    isConnected = result.isSuccess,
                )
            }
        }
    }

    private fun Throwable.toConnectWatchError(): ConnectWatchError = when (this) {
        is InvalidWatchPairingCodeException, is ExpiredWatchPairingException -> ConnectWatchError.INVALID_OR_EXPIRED
        is TooManyWatchPairingAttemptsException -> ConnectWatchError.TOO_MANY_ATTEMPTS
        is WatchPairingNeedsAccountException -> ConnectWatchError.NEEDS_ACCOUNT
        else -> ConnectWatchError.FAILED
    }
}
