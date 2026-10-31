package com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettlements
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.feature.groupdetail.presentation.model.BalancesUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.BalancesUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.BalancesUiState
import com.quare.blitzsplit.feature.groupdetail.presentation.model.DebtKind
import com.quare.blitzsplit.feature.groupdetail.presentation.model.DebtRow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BalancesViewModel(
    private val route: Route.Balances,
    private val deleteSettlement: DeleteSettlement,
    navigator: Navigator,
    consumeRecordedPayment: ConsumeRecordedPayment,
    observeGroup: ObserveGroup,
    observeExpenses: ObserveExpenses,
    observeSettlements: ObserveSettlements,
    observeRecordedPayment: ObserveRecordedPayment,
) : ViewModel() {
    val uiState: StateFlow<BalancesUiState>
        field = MutableStateFlow(
            BalancesUiState(
                header = emptyGroupHeader,
                myDebts = emptyList(),
                otherDebts = emptyList(),
                hasBills = false,
                recordedPayment = null,
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
        navigator = navigator,
    )

    init {
        screen.watch(
            scope = viewModelScope,
            group = observeGroup(route.groupId),
            expenses = observeExpenses(route.groupId),
            settlements = observeSettlements(route.groupId),
            show = { group, expenses, _, balances -> show(group, expenses, balances) },
        )
        screen.watchRecordedPayment(
            scope = viewModelScope,
            recorded = observeRecordedPayment(route.groupId),
            isLoaded = uiState.map { it.isLoaded },
            consume = consumeRecordedPayment,
            show = { notice -> uiState.update { it.copy(recordedPayment = notice) } },
        )
    }

    fun onEvent(event: BalancesUiEvent) {
        when (event) {
            BalancesUiEvent.OnBackClicked -> screen.leave()

            BalancesUiEvent.OnSplitBillClicked -> screen.openSplitBill()

            BalancesUiEvent.OnInviteClicked -> screen.open(Route.Invite(route.groupId))

            BalancesUiEvent.OnExpensesTabClicked -> screen.openTab(Route.GroupDetail(route.groupId))

            BalancesUiEvent.OnMembersClicked -> screen.openTab(Route.Members(route.groupId, joinedAs = null))

            BalancesUiEvent.OnEditGroupClicked -> screen.open(Route.EditGroup(route.groupId))

            BalancesUiEvent.OnLeaveGroupClicked,
            BalancesUiEvent.OnDeleteGroupClicked,
            -> viewModelScope.launch { uiAction.emit(BalancesUiAction.ShowComingSoon) }

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
                    .map { debt -> toRow(debt, group, meId) },
                otherDebts = others.map { debt -> toRow(debt, group, meId) },
                hasBills = balances.hasBills,
                isLoaded = true,
            )
        }
    }

    private fun toRow(
        debt: Debt,
        group: Group,
        meId: String?,
    ): DebtRow {
        val from = group.members.firstOrNull { it.id == debt.fromMemberId }
        val to = group.members.firstOrNull { it.id == debt.toMemberId }
        return DebtRow(
            fromMemberId = debt.fromMemberId,
            toMemberId = debt.toMemberId,
            fromName = from?.name,
            fromFace = from?.let(::faceOf),
            toName = to?.name,
            toFace = to?.let(::faceOf),
            amountMinor = debt.amountMinor,
            kind = when (meId) {
                debt.fromMemberId -> DebtKind.YOU_OWE
                debt.toMemberId -> DebtKind.OWES_YOU
                else -> DebtKind.BETWEEN_OTHERS
            },
        )
    }

    private fun undoRecordedPayment() {
        val notice = uiState.value.recordedPayment ?: return
        uiState.update { it.copy(recordedPayment = null) }
        viewModelScope.launch { deleteSettlement(notice.settlementId) }
    }
}
