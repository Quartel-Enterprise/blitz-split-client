package com.quare.blitzsplit.android.feature.billdetail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.showReplacingSnackbar
import com.quare.blitzsplit.android.platform.shareText
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillUiAction
import com.quare.blitzsplit.feature.billdetail.presentation.model.SharedBillUiEvent
import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.SharedBillViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/*
 * Why: a bill's link opened (7.3h), blitzsplit.com/c/<code>: a screen of its own over wherever the app is, which draws
 * the bill as Conta does, read-only (no Editar, Apagar, Histórico nor adding or removing photos), with its attachments
 * and comments. Whoever is signed in comments, in the group or not, online only.
 */
@Composable
fun SharedBillScreen(
    route: Route.SharedBill,
    viewModel: SharedBillViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val saveToGallery = rememberBillPhotoSaver(snackbarHostState)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SharedBillActionsEffect(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
    )
    SharedBillContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
    BillAttachmentViewerDialog(
        attachments = state.attachments,
        viewedIndex = state.viewedAttachmentIndex,
        photoCount = state.photoCount,
        isOnline = state.isOnline,
        snackbarHostState = snackbarHostState,
        callbacks = BillViewerCallbacks(
            onDownloadClick = { source, fileName ->
                viewModel.onEvent(SharedBillUiEvent.OnDownloadClicked)
                saveToGallery(source, fileName)
            },
            onPageChange = { viewModel.onEvent(SharedBillUiEvent.OnViewerPageChanged(it)) },
            onRemoveClick = null,
            onDismiss = { viewModel.onEvent(SharedBillUiEvent.OnViewerDismissed) },
        ),
    )
    if (state.commentToDelete != null) {
        DeleteCommentDialog(
            isBusy = state.isChangingComment,
            onConfirm = { viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteConfirmed) },
            onDismiss = { viewModel.onEvent(SharedBillUiEvent.OnCommentDeleteDismissed) },
        )
    }
    state.likesComment?.let { comment ->
        CommentLikesSheet(
            comment = comment,
            onDismiss = { viewModel.onEvent(SharedBillUiEvent.OnCommentLikesDismissed) },
        )
    }
}

// Why: a comment's "Compartilhar" goes through the system share; everything else the page says is a snackbar.
@Composable
private fun SharedBillActionsEffect(
    viewModel: SharedBillViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            val message = when (action) {
                is SharedBillUiAction.ShareComment -> {
                    context.shareText(shareCommentTextOf(resources, action.share))
                    null
                }

                SharedBillUiAction.ShowCommentError -> R.string.shared_bill_comment_error

                SharedBillUiAction.ShowNeedsName -> R.string.shared_bill_needs_name

                SharedBillUiAction.ShowCommentChangeError -> R.string.shared_bill_comment_change_error

                SharedBillUiAction.ShowCommentGone -> R.string.bill_detail_comment_gone
            }
            message?.let { showReplacingSnackbar(snackbarHostState, resources.getString(it)) }
        }
    }
}
