package com.quare.blitzsplit.android.feature.editprofile

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.PhotoCropDialog
import com.quare.blitzsplit.android.designsystem.component.PhotoCropFrame
import com.quare.blitzsplit.android.platform.rememberPhotoPickers
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.viewmodel.EditProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditProfileScreen(viewModel: EditProfileViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pickers = rememberPhotoPickers(
        snackbarHostState = snackbarHostState,
        onPhotoPicked = { bytes -> viewModel.onEvent(EditProfileUiEvent.OnPhotoPicked(bytes)) },
        onPhotoUnreadable = { viewModel.onEvent(EditProfileUiEvent.OnPhotoUnreadable) },
    )
    BackHandler { viewModel.onEvent(EditProfileUiEvent.OnBackClicked) }
    EditProfileContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onTakePhotoClick = pickers.onTakePhotoClick,
        onChoosePhotoClick = pickers.onChoosePhotoClick,
    )
    state.photoCrop?.let { editor ->
        PhotoCropDialog(
            editor = editor,
            frame = PhotoCropFrame.CIRCLE,
            onEvent = { event -> viewModel.onEvent(EditProfileUiEvent.OnPhotoCrop(event)) },
        )
    }
}
