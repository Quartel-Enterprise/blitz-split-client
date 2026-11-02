package com.quare.blitzsplit.android.feature.billdetail

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.DeleteBillDialog
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.android.designsystem.component.showReplacingSnackbar
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.platform.createCameraPhotoUri
import com.quare.blitzsplit.android.platform.readPhotoBytes
import com.quare.blitzsplit.android.platform.rememberCameraPermissionAccess
import com.quare.blitzsplit.android.platform.rememberGalleryPhotoSaver
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPhotoError
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.BillDetailViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun BillDetailScreen(
    route: Route.BillDetail,
    // Why: a tablet and the upright foldable keep the group behind the bill, as a dialog.
    isDialog: Boolean,
    viewModel: BillDetailViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberActionSnackbarHostState(viewModel.uiAction) { action ->
        when (action) {
            BillDetailUiAction.ShowDeleteError -> R.string.bill_detail_delete_error

            BillDetailUiAction.ShowCommentError -> R.string.bill_detail_comment_error

            BillDetailUiAction.ShowUndoError -> R.string.bill_detail_undo_error

            is BillDetailUiAction.ShowPhotoError -> when (action.error) {
                BillPhotoError.UNREADABLE -> R.string.bill_detail_photo_unreadable
                BillPhotoError.NO_ROOM -> R.string.bill_detail_photo_no_room
                BillPhotoError.FAILED -> R.string.bill_detail_photo_failed
            }

            BillDetailUiAction.ShowRemovePhotoError -> R.string.bill_detail_remove_photo_error
        }
    }
    val pickers = rememberBillPhotoPickers(
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val saveToGallery = rememberGalleryPhotoSaver { isSaved ->
        scope.showReplacingSnackbar(
            snackbarHostState = snackbarHostState,
            message = resources.getString(
                if (isSaved) R.string.bill_detail_photo_saved else R.string.bill_detail_photo_save_error,
            ),
        )
    }
    BackHandler { viewModel.onEvent(BillDetailUiEvent.OnBackClicked) }
    BillDetailContent(
        state = state,
        isDialog = isDialog,
        snackbarHostState = snackbarHostState,
        pickers = pickers,
        onEvent = viewModel::onEvent,
    )
    BillAttachmentViewerDialog(
        state = state,
        snackbarHostState = snackbarHostState,
        onDownloadClick = saveToGallery,
        onEvent = viewModel::onEvent,
    )
    state.deletion?.let { deletion ->
        DeleteBillDialog(
            description = deletion.description,
            myBalanceMinor = deletion.myBalanceMinor,
            settledPair = deletion.settledPair,
            isDeleting = state.isDeleting,
            money = rememberMoneyFormatter(state.currency),
            onConfirm = { viewModel.onEvent(BillDetailUiEvent.OnDeleteConfirmed) },
            onDismiss = { viewModel.onEvent(BillDetailUiEvent.OnDeleteDismissed) },
        )
    }
}

/*
 * Why: "Adicionar foto" as Editar perfil picks one: the system photo picker, which needs no storage permission, or the
 * camera app writing the full size photo into the app's file. Either hands the ViewModel the file's bytes, which the
 * shared code decodes and shrinks.
 */
@Composable
private fun rememberBillPhotoPickers(
    snackbarHostState: SnackbarHostState,
    onEvent: (BillDetailUiEvent) -> Unit,
): BillPhotoPickers {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onPicked = { uri: Uri ->
        scope.launch {
            val bytes = context.readPhotoBytes(uri)
            onEvent(if (bytes != null) BillDetailUiEvent.OnPhotoPicked(bytes) else BillDetailUiEvent.OnPhotoUnreadable)
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPicked)
    }
    // Why: kept across the camera app, which can outlive this activity, so its photo is still found on return.
    var cameraPhoto by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { isSaved ->
        val uri = cameraPhoto
        if (isSaved && uri != null) onPicked(uri)
    }
    val launchCamera = {
        try {
            val uri = context.createCameraPhotoUri()
            cameraPhoto = uri
            camera.launch(uri)
        } catch (_: ActivityNotFoundException) {
            onEvent(BillDetailUiEvent.OnPhotoUnreadable)
        }
    }
    val deniedMessage = stringResource(R.string.bill_detail_camera_denied)
    val settingsLabel = stringResource(R.string.bill_detail_camera_settings)
    // Why: refused for good, Android shows nothing more, so the snackbar points to the settings.
    val cameraPermission = rememberCameraPermissionAccess { isGranted ->
        when {
            isGranted -> launchCamera()

            isDeniedForGood() -> scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(deniedMessage, actionLabel = settingsLabel)
                if (result == SnackbarResult.ActionPerformed) openSettings()
            }
        }
    }
    // Why: a device without a camera, or without a camera app, only offers the gallery.
    val hasCamera = remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    return BillPhotoPickers(
        onTakePhotoClick = if (hasCamera) {
            { if (cameraPermission.isGranted()) launchCamera() else cameraPermission.request() }
        } else {
            null
        },
        onChoosePhotoClick = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}
