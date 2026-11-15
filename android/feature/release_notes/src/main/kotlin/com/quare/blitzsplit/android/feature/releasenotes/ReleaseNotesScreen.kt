package com.quare.blitzsplit.android.feature.releasenotes

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.feature.releasenotes.presentation.model.ReleaseNotesUiEvent
import com.quare.blitzsplit.feature.releasenotes.presentation.viewmodel.ReleaseNotesViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReleaseNotesScreen(viewModel: ReleaseNotesViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler { viewModel.onEvent(ReleaseNotesUiEvent.OnBackClicked) }
    ReleaseNotesContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}
