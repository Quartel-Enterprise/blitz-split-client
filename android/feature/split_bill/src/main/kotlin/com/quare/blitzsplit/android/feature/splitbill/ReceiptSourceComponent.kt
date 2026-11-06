package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.platform.ReceiptCameraTaps
import com.quare.blitzsplit.android.platform.ReceiptSourcePicker
import com.quare.blitzsplit.android.platform.ReceiptSourceRequest
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

// Why: Rachar conta's "Ler cupom fiscal" (7.1e): where the photo comes from and its pickers, as Início's shortcut has.
@Composable
internal fun ReceiptSourceComponent(
    state: SplitBillUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    ReceiptSourcePicker(
        request = ReceiptSourceRequest(
            showsChoice = state.showsReceiptSourceChoice,
            shownSource = state.shownReceiptSource,
            onSourcePicked = { onEvent(SplitBillUiEvent.OnReceiptSourcePicked(it)) },
            onChoiceSkipped = { onEvent(SplitBillUiEvent.OnReceiptChoiceSkipped(it)) },
            onDismissed = { onEvent(SplitBillUiEvent.OnReceiptPickerDismissed) },
            onPhotoPicked = { onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(it)) },
            cameraTaps = ReceiptCameraTaps(
                onTorchClicked = { onEvent(SplitBillUiEvent.OnReceiptTorchClicked(it)) },
                onAllowClicked = { onEvent(SplitBillUiEvent.OnReceiptCameraAllowClicked) },
                onSettingsClicked = { onEvent(SplitBillUiEvent.OnReceiptCameraSettingsClicked) },
            ),
        ),
        snackbarHostState = snackbarHostState,
    )
}
