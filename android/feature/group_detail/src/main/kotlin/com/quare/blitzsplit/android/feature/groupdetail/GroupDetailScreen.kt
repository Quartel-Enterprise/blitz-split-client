package com.quare.blitzsplit.android.feature.groupdetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.DeleteBillDialog
import com.quare.blitzsplit.android.designsystem.component.GroupExitAlertDialog
import com.quare.blitzsplit.android.designsystem.component.RecordedPaymentNoticeEffect
import com.quare.blitzsplit.android.designsystem.component.SimplifyConflictNoticeEffect
import com.quare.blitzsplit.android.designsystem.component.showReplacingSnackbar
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupDetailViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import com.quare.blitzsplit.android.designsystem.R as DesignR

/*
 * Why: one entry of the back stack for the whole group: picking Despesas, Saldos or Membros changes this screen's state,
 * so back from any of them leaves the group. Membros comes from another module, asked for on every tab so it is read
 * by the time it is picked.
 */
@Composable
fun GroupDetailScreen(
    route: Route.GroupDetail,
    membersTab: @Composable (SnackbarHostState) -> LazyListScope.() -> Unit,
    /*
     * Why: the tab picked, for the app: a tabletop lying down draws Saldos alone, full screen, where the other tabs
     * stay beside the list (6.2).
     */
    onTabChanged: (GroupTab) -> Unit = {},
    viewModel: GroupDetailViewModel = koinViewModel(key = route.toString()) { parametersOf(route) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val leaveError = stringResource(DesignR.string.designsystem_group_exit_leave_error)
    val changeError = stringResource(R.string.group_detail_change_error)
    val billDeleted = stringResource(R.string.group_detail_bill_deleted)
    val teachError = stringResource(R.string.group_detail_teach_error)
    val billDeleteError = stringResource(DesignR.string.designsystem_delete_bill_error)
    val scope = rememberCoroutineScope()
    val showSnackbar = { message: String -> scope.showReplacingSnackbar(snackbarHostState, message) }
    LaunchedEffect(viewModel) {
        viewModel.uiAction.collect { action ->
            when (action) {
                GroupDetailUiAction.ShowLeaveError -> showSnackbar(leaveError)
                GroupDetailUiAction.ShowChangeError -> showSnackbar(changeError)
                GroupDetailUiAction.ShowTeachError -> showSnackbar(teachError)
                GroupDetailUiAction.ShowBillDeleteError -> showSnackbar(billDeleteError)
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
    SimplifyConflictNoticeEffect(
        notice = state.simplifyConflict,
        snackbarHostState = snackbarHostState,
        onActivityClick = { viewModel.onEvent(GroupDetailUiEvent.OnSimplifyConflictActivityClicked) },
        onDismiss = { viewModel.onEvent(GroupDetailUiEvent.OnSimplifyConflictDismissed) },
    )
    CategoryTeachNoticeEffect(
        teach = state.teach,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
    val currentOnTabChanged by rememberUpdatedState(onTabChanged)
    /*
     * Why: a tab tapped reaches the app in the tap, with the ViewModel, so a tabletop's scene changes in the same frame
     * as the tab instead of drawing the old layout once; the effect covers the tab the group opens on.
     */
    val onEvent: (GroupDetailUiEvent) -> Unit = remember(viewModel) {
        { event ->
            if (event is GroupDetailUiEvent.OnTabSelected) currentOnTabChanged(event.tab)
            viewModel.onEvent(event)
        }
    }
    LaunchedEffect(state.tab) { currentOnTabChanged(state.tab) }
    BackHandler { viewModel.onEvent(GroupDetailUiEvent.OnBackClicked) }
    GroupDetailContent(
        state = state,
        snackbarHostState = snackbarHostState,
        membersTab = membersTab(snackbarHostState),
        onEvent = onEvent,
        sharedGroupId = route.groupId,
    )
    state.teach?.takeIf { it.isPickingWord }?.let {
        CategoryTeachDialog(
            teach = it,
            onEvent = viewModel::onEvent,
        )
    }
    state.paymentToView?.let { payment ->
        PaymentDetailsSheet(
            payment = payment,
            onEvent = viewModel::onEvent,
        )
    }
    if (state.paymentToDelete != null) {
        DeletePaymentDialog(onEvent = viewModel::onEvent)
    }
    // Why: a bill's ⋮ "Apagar" asks what Conta asks, in the same words.
    state.billToDelete?.let { deletion ->
        DeleteBillDialog(
            description = deletion.description,
            myBalanceMinor = deletion.myBalanceMinor,
            settledPair = deletion.settledPair,
            isDeleting = state.isDeletingBill,
            currency = deletion.currency,
            onConfirm = { viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteConfirmed) },
            onDismiss = { viewModel.onEvent(GroupDetailUiEvent.OnExpenseDeleteDismissed) },
        )
    }
    GroupExitAlertDialog(
        exit = state.exit,
        onConfirm = { viewModel.onEvent(GroupDetailUiEvent.OnGroupExitConfirmed) },
        onDismiss = { viewModel.onEvent(GroupDetailUiEvent.OnGroupExitDismissed) },
        onBackToGroups = { viewModel.onEvent(GroupDetailUiEvent.OnBackToGroupsClicked) },
    )
}
