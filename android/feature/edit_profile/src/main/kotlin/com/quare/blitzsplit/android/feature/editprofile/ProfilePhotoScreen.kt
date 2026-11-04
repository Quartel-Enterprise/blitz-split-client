package com.quare.blitzsplit.android.feature.editprofile

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.PhotoCropDialog
import com.quare.blitzsplit.android.designsystem.component.PhotoCropFrame
import com.quare.blitzsplit.android.platform.rememberPhotoPickers
import com.quare.blitzsplit.feature.editprofile.presentation.model.ProfilePhotoUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.viewmodel.ProfilePhotoViewModel
import org.koin.compose.viewmodel.koinViewModel

// Why: Route.ProfilePhoto, a dialog over the profile whose shortcuts change the photo at once.
@Composable
fun ProfilePhotoScreen(viewModel: ProfilePhotoViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pickers = rememberPhotoPickers(
        snackbarHostState = snackbarHostState,
        onPhotoPicked = { bytes -> viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoPicked(bytes)) },
        onPhotoUnreadable = { viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoUnreadable) },
    )
    ProfilePhotoContent(
        state = state,
        onEvent = viewModel::onEvent,
        onTakePhotoClick = pickers.onTakePhotoClick,
        onChoosePhotoClick = pickers.onChoosePhotoClick,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    )
    state.photoCrop?.let { editor ->
        PhotoCropDialog(
            editor = editor,
            frame = PhotoCropFrame.CIRCLE,
            onEvent = { event -> viewModel.onEvent(ProfilePhotoUiEvent.OnPhotoCrop(event)) },
        )
    }
}
