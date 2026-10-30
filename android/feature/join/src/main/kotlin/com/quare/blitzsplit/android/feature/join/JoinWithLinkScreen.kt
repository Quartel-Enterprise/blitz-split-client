package com.quare.blitzsplit.android.feature.join

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.platform.ClipboardReader
import com.quare.blitzsplit.android.platform.QrCameraDisplay
import com.quare.blitzsplit.android.platform.rememberCameraPermissionAccess
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.join.presentation.model.CameraAccess
import com.quare.blitzsplit.feature.join.presentation.model.JoinWithLinkUiAction
import com.quare.blitzsplit.feature.join.presentation.model.JoinWithLinkUiEvent
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinWithLinkViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun JoinWithLinkScreen(
    route: Route.JoinWithLink,
    viewModel: JoinWithLinkViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = remember(context) { ClipboardReader(context) }
    // Why: saved, since the activity may be recreated (a rotation) while the system dialog is on top of it.
    var hasAsked by rememberSaveable { mutableStateOf(false) }
    val cameraPermission = rememberCameraPermissionAccess { isGranted ->
        viewModel.onEvent(JoinWithLinkUiEvent.OnCameraAccessChanged(isGranted))
    }
    // Why: asked once when the reader opens, and read again on coming back from the settings.
    LifecycleResumeEffect(cameraPermission) {
        when {
            cameraPermission.isGranted() -> viewModel.onEvent(
                JoinWithLinkUiEvent.OnCameraAccessChanged(isGranted = true),
            )

            !hasAsked -> {
                hasAsked = true
                cameraPermission.request()
            }

            viewModel.uiState.value.cameraAccess == CameraAccess.GRANTED ->
                viewModel.onEvent(JoinWithLinkUiEvent.OnCameraAccessChanged(isGranted = false))
        }
        onPauseOrDispose {}
    }
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                JoinWithLinkUiAction.OpenCameraSettings -> cameraPermission.openSettings()
            }
        }
    }
    var field by rememberSaveable { mutableStateOf(viewModel.uiState.value.link) }
    // Why: a paste or "Limpar" change the link without the keyboard. Read from the ViewModel, never a frame behind it.
    LaunchedEffect(viewModel) {
        viewModel.uiState
            .map { it.link }
            .distinctUntilChanged()
            .collect { link -> if (link != field) field = link }
    }
    LightStatusBarEffect()
    BackHandler { viewModel.onEvent(JoinWithLinkUiEvent.OnCloseClicked) }
    JoinWithLinkContent(
        state = state,
        field = field,
        onFieldChange = {
            field = it
            viewModel.onEvent(JoinWithLinkUiEvent.OnLinkChanged(it))
        },
        onPasteClicked = { viewModel.onEvent(JoinWithLinkUiEvent.OnPasted(clipboard.readText().orEmpty())) },
        onEvent = viewModel::onEvent,
        camera = { modifier ->
            QrCameraDisplay(
                isTorchOn = state.isTorchOn,
                onScanned = { viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned(it)) },
                onOpened = { hasTorch -> viewModel.onEvent(JoinWithLinkUiEvent.OnCameraOpened(hasTorch)) },
                onUnavailable = { viewModel.onEvent(JoinWithLinkUiEvent.OnCameraUnavailable) },
                modifier = modifier,
            )
        },
    )
}

// Why: the camera stays dark in both themes, so the status bar's icons are light over it until the reader closes.
@Composable
private fun LightStatusBarEffect() {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val wasLight = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = wasLight }
    }
}
