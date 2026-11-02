package com.quare.blitzsplit.android.feature.categorymap

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiAction
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiEvent
import com.quare.blitzsplit.feature.categorymap.presentation.viewmodel.CategoryMapViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CategoryMapScreen(viewModel: CategoryMapViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveError = stringResource(R.string.category_map_save_error)
    val undo = stringResource(R.string.category_map_undo)
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                CategoryMapUiAction.ShowSaveError -> snackbarHostState.showSnackbar(saveError)
            }
        }
    }
    // Why: "“café” saiu do mapa" with "Desfazer" (9.1d), once per removed term.
    val removed = state.removedTerm
    val removedText = removed?.let { stringResource(R.string.category_map_removed, it.term) }
    LaunchedEffect(removed?.key) {
        if (removedText == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = removedText,
            actionLabel = undo,
            duration = SnackbarDuration.Long,
        )
        val event = if (result == SnackbarResult.ActionPerformed) {
            CategoryMapUiEvent.OnUndoRemoveClicked
        } else {
            CategoryMapUiEvent.OnRemovedNoticeDismissed
        }
        viewModel.onEvent(event)
    }
    BackHandler { viewModel.onEvent(CategoryMapUiEvent.OnBackClicked) }
    CategoryMapContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
}
