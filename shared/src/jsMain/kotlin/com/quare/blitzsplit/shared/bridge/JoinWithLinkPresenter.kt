@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.join.presentation.model.JoinWithLinkUiAction
import com.quare.blitzsplit.feature.join.presentation.model.JoinWithLinkUiEvent
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinWithLinkViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

class JsJoinWithLinkState(
    val link: String,
    val isLinkInvalid: Boolean,
    val isQrInvalid: Boolean,
    // Why: the CameraAccess's name in lower case, "asking", "granted" or "denied".
    val cameraAccess: String,
    val isTorchOn: Boolean,
    val canOpen: Boolean,
)

class JoinWithLinkPresenter {
    private val holder = ViewModelHolder(JoinWithLinkViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsJoinWithLinkState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsJoinWithLinkState(
                link = state.link,
                isLinkInvalid = state.isLinkInvalid,
                isQrInvalid = state.isQrInvalid,
                cameraAccess = state.cameraAccess.name.lowercase(),
                isTorchOn = state.isTorchOn,
                canOpen = state.canOpen,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "open-camera-settings". A browser has no settings page to open, so the web asks for the camera again.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                JoinWithLinkUiAction.OpenCameraSettings -> "open-camera-settings"
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun changeCameraAccess(isGranted: Boolean) {
        viewModel.onEvent(JoinWithLinkUiEvent.OnCameraAccessChanged(isGranted))
    }

    fun openSettings() {
        viewModel.onEvent(JoinWithLinkUiEvent.OnOpenSettingsClicked)
    }

    fun toggleTorch() {
        viewModel.onEvent(JoinWithLinkUiEvent.OnTorchClicked)
    }

    fun scan(content: String) {
        viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned(content))
    }

    fun changeLink(link: String) {
        viewModel.onEvent(JoinWithLinkUiEvent.OnLinkChanged(link))
    }

    fun paste(text: String) {
        viewModel.onEvent(JoinWithLinkUiEvent.OnPasted(text))
    }

    fun clear() {
        viewModel.onEvent(JoinWithLinkUiEvent.OnClearClicked)
    }

    fun open() {
        viewModel.onEvent(JoinWithLinkUiEvent.OnOpenClicked)
    }

    fun close() {
        viewModel.onEvent(JoinWithLinkUiEvent.OnCloseClicked)
    }

    fun dispose() {
        holder.clear()
    }
}
