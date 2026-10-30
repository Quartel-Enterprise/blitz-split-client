package com.quare.blitzsplit.android.feature.editgroup

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.EditGroupViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditGroupScreen(
    route: Route.EditGroup,
    // Why: a tablet opens the form as a dialog over the group, so the screen drops its own scaffold.
    isDialog: Boolean,
    viewModel: EditGroupViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler { viewModel.onEvent(EditGroupUiEvent.OnCloseClicked) }
    EditGroupContent(
        state = state,
        isDialog = isDialog,
        onEvent = viewModel::onEvent,
    )
}
