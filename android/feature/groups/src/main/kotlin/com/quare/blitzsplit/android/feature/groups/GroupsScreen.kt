package com.quare.blitzsplit.android.feature.groups

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quare.blitzsplit.android.designsystem.component.GroupExitAlertDialog
import com.quare.blitzsplit.android.designsystem.component.RecordedPaymentNoticeEffect
import com.quare.blitzsplit.android.designsystem.component.rememberActionSnackbarHostState
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.rememberListDetailLayout
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiEvent
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import org.koin.compose.viewmodel.koinViewModel
import com.quare.blitzsplit.android.designsystem.R as DesignR

/*
 * Why: features never see each other, so the recovery code sheet comes in as a slot the app fills
 * with :android:feature:recovery_code.
 */
@Composable
fun GroupsScreen(
    recoveryCodeSheet: @Composable () -> Unit,
    viewModel: GroupsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberActionSnackbarHostState(viewModel.uiAction) { action ->
        when (action) {
            GroupsUiAction.ShowComingSoon -> R.string.groups_coming_soon
            GroupsUiAction.ShowLeaveError -> DesignR.string.designsystem_group_exit_leave_error
            GroupsUiAction.ShowDeleteError -> DesignR.string.designsystem_group_exit_delete_error
            GroupsUiAction.ShowChangeError -> R.string.groups_change_error
        }
    }
    // Why: state, not an action, so it waits until the home is drawn; the next deleted group follows once it goes.
    val deletedNotice = state.deletedGroupNotice
    val deletedMessage = deletedNotice?.let { notice ->
        notice.deletedByName?.let { stringResource(R.string.groups_deleted_by, it, notice.groupName) }
            ?: stringResource(R.string.groups_deleted, notice.groupName)
    }
    LaunchedEffect(deletedNotice) {
        if (deletedMessage != null) {
            snackbarHostState.showSnackbar(deletedMessage)
            viewModel.onEvent(GroupsUiEvent.OnDeletedGroupNoticeShown)
        }
    }
    // Why: Registrar pagamento, which a row's "Acertar" opened over the home, just closed.
    RecordedPaymentNoticeEffect(
        notice = state.recordedPayment,
        snackbarHostState = snackbarHostState,
        onUndo = { viewModel.onEvent(GroupsUiEvent.OnUndoPaymentClicked) },
        onShown = { viewModel.onEvent(GroupsUiEvent.OnRecordedPaymentNoticeShown) },
    )
    // Why: the window decides whether the group opens beside the list, which the ViewModel needs for taps and search.
    val showsDetailPane = rememberListDetailLayout() != null
    LaunchedEffect(showsDetailPane) { viewModel.onEvent(GroupsUiEvent.OnDetailPaneChanged(showsDetailPane)) }
    // Why: back closes the search before it leaves the home.
    BackHandler(enabled = state.isSearching) { viewModel.onEvent(GroupsUiEvent.OnSearchClosed) }
    GroupsContent(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
    // Why: what "Sair do grupo" and "Apagar grupo" open from a row, the same dialogs as inside the group.
    state.exitGroup?.let { group ->
        GroupExitAlertDialog(
            exit = state.exit,
            money = rememberMoneyFormatter(group.currency),
            onConfirm = { viewModel.onEvent(GroupsUiEvent.OnGroupExitConfirmed) },
            onDismiss = { viewModel.onEvent(GroupsUiEvent.OnGroupExitDismissed) },
            // Why: "Grupo apagado" is for a group open when someone else deleted it, never shown over the home.
            onBackToGroups = { viewModel.onEvent(GroupsUiEvent.OnGroupExitDismissed) },
        )
    }
    if (state.showsRecoveryCodePrompt) recoveryCodeSheet()
}
