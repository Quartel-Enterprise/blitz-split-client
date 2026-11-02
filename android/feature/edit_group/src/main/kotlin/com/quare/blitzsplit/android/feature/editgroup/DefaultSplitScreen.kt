package com.quare.blitzsplit.android.feature.editgroup

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.DefaultSplitViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DefaultSplitScreen(
    route: Route.DefaultSplit,
    // Why: a tablet and a book draw "Divisão padrão" as a dialog over the group, its exception as one over it.
    isDialog: Boolean,
    viewModel: DefaultSplitViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler { viewModel.onEvent(DefaultSplitUiEvent.OnBackClicked) }
    DefaultSplitContent(
        state = state,
        isDialog = isDialog,
        onEvent = viewModel::onEvent,
    )
    state.exceptionEditor?.let { editor ->
        ExceptionSheet(
            state = state,
            editor = editor,
            money = rememberMoneyFormatter(state.currency),
            isDialog = isDialog,
            onEvent = viewModel::onEvent,
        )
    }
}
