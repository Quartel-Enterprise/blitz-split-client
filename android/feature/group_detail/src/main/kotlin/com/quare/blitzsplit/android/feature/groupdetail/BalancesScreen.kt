package com.quare.blitzsplit.android.feature.groupdetail

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.GroupExitAlertDialog
import com.quare.blitzsplit.android.designsystem.component.RecordedPaymentNoticeEffect
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.groupdetail.presentation.model.BalancesUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.BalancesUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.BalancesViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.quare.blitzsplit.android.designsystem.R as DesignR

// Why: "Saldos", the group's tab with who owes whom.
@Composable
fun BalancesScreen(
    route: Route.Balances,
    viewModel: BalancesViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberActionSnackbarHostState(viewModel.uiAction) { action ->
        when (action) {
            BalancesUiAction.ShowLeaveError -> DesignR.string.designsystem_group_exit_leave_error
            BalancesUiAction.ShowDeleteError -> DesignR.string.designsystem_group_exit_delete_error
            BalancesUiAction.ShowChangeError -> R.string.group_detail_change_error
        }
    }
    RecordedPaymentNoticeEffect(
        notice = state.recordedPayment,
        snackbarHostState = snackbarHostState,
        onUndo = { viewModel.onEvent(BalancesUiEvent.OnUndoPaymentClicked) },
        onShown = { viewModel.onEvent(BalancesUiEvent.OnRecordedPaymentNoticeShown) },
    )
    BackHandler { viewModel.onEvent(BalancesUiEvent.OnBackClicked) }
    BalancesContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
    GroupExitAlertDialog(
        exit = state.exit,
        money = rememberMoneyFormatter(state.header.currency),
        onConfirm = { viewModel.onEvent(BalancesUiEvent.OnGroupExitConfirmed) },
        onDismiss = { viewModel.onEvent(BalancesUiEvent.OnGroupExitDismissed) },
        onBackToGroups = { viewModel.onEvent(BalancesUiEvent.OnBackToGroupsClicked) },
    )
}
