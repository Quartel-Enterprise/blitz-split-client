package com.quare.blitzsplit.android.feature.members

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiAction
import com.quare.blitzsplit.feature.members.presentation.model.MembersUiEvent
import com.quare.blitzsplit.feature.members.presentation.viewmodel.MembersViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MembersScreen(
    route: Route.Members,
    viewModel: MembersViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val comingSoon = stringResource(R.string.members_coming_soon)
    val changeError = stringResource(R.string.members_change_error)
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            snackbarHostState.currentSnackbarData?.dismiss()
            // Why: on its own, so the next action replaces this snackbar instead of waiting until it hides.
            launch {
                snackbarHostState.showSnackbar(
                    when (action) {
                        MembersUiAction.ShowComingSoon -> comingSoon
                        MembersUiAction.ShowChangeError -> changeError
                    },
                )
            }
        }
    }
    BackHandler { viewModel.onEvent(MembersUiEvent.OnBackClicked) }
    MembersContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
    state.editor?.let { editor ->
        MemberNameDialog(
            editor = editor,
            onEvent = viewModel::onEvent,
        )
    }
    state.memberToRemove?.let { member ->
        RemoveMemberDialog(
            name = member.name,
            onEvent = viewModel::onEvent,
        )
    }
}
