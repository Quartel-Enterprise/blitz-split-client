package com.quare.blitzsplit.android.feature.billdetail

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.DeleteBillDialog
import com.quare.blitzsplit.android.designsystem.component.showReplacingSnackbar
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.platform.rememberGalleryPhotoSaver
import com.quare.blitzsplit.android.platform.rememberPhotoPickers
import com.quare.blitzsplit.android.platform.shareText
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPhotoError
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.BillDetailViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.quare.blitzsplit.android.designsystem.R as DesignR

@Composable
fun BillDetailScreen(
    route: Route.BillDetail,
    // Why: a tablet and the upright foldable keep the group behind the bill, as a dialog.
    isDialog: Boolean,
    viewModel: BillDetailViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            val message = when (action) {
                // Why: a comment's "Compartilhar" goes through the system share, the only action that isn't a message.
                is BillDetailUiAction.ShareComment -> {
                    context.shareText(shareCommentTextOf(resources, action.share))
                    null
                }

                BillDetailUiAction.ShowDeleteError -> DesignR.string.designsystem_delete_bill_error

                BillDetailUiAction.ShowCommentError -> R.string.bill_detail_comment_error

                BillDetailUiAction.ShowCommentChangeError -> R.string.bill_detail_comment_change_error

                BillDetailUiAction.ShowCommentGone -> R.string.bill_detail_comment_gone

                BillDetailUiAction.ShowUndoError -> R.string.bill_detail_undo_error

                is BillDetailUiAction.ShowPhotoError -> when (action.error) {
                    BillPhotoError.UNREADABLE -> R.string.bill_detail_photo_unreadable
                    BillPhotoError.TOO_LARGE -> R.string.bill_detail_photo_too_large
                    BillPhotoError.NO_ROOM -> R.string.bill_detail_photo_no_room
                    BillPhotoError.FAILED -> R.string.bill_detail_photo_failed
                }

                BillDetailUiAction.ShowRemovePhotoError -> R.string.bill_detail_remove_photo_error
            }
            message?.let { showReplacingSnackbar(snackbarHostState, resources.getString(it)) }
        }
    }
    // Why: "Adicionar foto" picks one as Editar perfil does, from the camera or the system photo picker.
    val pickers = rememberPhotoPickers(
        snackbarHostState = snackbarHostState,
        onPhotoPicked = { bytes -> viewModel.onEvent(BillDetailUiEvent.OnPhotoPicked(bytes)) },
        onPhotoUnreadable = { viewModel.onEvent(BillDetailUiEvent.OnPhotoUnreadable) },
    )
    val scope = rememberCoroutineScope()
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
        onDownloadClick = { source, fileName ->
            viewModel.onEvent(BillDetailUiEvent.OnDownloadClicked)
            saveToGallery(source, fileName)
        },
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
    if (state.commentToDelete != null) {
        DeleteCommentDialog(onEvent = viewModel::onEvent)
    }
    state.likesComment?.let { comment ->
        CommentLikesSheet(
            comment = comment,
            onEvent = viewModel::onEvent,
        )
    }
}
