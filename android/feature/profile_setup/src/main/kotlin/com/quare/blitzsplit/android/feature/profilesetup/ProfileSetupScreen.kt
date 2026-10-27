package com.quare.blitzsplit.android.feature.profilesetup

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.feature.profilesetup.presentation.model.ProfileSetupUiEvent
import com.quare.blitzsplit.feature.profilesetup.presentation.viewmodel.ProfileSetupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileSetupScreen(viewModel: ProfileSetupViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Why: the system photo picker needs no storage permission and returns one image, or nothing.
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = context.readBytes(uri)
            viewModel.onEvent(
                if (bytes != null) ProfileSetupUiEvent.OnPhotoPicked(bytes) else ProfileSetupUiEvent.OnPhotoUnreadable,
            )
        }
    }
    BackHandler { viewModel.onEvent(ProfileSetupUiEvent.OnBackClicked) }
    ProfileSetupContent(
        state = state,
        onEvent = viewModel::onEvent,
        onAddPhotoClick = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}

private suspend fun Context.readBytes(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
}
