package com.quare.blitzsplit.android.feature.editprofile

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.platform.createCameraPhotoUri
import com.quare.blitzsplit.android.platform.readPhotoBytes
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.viewmodel.EditProfileViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditProfileScreen(viewModel: EditProfileViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onPicked = { uri: Uri ->
        scope.launch {
            val bytes = context.readPhotoBytes(uri)
            viewModel.onEvent(
                if (bytes != null) EditProfileUiEvent.OnPhotoPicked(bytes) else EditProfileUiEvent.OnPhotoUnreadable,
            )
        }
    }
    // Why: the system photo picker needs no storage permission and returns one image, or nothing.
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPicked)
    }
    // Why: kept across the camera app, which can outlive this activity, so its photo is still found on return.
    var cameraPhoto by rememberSaveable { mutableStateOf<Uri?>(null) }
    // Why: the camera app takes the full size picture into the app's file, so the app asks for no camera permission.
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { isSaved ->
        val uri = cameraPhoto
        if (isSaved && uri != null) onPicked(uri)
    }
    // Why: a device without a camera, or without a camera app, can't take one, as iOS hides it too.
    val hasCamera = remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    BackHandler { viewModel.onEvent(EditProfileUiEvent.OnBackClicked) }
    EditProfileContent(
        state = state,
        onEvent = viewModel::onEvent,
        onTakePhotoClick = if (hasCamera) {
            {
                try {
                    val uri = context.createCameraPhotoUri()
                    cameraPhoto = uri
                    camera.launch(uri)
                } catch (_: ActivityNotFoundException) {
                    viewModel.onEvent(EditProfileUiEvent.OnPhotoUnreadable)
                }
            }
        } else {
            null
        },
        onChoosePhotoClick = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}
