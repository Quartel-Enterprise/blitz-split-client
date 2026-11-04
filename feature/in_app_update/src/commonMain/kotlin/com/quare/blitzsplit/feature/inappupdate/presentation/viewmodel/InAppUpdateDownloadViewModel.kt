package com.quare.blitzsplit.feature.inappupdate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateDownloadState
import com.quare.blitzsplit.core.inappupdate.domain.usecase.CompleteUpdateInstall
import com.quare.blitzsplit.core.inappupdate.domain.usecase.ObserveUpdateDownloadState
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.feature.inappupdate.presentation.model.InAppUpdateDownloadUiAction
import com.quare.blitzsplit.feature.inappupdate.presentation.model.InAppUpdateDownloadUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/*
 * Why: Google Play's flexible update downloads while the app stays in use (Android only): "Baixando atualização… 42%"
 * over every screen, installed (the app restarts) as soon as it finishes, and a notice when it fails.
 */
class InAppUpdateDownloadViewModel(
    private val completeUpdateInstall: CompleteUpdateInstall,
    observeUpdateDownloadState: ObserveUpdateDownloadState,
) : ViewModel() {
    val uiState: StateFlow<InAppUpdateDownloadUiState>
        field = MutableStateFlow(InAppUpdateDownloadUiState(downloadPercent = null))

    val uiAction: SharedFlow<InAppUpdateDownloadUiAction>
        field = MutableSharedFlow<InAppUpdateDownloadUiAction>()

    val stateWatcher: FlowWatcher<InAppUpdateDownloadUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<InAppUpdateDownloadUiAction> = FlowWatcher(uiAction)

    init {
        observeUpdateDownloadState()
            .onEach(::onDownloadState)
            .launchIn(viewModelScope)
    }

    private suspend fun onDownloadState(state: UpdateDownloadState) {
        when (state) {
            is UpdateDownloadState.Downloading -> showPercent(state.percent)

            UpdateDownloadState.Downloaded -> {
                /*
                 * Why: the updater replays Downloaded to a ViewModel made after the download ended (the Activity
                 * recreated, the process kept). That case is the foreground check's, which installs an update
                 * downloaded earlier, so the same download isn't installed twice.
                 */
                val hasWatchedDownload = uiState.value.downloadPercent != null
                showPercent(null)
                if (hasWatchedDownload) completeUpdateInstall()
            }

            UpdateDownloadState.Failed -> {
                showPercent(null)
                uiAction.emit(InAppUpdateDownloadUiAction.ShowDownloadFailed)
            }

            UpdateDownloadState.Idle -> showPercent(null)
        }
    }

    private fun showPercent(percent: Int?) {
        uiState.update { it.copy(downloadPercent = percent) }
    }
}
