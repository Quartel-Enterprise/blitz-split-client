package com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeDeletedBillNotice
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveDeletedBillNotice
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.PayerOrder
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.groupdetail.presentation.model.DayEntry
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRowPayer
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiState
import com.quare.blitzsplit.feature.groupdetail.presentation.model.PaymentRow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class GroupDetailViewModel(
    private val route: Route.GroupDetail,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    private val consumeDeletedBillNotice: ConsumeDeletedBillNotice,
    observeDeletedBillNotice: ObserveDeletedBillNotice,
    useCases: GroupScreenUseCases,
) : ViewModel() {
    val uiState: StateFlow<GroupDetailUiState>
        field = MutableStateFlow(
            GroupDetailUiState(
                header = emptyGroupHeader,
                days = emptyList(),
                paymentToDelete = null,
                recordedPayment = null,
                showsBillDeletedNotice = false,
                exit = GroupExitState.idle,
                isLoaded = false,
            ),
        )

    val uiAction: SharedFlow<GroupDetailUiAction>
        field = MutableSharedFlow<GroupDetailUiAction>()

    val stateWatcher: FlowWatcher<GroupDetailUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<GroupDetailUiAction> = FlowWatcher(uiAction)

    private val screen = GroupScreen(
        route = route,
        groupId = route.groupId,
        useCases = useCases,
        scope = viewModelScope,
        showExitError = { isDelete ->
            uiAction.emit(if (isDelete) GroupDetailUiAction.ShowDeleteError else GroupDetailUiAction.ShowLeaveError)
        },
    )

    init {
        screen.watch(
            show = ::show,
            isLoaded = uiState.map { it.isLoaded },
            showNotice = { notice -> uiState.update { it.copy(recordedPayment = notice) } },
        )
        screen.exit.state
            .onEach { exit -> uiState.update { it.copy(exit = exit) } }
            .launchIn(viewModelScope)
        // Why: the notice waits until this screen is on top again, under the bill's screen that closed.
        combine(observeDeletedBillNotice(route.groupId), useCases.navigator.backStack, ::Pair)
            .onEach { (isOwed, _) -> if (isOwed && screen.isOnTop()) showBillDeleted() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: GroupDetailUiEvent) {
        when (event) {
            GroupDetailUiEvent.OnBackClicked -> screen.leave()

            GroupDetailUiEvent.OnSplitBillClicked -> screen.openSplitBill()

            is GroupDetailUiEvent.OnExpenseClicked -> screen.open(
                Route.BillDetail(
                    groupId = route.groupId,
                    expenseId = event.expenseId,
                ),
            )

            GroupDetailUiEvent.OnInviteClicked -> screen.open(Route.Invite(route.groupId, isNewGroup = false))

            GroupDetailUiEvent.OnStatsClicked -> screen.open(Route.GroupStats(route.groupId))

            GroupDetailUiEvent.OnMembersClicked -> screen.openTab(Route.Members(route.groupId, joinedAs = null))

            GroupDetailUiEvent.OnBalancesTabClicked -> screen.openTab(Route.Balances(route.groupId))

            GroupDetailUiEvent.OnEditGroupClicked -> screen.open(Route.EditGroup(route.groupId))

            GroupDetailUiEvent.OnLeaveGroupClicked -> screen.exit.askToLeave()

            GroupDetailUiEvent.OnDeleteGroupClicked -> screen.exit.askToDelete()

            GroupDetailUiEvent.OnGroupExitConfirmed -> screen.exit.confirm()

            GroupDetailUiEvent.OnGroupExitDismissed -> screen.exit.dismiss()

            GroupDetailUiEvent.OnBackToGroupsClicked -> screen.closeDeletedNotice()

            GroupDetailUiEvent.OnBillDeletedNoticeShown -> uiState.update { it.copy(showsBillDeletedNotice = false) }

            GroupDetailUiEvent.OnSettleClicked -> screen.openSettle()

            is GroupDetailUiEvent.OnPaymentClicked -> askToDeletePayment(event.paymentId)

            GroupDetailUiEvent.OnPaymentDeleteConfirmed -> deletePayment()

            GroupDetailUiEvent.OnPaymentDeleteDismissed -> uiState.update { it.copy(paymentToDelete = null) }

            GroupDetailUiEvent.OnUndoPaymentClicked -> undoRecordedPayment()

            GroupDetailUiEvent.OnRecordedPaymentNoticeShown -> uiState.update { it.copy(recordedPayment = null) }
        }
    }

    private fun show(
        group: Group,
        expenses: List<Expense>,
        settlements: List<Settlement>,
        balances: GroupBalances,
    ) {
        val timeZone = currentTimeZone()
        val now = currentTimeMillis()
        val namesById = group.members.associate { it.id to it.name }
        val meId = group.members.firstOrNull { it.isMe }?.id
        val payerOrder = PayerOrder(group.members)
        val entries = expenses.map { it.spentAt to toRow(it, namesById, meId, payerOrder) } +
            settlements.map { it.paidAt to toRow(it, namesById, meId) }
        val days = entries
            .sortedByDescending { (time, _) -> time }
            .groupBy { (time, _) -> SpentDay.dateOf(time, timeZone) }
            .map { (date, dayEntries) ->
                ExpenseDay(
                    dayStartMillis = SpentDay.startOf(date, timeZone),
                    relativeDay = SpentDay.relativeDayOf(date, now, timeZone),
                    entries = dayEntries.map { (_, entry) -> entry },
                )
            }
        uiState.update {
            it.copy(
                header = groupHeaderOf(
                    group = group,
                    totalSpentMinor = expenses.sumOf { expense -> expense.amountMinor },
                    balances = balances,
                ),
                days = days,
                // Why: the payment as it is now; one someone else undid meanwhile has nothing left to delete.
                paymentToDelete = it.paymentToDelete?.let { asked -> paymentRowOrNull(days, asked.id) },
                isLoaded = true,
            )
        }
    }

    private fun toRow(
        expense: Expense,
        namesById: Map<String, String>,
        meId: String?,
        payerOrder: PayerOrder,
    ): DayEntry {
        val payers = payerOrder.sort(expense.payers.map { it.memberId }).map { id ->
            ExpenseRowPayer(
                name = namesById[id],
                isMe = id == meId,
            )
        }
        return ExpenseRow(
            id = expense.id,
            description = expense.description,
            spentAt = expense.spentAt,
            payers = payers,
            totalMinor = expense.amountMinor,
            myBalanceMinor = meId?.let(expense::balanceOf),
            isPending = expense.isPending,
        )
    }

    private fun toRow(
        settlement: Settlement,
        namesById: Map<String, String>,
        meId: String?,
    ): DayEntry = PaymentRow(
        id = settlement.id,
        paidAt = settlement.paidAt,
        fromName = namesById[settlement.fromMemberId],
        fromIsMe = settlement.fromMemberId == meId,
        toName = namesById[settlement.toMemberId],
        toIsMe = settlement.toMemberId == meId,
        amountMinor = settlement.amountMinor,
        method = settlement.method,
        isPending = settlement.isPending,
    )

    private fun askToDeletePayment(paymentId: String) {
        val payment = paymentRowOrNull(uiState.value.days, paymentId) ?: return
        uiState.update { it.copy(paymentToDelete = payment) }
    }

    private fun paymentRowOrNull(
        days: List<ExpenseDay>,
        paymentId: String,
    ): PaymentRow? = days
        .asSequence()
        .flatMap { it.entries }
        .filterIsInstance<PaymentRow>()
        .firstOrNull { it.id == paymentId }

    private fun deletePayment() {
        val payment = uiState.value.paymentToDelete ?: return
        uiState.update { it.copy(paymentToDelete = null) }
        screen.deletePayment(payment.id) { uiAction.emit(GroupDetailUiAction.ShowChangeError) }
    }

    private fun showBillDeleted() {
        consumeDeletedBillNotice(route.groupId)
        uiState.update { it.copy(showsBillDeletedNotice = true) }
    }

    private fun undoRecordedPayment() {
        val notice = uiState.value.recordedPayment ?: return
        uiState.update { it.copy(recordedPayment = null) }
        screen.deletePayment(notice.settlementId) { uiAction.emit(GroupDetailUiAction.ShowChangeError) }
    }
}
