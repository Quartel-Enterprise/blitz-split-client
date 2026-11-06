package com.quare.blitzsplit.android.platform

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.SheetActionItem
import com.quare.blitzsplit.core.model.ReceiptPhotoSource
import kotlinx.coroutines.launch

// Why: what the shared code sends and takes about "Ler cupom fiscal"'s photo, on Rachar conta and on Início alike.
class ReceiptSourceRequest(
    // Why: "Tirar foto", the gallery or a file asked for.
    val showsChoice: Boolean,
    // Why: the picker picked on that choice, open until a photo comes or it closes.
    val shownSource: ReceiptPhotoSource?,
    val onSourcePicked: (ReceiptPhotoSource) -> Unit,
    // Why: a device without a camera skips the choice and opens the gallery.
    val onChoiceSkipped: (ReceiptPhotoSource) -> Unit,
    val onDismissed: () -> Unit,
    // Why: the photo as it came; empty bytes for a file that couldn't be read, which reads as a receipt that couldn't be.
    val onPhotoPicked: (ByteArray) -> Unit,
    val cameraTaps: ReceiptCameraTaps,
)

/*
 * Why: "Ler cupom fiscal" (7.1e) asks where the photo comes from first (beta feedback, part 2, item 7; part 3, item 16):
 * "Tirar foto" opens the app's own camera, with the receipt's frame, "Escolher da galeria" the system photo picker,
 * which needs no storage permission, and "Escolher arquivo" the documents picker (a receipt saved as an image). A device
 * without a camera goes straight to the gallery. Always composed, so the picker's photo still arrives once the request
 * that opened it is over.
 */
@Composable
fun ReceiptSourcePicker(
    request: ReceiptSourceRequest,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickers = rememberPhotoPickers(
        snackbarHostState = snackbarHostState,
        onPhotoPicked = request.onPhotoPicked,
        onPhotoUnreadable = { request.onPhotoPicked(ByteArray(0)) },
    )
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch { request.onPhotoPicked(context.readPhotoBytes(uri) ?: ByteArray(0)) }
        }
    }
    if (request.showsChoice) {
        if (pickers.hasCamera) {
            ReceiptSourceSheet(
                onPick = request.onSourcePicked,
                onDismiss = request.onDismissed,
            )
        } else {
            LaunchedEffect(Unit) { request.onChoiceSkipped(ReceiptPhotoSource.PHOTOS) }
        }
    }
    when (request.shownSource) {
        ReceiptPhotoSource.CAMERA -> ReceiptCameraDialog(
            onPhoto = request.onPhotoPicked,
            onDismiss = request.onDismissed,
            taps = request.cameraTaps,
        )

        // Why: the system pickers have no "closed" to report, so the request ends as they open; the photo comes later.
        ReceiptPhotoSource.PHOTOS -> LaunchedEffect(Unit) {
            pickers.onChoosePhotoClick()
            request.onDismissed()
        }

        ReceiptPhotoSource.FILES -> LaunchedEffect(Unit) {
            try {
                filePicker.launch(arrayOf("image/*"))
            } catch (_: ActivityNotFoundException) {
                request.onPhotoPicked(ByteArray(0))
            }
            request.onDismissed()
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
    val title = stringResource(R.string.platform_receipt_camera_title)
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
                label = stringResource(R.string.platform_receipt_source_camera),
                onClick = { onPick(ReceiptPhotoSource.CAMERA) },
            )
            SheetActionItem(
                icon = Icons.Outlined.PhotoLibrary,
                label = stringResource(R.string.platform_receipt_source_gallery),
                onClick = { onPick(ReceiptPhotoSource.PHOTOS) },
            )
            SheetActionItem(
                icon = Icons.Outlined.Folder,
                label = stringResource(R.string.platform_receipt_source_file),
                onClick = { onPick(ReceiptPhotoSource.FILES) },
            )
        }
    }
}
