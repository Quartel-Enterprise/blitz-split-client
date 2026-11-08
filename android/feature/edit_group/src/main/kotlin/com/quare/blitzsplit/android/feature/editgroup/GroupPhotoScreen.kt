package com.quare.blitzsplit.android.feature.editgroup

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.PhotoCropDialog
import com.quare.blitzsplit.android.designsystem.component.PhotoCropFrame
import com.quare.blitzsplit.android.platform.rememberPhotoPickers
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.editgroup.presentation.model.GroupPhotoUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.GroupPhotoViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// Why: Route.GroupPhoto, a dialog over the group whose shortcuts change the photo at once.
@Composable
fun GroupPhotoScreen(
    route: Route.GroupPhoto,
    viewModel: GroupPhotoViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pickers = rememberPhotoPickers(
        snackbarHostState = snackbarHostState,
        onPhotoPicked = { bytes -> viewModel.onEvent(GroupPhotoUiEvent.OnPhotoPicked(bytes)) },
        onPhotoUnreadable = { viewModel.onEvent(GroupPhotoUiEvent.OnPhotoUnreadable) },
    )
    GroupPhotoContent(
        state = state,
        onEvent = viewModel::onEvent,
        onTakePhotoClick = pickers.onTakePhotoClick,
        onChoosePhotoClick = pickers.onChoosePhotoClick,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    )
    state.photoCrop?.let { editor ->
        PhotoCropDialog(
            editor = editor,
            frame = PhotoCropFrame.ROUNDED_SQUARE,
            onEvent = { event -> viewModel.onEvent(GroupPhotoUiEvent.OnPhotoCrop(event)) },
        )
    }
}
