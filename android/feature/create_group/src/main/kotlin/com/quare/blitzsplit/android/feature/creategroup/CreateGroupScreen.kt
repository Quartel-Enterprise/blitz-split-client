package com.quare.blitzsplit.android.feature.creategroup

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
import com.quare.blitzsplit.android.platform.rememberPhotoPickers
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiEvent
import com.quare.blitzsplit.feature.creategroup.presentation.viewmodel.CreateGroupViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateGroupScreen(
    // Why: anything wider than a phone opens the new group as a dialog over the home, so the screen drops its scaffold.
    isDialog: Boolean,
    viewModel: CreateGroupViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler { viewModel.onEvent(CreateGroupUiEvent.OnCloseClicked) }
    // Why: the camera's "Permita a câmera nas configurações" shows over the form.
    val snackbarHostState = remember { SnackbarHostState() }
    val pickers = rememberPhotoPickers(
        snackbarHostState = snackbarHostState,
        onPhotoPicked = { bytes -> viewModel.onEvent(CreateGroupUiEvent.OnPhotoPicked(bytes)) },
        onPhotoUnreadable = { viewModel.onEvent(CreateGroupUiEvent.OnPhotoUnreadable) },
    )
    Box {
        CreateGroupContent(
            state = state,
            isDialog = isDialog,
            onEvent = viewModel::onEvent,
            photoPickers = pickers,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
    // Why: the rounded square every group's photo is drawn in, whatever the group's shape.
    state.photoCrop?.let { editor ->
        PhotoCropDialog(
            editor = editor,
            frame = PhotoCropFrame.ROUNDED_SQUARE,
            onEvent = { event -> viewModel.onEvent(CreateGroupUiEvent.OnPhotoCrop(event)) },
        )
    }
}
