package com.quare.blitzsplit.android.feature.creategroup

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.feature.creategroup.presentation.model.CreateGroupUiEvent
import com.quare.blitzsplit.feature.creategroup.presentation.viewmodel.CreateGroupViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreateGroupScreen(
    // Why: anything wider than a phone opens the new group as a dialog over the home, so the screen drops its scaffold.
    isDialog: Boolean,
    viewModel: CreateGroupViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler { viewModel.onEvent(CreateGroupUiEvent.OnCloseClicked) }
    CreateGroupContent(
        state = state,
        isDialog = isDialog,
        onEvent = viewModel::onEvent,
    )
}
