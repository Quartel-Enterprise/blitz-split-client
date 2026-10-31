package com.quare.blitzsplit.android.feature.join

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiAction
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiEvent
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinInviteViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun JoinInviteScreen(
    route: Route.JoinInvite,
    viewModel: JoinInviteViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberActionSnackbarHostState(viewModel.uiAction) { action ->
        when (action) {
            JoinInviteUiAction.ShowJoinError -> R.string.join_pick_error
        }
    }
    BackHandler { viewModel.onEvent(JoinInviteUiEvent.OnNotNowClicked) }
    JoinInviteContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
}
