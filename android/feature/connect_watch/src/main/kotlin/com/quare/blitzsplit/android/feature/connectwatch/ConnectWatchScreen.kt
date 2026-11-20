package com.quare.blitzsplit.android.feature.connectwatch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.feature.connectwatch.presentation.viewmodel.ConnectWatchViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ConnectWatchScreen(
    // Why: a tablet, a book and a tabletop open it as a dialog over the profile, as Delete account.
    isDialog: Boolean,
    viewModel: ConnectWatchViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ConnectWatchContent(
        state = state,
        isDialog = isDialog,
        onEvent = viewModel::onEvent,
    )
}
