package com.quare.blitzsplit.android.feature.join

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiAction
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiEvent
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinPickViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun JoinPickScreen(
    route: Route.JoinPick,
    viewModel: JoinPickViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val joinError = stringResource(R.string.join_pick_error)
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            snackbarHostState.currentSnackbarData?.dismiss()
            // Why: on its own, so the next action replaces this snackbar instead of waiting until it hides.
            launch {
                snackbarHostState.showSnackbar(
                    when (action) {
                        JoinPickUiAction.ShowJoinError -> joinError
                    },
                )
            }
        }
    }
    BackHandler { viewModel.onEvent(JoinPickUiEvent.OnBackClicked) }
    JoinPickContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
    state.newMember?.let { draft ->
        NewMemberSheet(
            draft = draft,
            canJoin = state.canJoinAsNewMember,
            onEvent = viewModel::onEvent,
        )
    }
}
