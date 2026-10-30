package com.quare.blitzsplit.feature.join.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.groups.domain.model.InviteLink
import com.quare.blitzsplit.core.groups.domain.model.InviteTarget
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.feature.join.presentation.model.CameraAccess
import com.quare.blitzsplit.feature.join.presentation.model.JoinWithLinkUiAction
import com.quare.blitzsplit.feature.join.presentation.model.JoinWithLinkUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.JoinWithLinkUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class JoinWithLinkViewModel(
    private val navigator: Navigator,
) : ViewModel() {
    val uiState: StateFlow<JoinWithLinkUiState>
        field = MutableStateFlow(
            JoinWithLinkUiState(
                link = "",
                isLinkInvalid = false,
                isQrInvalid = false,
                cameraAccess = CameraAccess.ASKING,
                isTorchOn = false,
            ),
        )

    val uiAction: SharedFlow<JoinWithLinkUiAction>
        field = MutableSharedFlow<JoinWithLinkUiAction>()

    val stateWatcher: FlowWatcher<JoinWithLinkUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<JoinWithLinkUiAction> = FlowWatcher(uiAction)

    private val invalidQrDuration = 3.seconds
    private var invalidQrTimer: Job? = null

    fun onEvent(event: JoinWithLinkUiEvent) {
        when (event) {
            is JoinWithLinkUiEvent.OnCameraAccessChanged -> uiState.update {
                it.copy(
                    cameraAccess = if (event.isGranted) CameraAccess.GRANTED else CameraAccess.DENIED,
                    isTorchOn = it.isTorchOn && event.isGranted,
                )
            }

            JoinWithLinkUiEvent.OnOpenSettingsClicked -> viewModelScope.launch {
                uiAction.emit(JoinWithLinkUiAction.OpenCameraSettings)
            }

            JoinWithLinkUiEvent.OnTorchClicked -> uiState.update {
                it.copy(isTorchOn = !it.isTorchOn && it.cameraAccess == CameraAccess.GRANTED)
            }

            is JoinWithLinkUiEvent.OnQrScanned -> InviteLink.findIn(event.content)?.let(::open) ?: showInvalidQr()

            is JoinWithLinkUiEvent.OnLinkChanged -> uiState.update {
                it.copy(
                    link = event.link,
                    isLinkInvalid = false,
                )
            }

            is JoinWithLinkUiEvent.OnPasted -> paste(event.text)

            JoinWithLinkUiEvent.OnClearClicked -> uiState.update {
                it.copy(
                    link = "",
                    isLinkInvalid = false,
                )
            }

            JoinWithLinkUiEvent.OnOpenClicked -> {
                val link = uiState.value.link
                if (link.isNotBlank()) {
                    InviteLink.findIn(link)?.let(::open) ?: uiState.update { it.copy(isLinkInvalid = true) }
                }
            }

            JoinWithLinkUiEvent.OnCloseClicked -> if (isOnTop()) navigator.navigateBack()
        }
    }

    /*
     * Why: the invite takes the reader's place, so back from it goes where the reader came from. Only from the top: the
     * camera keeps reading the same QR after the first one opened it.
     */
    private fun open(target: InviteTarget) {
        if (!isOnTop()) return
        navigator.replaceTop(
            Route.JoinInvite(
                code = target.code,
                memberCode = target.memberCode,
                inviterCode = target.inviterCode,
            ),
        )
    }

    // Why: the camera keeps reading, so pointing at the wrong QR keeps the warning, and it goes a moment after.
    private fun showInvalidQr() {
        uiState.update { it.copy(isQrInvalid = true) }
        invalidQrTimer?.cancel()
        invalidQrTimer = viewModelScope.launch {
            delay(invalidQrDuration)
            uiState.update { it.copy(isQrInvalid = false) }
        }
    }

    // Why: an empty clipboard leaves the field as it was.
    private fun paste(text: String) {
        if (text.isBlank()) return
        InviteLink.findIn(text)?.let(::open) ?: uiState.update {
            it.copy(
                link = text.trim(),
                isLinkInvalid = true,
            )
        }
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == Route.JoinWithLink
}
