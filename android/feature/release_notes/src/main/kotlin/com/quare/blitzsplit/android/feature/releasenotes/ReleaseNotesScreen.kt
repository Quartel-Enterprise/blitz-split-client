package com.quare.blitzsplit.android.feature.releasenotes

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.feature.releasenotes.presentation.model.ReleaseNotesUiAction
import com.quare.blitzsplit.feature.releasenotes.presentation.model.ReleaseNotesUiEvent
import com.quare.blitzsplit.feature.releasenotes.presentation.viewmodel.ReleaseNotesViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReleaseNotesScreen(viewModel: ReleaseNotesViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(viewModel, uriHandler) {
        viewModel.uiAction.collect { action ->
            when (action) {
                // Why: a device with no browser throws; the tap then does nothing.
                is ReleaseNotesUiAction.OpenUrl -> runCatching { uriHandler.openUri(action.url) }
            }
        }
    }
    BackHandler { viewModel.onEvent(ReleaseNotesUiEvent.OnBackClicked) }
    ReleaseNotesContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}
