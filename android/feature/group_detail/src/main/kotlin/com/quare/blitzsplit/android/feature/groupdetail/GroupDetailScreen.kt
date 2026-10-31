package com.quare.blitzsplit.android.feature.groupdetail

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.RecordedPaymentNoticeEffect
import com.quare.blitzsplit.android.designsystem.component.showReplacingSnackbar
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupDetailViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupDetailScreen(
    route: Route.GroupDetail,
    viewModel: GroupDetailViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val comingSoon = stringResource(R.string.group_detail_coming_soon)
    val billDeleted = stringResource(R.string.group_detail_bill_deleted)
    val scope = rememberCoroutineScope()
    val showSnackbar = { message: String -> scope.showReplacingSnackbar(snackbarHostState, message) }
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                GroupDetailUiAction.ShowComingSoon -> showSnackbar(comingSoon)
            }
        }
    }
    // Why: state, not an action, so it waits for this screen while the bill's screen still covers it.
    LaunchedEffect(state.showsBillDeletedNotice) {
        if (state.showsBillDeletedNotice) {
            showSnackbar(billDeleted)
            viewModel.onEvent(GroupDetailUiEvent.OnBillDeletedNoticeShown)
        }
    }
    RecordedPaymentNoticeEffect(
        notice = state.recordedPayment,
        snackbarHostState = snackbarHostState,
        onUndo = { viewModel.onEvent(GroupDetailUiEvent.OnUndoPaymentClicked) },
        onShown = { viewModel.onEvent(GroupDetailUiEvent.OnRecordedPaymentNoticeShown) },
    )
    BackHandler { viewModel.onEvent(GroupDetailUiEvent.OnBackClicked) }
    GroupDetailContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
    if (state.paymentToDelete != null) {
        DeletePaymentDialog(onEvent = viewModel::onEvent)
    }
}
