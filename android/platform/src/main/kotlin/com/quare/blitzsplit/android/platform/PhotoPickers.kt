package com.quare.blitzsplit.android.platform

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
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
import kotlinx.coroutines.launch

// Why: "Tirar foto" opens the camera, null where there is none; "Escolher da galeria" the system photo picker.
class PhotoPickers(
    val onTakePhotoClick: (() -> Unit)?,
    val onChoosePhotoClick: () -> Unit,
) {
    // Why: a device with a camera to take the photo with, which a screen with its own camera asks too.
    val hasCamera: Boolean
        get() = onTakePhotoClick != null
}

/*
 * Why: Editar perfil's photo and a bill's "Adicionar foto" pick one the same way: the system photo picker, which needs
 * no storage permission and returns one image or nothing, or the camera app writing the full size photo into the
 * app's file. Either hands over the file's bytes, which the shared code decodes and shrinks; a file that can't be read
 * back, or a camera app that can't be opened, is unreadable.
 */
@Composable
fun rememberPhotoPickers(
    snackbarHostState: SnackbarHostState,
    onPhotoPicked: (bytes: ByteArray) -> Unit,
    onPhotoUnreadable: () -> Unit,
): PhotoPickers {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onPicked = { uri: Uri ->
        scope.launch {
            val bytes = context.readPhotoBytes(uri)
            if (bytes != null) onPhotoPicked(bytes) else onPhotoUnreadable()
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPicked)
    }
    // Why: kept across the camera app, which can outlive this activity, so its photo is still found on return.
    var cameraPhoto by rememberSaveable { mutableStateOf<Uri?>(null) }
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
            onPhotoUnreadable()
        }
    }
    val deniedMessage = stringResource(R.string.platform_camera_denied)
    val settingsLabel = stringResource(R.string.platform_camera_settings)
    /*
     * Why: the app declares the camera permission for the invite reader, and Android then refuses the camera app's
     * intent until it is granted. Refusing it here is like closing the camera app: nothing changes. Refused for good,
     * Android shows nothing more, so the snackbar points to the settings.
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
    // Why: a device without a camera, or without a camera app, only offers the gallery, as iOS hides it too.
    val hasCamera = remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    return PhotoPickers(
        onTakePhotoClick = if (hasCamera) {
            { if (cameraPermission.isGranted()) launchCamera() else cameraPermission.request() }
        } else {
            null
        },
        // Why: a build with neither the Photo Picker nor a documents app to fall back on reads as an unreadable photo.
        onChoosePhotoClick = {
            try {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } catch (_: ActivityNotFoundException) {
                onPhotoUnreadable()
            }
        },
    )
}
