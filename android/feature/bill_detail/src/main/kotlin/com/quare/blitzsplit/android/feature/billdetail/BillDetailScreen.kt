package com.quare.blitzsplit.android.feature.billdetail

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.DeleteBillDialog
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.android.designsystem.component.showReplacingSnackbar
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.platform.rememberGalleryPhotoSaver
import com.quare.blitzsplit.android.platform.rememberPhotoPickers
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPhotoError
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.BillDetailViewModel
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
                BillPhotoError.TOO_LARGE -> R.string.bill_detail_photo_too_large
                BillPhotoError.NO_ROOM -> R.string.bill_detail_photo_no_room
                BillPhotoError.FAILED -> R.string.bill_detail_photo_failed
            }

            BillDetailUiAction.ShowRemovePhotoError -> R.string.bill_detail_remove_photo_error
        }
    }
    // Why: "Adicionar foto" picks one as Editar perfil does, from the camera or the system photo picker.
    val pickers = rememberPhotoPickers(
        snackbarHostState = snackbarHostState,
        onPhotoPicked = { bytes -> viewModel.onEvent(BillDetailUiEvent.OnPhotoPicked(bytes)) },
        onPhotoUnreadable = { viewModel.onEvent(BillDetailUiEvent.OnPhotoUnreadable) },
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
