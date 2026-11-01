package com.quare.blitzsplit.android.feature.groupstats

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiAction
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.viewmodel.GroupStatsViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.quare.blitzsplit.android.designsystem.R as DesignR

@Composable
fun GroupStatsScreen(
    route: Route.GroupStats,
    viewModel: GroupStatsViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberActionSnackbarHostState(viewModel.uiAction) { action ->
        when (action) {
            GroupStatsUiAction.ShowComingSoon -> DesignR.string.designsystem_coming_soon
        }
    }
    var isPickingDates by rememberSaveable { mutableStateOf(false) }
    BackHandler { viewModel.onEvent(GroupStatsUiEvent.OnBackClicked) }
    GroupStatsContent(
        state = state,
        onEvent = viewModel::onEvent,
        onPickDates = { isPickingDates = true },
        snackbarHostState = snackbarHostState,
    )
    if (isPickingDates) {
        StatsDateRangeDialog(
            startMillis = state.periodStartMillis,
            endMillis = state.periodEndMillis,
            onPicked = { start, end ->
                isPickingDates = false
                viewModel.onEvent(GroupStatsUiEvent.OnCustomPeriodPicked(start, end))
            },
            onDismiss = { isPickingDates = false },
        )
    }
}
