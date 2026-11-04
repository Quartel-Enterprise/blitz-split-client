package com.quare.blitzsplit.android.feature.editgroup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.PhotoCropDialog
import com.quare.blitzsplit.android.designsystem.component.PhotoCropFrame
import com.quare.blitzsplit.android.designsystem.component.SimplifyConflictNoticeEffect
import com.quare.blitzsplit.android.platform.rememberPhotoPickers
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.EditGroupViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditGroupScreen(
    route: Route.EditGroup,
    // Why: a tablet opens the form as a dialog over the group, so the screen drops its own scaffold.
    isDialog: Boolean,
    viewModel: EditGroupViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler { viewModel.onEvent(EditGroupUiEvent.OnCloseClicked) }
    // Why: the camera's "Permita a câmera nas configurações" and the conflict of 13.1e show over the form.
    val snackbarHostState = remember { SnackbarHostState() }
    val pickers = rememberPhotoPickers(
        snackbarHostState = snackbarHostState,
        onPhotoPicked = { bytes -> viewModel.onEvent(EditGroupUiEvent.OnPhotoPicked(bytes)) },
        onPhotoUnreadable = { viewModel.onEvent(EditGroupUiEvent.OnPhotoUnreadable) },
    )
    Box {
        EditGroupContent(
            state = state,
            isDialog = isDialog,
            onEvent = viewModel::onEvent,
            onTakePhotoClick = pickers.onTakePhotoClick,
            onChoosePhotoClick = pickers.onChoosePhotoClick,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
    state.simplifyConfirmation?.let { confirmation ->
        SimplifyDebtsDialog(
            confirmation = confirmation,
            currency = state.currency,
            isChanging = state.isChangingSimplifyDebts,
            onEvent = viewModel::onEvent,
        )
    }
    SimplifyConflictNoticeEffect(
        notice = state.simplifyConflict,
        snackbarHostState = snackbarHostState,
        onActivityClick = { viewModel.onEvent(EditGroupUiEvent.OnSimplifyConflictActivityClicked) },
        onDismiss = { viewModel.onEvent(EditGroupUiEvent.OnSimplifyConflictDismissed) },
    )
    // Why: the rounded square every group's photo is drawn in, whatever the group's shape.
    state.photoCrop?.let { editor ->
        PhotoCropDialog(
            editor = editor,
            frame = PhotoCropFrame.ROUNDED_SQUARE,
            onEvent = { event -> viewModel.onEvent(EditGroupUiEvent.OnPhotoCrop(event)) },
        )
    }
}
