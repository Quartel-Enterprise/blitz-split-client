package com.quare.blitzsplit.android.feature.splitbill

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.SheetActionItem
import com.quare.blitzsplit.android.platform.rememberPhotoPickers
import com.quare.blitzsplit.feature.splitbill.presentation.model.ReceiptPhotoSource
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

/*
 * Why: "Ler cupom fiscal" (7.1e) asks where the photo comes from first (beta feedback, part 2, item 7): "Tirar foto"
 * opens the app's own camera, with the receipt's frame, and "Escolher da galeria" the system photo picker, which needs
 * no storage permission. A device without a camera goes straight to the gallery. Always composed, so the picker's
 * photo still arrives once the request that opened it is over.
 */
@Composable
internal fun ReceiptSourceComponent(
    state: SplitBillUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (SplitBillUiEvent) -> Unit,
) {
    val pickers = rememberPhotoPickers(
        snackbarHostState = snackbarHostState,
        onPhotoPicked = { photo -> onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(photo)) },
        // Why: a gallery file that can't be read goes too, and reads as a receipt that couldn't be read.
        onPhotoUnreadable = { onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(ByteArray(0))) },
    )
    if (state.showsReceiptSourceChoice) {
        if (pickers.hasCamera) {
            ReceiptSourceSheet(
                onPick = { source -> onEvent(SplitBillUiEvent.OnReceiptSourcePicked(source)) },
                onDismiss = { onEvent(SplitBillUiEvent.OnReceiptPickerDismissed) },
            )
        } else {
            LaunchedEffect(Unit) { onEvent(SplitBillUiEvent.OnReceiptChoiceSkipped(ReceiptPhotoSource.PHOTOS)) }
        }
    }
    when (state.shownReceiptSource) {
        ReceiptPhotoSource.CAMERA -> ReceiptCameraDialog(
            onPhoto = { photo -> onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(photo)) },
            onDismiss = { onEvent(SplitBillUiEvent.OnReceiptPickerDismissed) },
        )

        // Why: the system picker has no "closed" to report, so the request ends as it opens; its photo comes later.
        ReceiptPhotoSource.PHOTOS, ReceiptPhotoSource.FILES -> LaunchedEffect(Unit) {
            pickers.onChoosePhotoClick()
            onEvent(SplitBillUiEvent.OnReceiptPickerDismissed)
        }

        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptSourceSheet(
    onPick: (ReceiptPhotoSource) -> Unit,
    onDismiss: () -> Unit,
) {
    val title = stringResource(R.string.split_bill_receipt_camera_title)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.semantics { paneTitle = title },
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                text = title,
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 8.dp,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            SheetActionItem(
                icon = Icons.Outlined.PhotoCamera,
                label = stringResource(R.string.split_bill_receipt_source_camera),
                onClick = { onPick(ReceiptPhotoSource.CAMERA) },
            )
            SheetActionItem(
                icon = Icons.Outlined.PhotoLibrary,
                label = stringResource(R.string.split_bill_receipt_source_gallery),
                onClick = { onPick(ReceiptPhotoSource.PHOTOS) },
            )
        }
    }
}
