package com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState
import com.quare.blitzsplit.core.expenses.domain.model.debtRowOf
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.feature.groupdetail.presentation.model.BalancesUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.BalancesUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.BalancesUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class BalancesViewModel(
    private val route: Route.Balances,
    useCases: GroupScreenUseCases,
) : ViewModel() {
    val uiState: StateFlow<BalancesUiState>
        field = MutableStateFlow(
            BalancesUiState(
                header = emptyGroupHeader,
                myDebts = emptyList(),
                otherDebts = emptyList(),
                hasBills = false,
                recordedPayment = null,
                exit = GroupExitState.idle,
                isLoaded = false,
            ),
        )

    val uiAction: SharedFlow<BalancesUiAction>
        field = MutableSharedFlow<BalancesUiAction>()

    val stateWatcher: FlowWatcher<BalancesUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<BalancesUiAction> = FlowWatcher(uiAction)

    private val screen = GroupScreen(
        route = route,
        groupId = route.groupId,
        useCases = useCases,
        scope = viewModelScope,
        showExitError = { isDelete ->
            uiAction.emit(if (isDelete) BalancesUiAction.ShowDeleteError else BalancesUiAction.ShowLeaveError)
        },
    )

    init {
        screen.watch(
            show = { group, expenses, _, balances -> show(group, expenses, balances) },
            isLoaded = uiState.map { it.isLoaded },
            showNotice = { notice -> uiState.update { it.copy(recordedPayment = notice) } },
        )
        screen.exit.state
            .onEach { exit -> uiState.update { it.copy(exit = exit) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: BalancesUiEvent) {
        when (event) {
            BalancesUiEvent.OnBackClicked -> screen.leave()

            BalancesUiEvent.OnSplitBillClicked -> screen.openSplitBill()

            BalancesUiEvent.OnInviteClicked -> screen.open(Route.Invite(route.groupId, isNewGroup = false))

            BalancesUiEvent.OnStatsClicked -> screen.open(Route.GroupStats(route.groupId))

            BalancesUiEvent.OnExpensesTabClicked -> screen.openTab(Route.GroupDetail(route.groupId))

            BalancesUiEvent.OnMembersClicked -> screen.openTab(Route.Members(route.groupId, joinedAs = null))

            BalancesUiEvent.OnEditGroupClicked -> screen.open(Route.EditGroup(route.groupId))

            BalancesUiEvent.OnLeaveGroupClicked -> screen.exit.askToLeave()

            BalancesUiEvent.OnDeleteGroupClicked -> screen.exit.askToDelete()

            BalancesUiEvent.OnGroupExitConfirmed -> screen.exit.confirm()

            BalancesUiEvent.OnGroupExitDismissed -> screen.exit.dismiss()

            BalancesUiEvent.OnBackToGroupsClicked -> screen.closeDeletedNotice()

            BalancesUiEvent.OnSettleClicked -> screen.openSettle()

            is BalancesUiEvent.OnDebtClicked -> screen.openRegisterPayment(
                fromMemberId = event.fromMemberId,
                toMemberId = event.toMemberId,
            )

            BalancesUiEvent.OnUndoPaymentClicked -> undoRecordedPayment()

            BalancesUiEvent.OnRecordedPaymentNoticeShown -> uiState.update { it.copy(recordedPayment = null) }
        }
    }

    private fun show(
        group: Group,
        expenses: List<Expense>,
        balances: GroupBalances,
    ) {
        val meId = group.members.firstOrNull { it.isMe }?.id
        val (mine, others) = balances.debts.partition { it.fromMemberId == meId || it.toMemberId == meId }
        uiState.update {
            it.copy(
                header = groupHeaderOf(
                    group = group,
                    totalSpentMinor = expenses.sumOf { expense -> expense.amountMinor },
                    balances = balances,
                ),
                // Why: what this person owes comes first, then what they're owed.
                myDebts = mine
                    .sortedBy { debt -> debt.fromMemberId != meId }
                    .map { debt -> debtRowOf(debt, group, meId) },
                otherDebts = others.map { debt -> debtRowOf(debt, group, meId) },
                hasBills = balances.hasBills,
                isLoaded = true,
            )
        }
    }

    private fun undoRecordedPayment() {
        val notice = uiState.value.recordedPayment ?: return
        uiState.update { it.copy(recordedPayment = null) }
        screen.deletePayment(notice.settlementId) { uiAction.emit(BalancesUiAction.ShowChangeError) }
    }
}
