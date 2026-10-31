package com.quare.blitzsplit.android.feature.join

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiAction
import com.quare.blitzsplit.feature.join.presentation.model.JoinPickUiEvent
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinPickViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun JoinPickScreen(
    route: Route.JoinPick,
    viewModel: JoinPickViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberActionSnackbarHostState(viewModel.uiAction) { action ->
        when (action) {
            JoinPickUiAction.ShowJoinError -> R.string.join_pick_error
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
