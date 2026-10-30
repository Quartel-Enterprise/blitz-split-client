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
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiAction
import com.quare.blitzsplit.feature.join.presentation.model.JoinInviteUiEvent
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinInviteViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun JoinInviteScreen(
    route: Route.JoinInvite,
    viewModel: JoinInviteViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
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
                        JoinInviteUiAction.ShowJoinError -> joinError
                    },
                )
            }
        }
    }
    BackHandler { viewModel.onEvent(JoinInviteUiEvent.OnNotNowClicked) }
    JoinInviteContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
}
