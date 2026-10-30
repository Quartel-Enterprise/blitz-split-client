package com.quare.blitzsplit.android.feature.editprofile

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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.platform.createCameraPhotoUri
import com.quare.blitzsplit.android.platform.readPhotoBytes
import com.quare.blitzsplit.android.platform.rememberCameraPermissionAccess
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
    // Why: the camera app takes the full size picture into the app's file.
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
            viewModel.onEvent(EditProfileUiEvent.OnPhotoUnreadable)
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val deniedMessage = stringResource(R.string.edit_profile_camera_denied)
    val settingsLabel = stringResource(R.string.edit_profile_camera_settings)
    /*
     * Why: the app declares the camera permission for the invite reader, and Android then refuses the camera app's
     * intent until it is granted. Refusing it here is like closing the camera app: the photo stays as it was. Refused
     * for good, Android shows nothing more, so the snackbar points to the settings.
     */
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
    // Why: a device without a camera, or without a camera app, can't take one, as iOS hides it too.
    val hasCamera = remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    BackHandler { viewModel.onEvent(EditProfileUiEvent.OnBackClicked) }
    EditProfileContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
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
