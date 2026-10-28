package com.quare.blitzsplit.android.feature.editprofile

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.feature.editprofile.presentation.model.EditProfileUiEvent
import com.quare.blitzsplit.feature.editprofile.presentation.viewmodel.EditProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import java.io.ByteArrayOutputStream

private const val CAMERA_JPEG_QUALITY = 95

@Composable
fun EditProfileScreen(viewModel: EditProfileViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onPicked = { bytes: ByteArray? ->
        viewModel.onEvent(
            if (bytes != null) EditProfileUiEvent.OnPhotoPicked(bytes) else EditProfileUiEvent.OnPhotoUnreadable,
        )
    }
    // Why: the system photo picker needs no storage permission and returns one image, or nothing.
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch { onPicked(context.readBytes(uri)) }
    }
    // Why: the camera app takes the picture, so the app asks for no camera permission; the avatar is 512px anyway.
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap ?: return@rememberLauncherForActivityResult
        scope.launch { onPicked(bitmap.toJpeg()) }
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
                    camera.launch(null)
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

private suspend fun Context.readBytes(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
}

private suspend fun Bitmap.toJpeg(): ByteArray = withContext(Dispatchers.Default) {
    ByteArrayOutputStream().use { stream ->
        compress(Bitmap.CompressFormat.JPEG, CAMERA_JPEG_QUALITY, stream)
        stream.toByteArray()
    }
}
