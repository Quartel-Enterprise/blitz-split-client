@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.groupdetail.presentation.model.DayEntry
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.PaymentRow
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupDetailViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

// Why: a null name is "Ex-membro".
class JsExpenseRowPayer(
    val name: String?,
    val isMe: Boolean,
)

// Why: amounts in the currency's minor unit and instants in epoch millis, as numbers JavaScript reads exactly.
class JsExpenseRow(
    val id: String,
    val description: String,
    val spentAt: Double,
    // Why: this device's user first, then the group's order, then whoever left it.
    val payers: Array<JsExpenseRowPayer>,
    val totalMinor: Double,
    val myBalanceMinor: Double?,
    val isPending: Boolean,
)

// Why: a payment that settled a debt; a null name is "Ex-membro".
class JsPaymentRow(
    val id: String,
    val paidAt: Double,
    val fromName: String?,
    val fromIsMe: Boolean,
    val toName: String?,
    val toIsMe: Boolean,
    val amountMinor: Double,
    // Why: the SettlementMethod's code: "pix", "cash", "transfer" or "other".
    val method: String,
    val isPending: Boolean,
)

// Why: a line of the day, flat: a bill (expense set) or a payment (payment set).
class JsDayEntry(
    val id: String,
    val expense: JsExpenseRow?,
    val payment: JsPaymentRow?,
)

class JsExpenseDay(
    val dayStartMillis: Double,
    // Why: "today", "yesterday" or "other", when the page writes the date.
    val relativeDay: String,
    // Why: the most recent first, bills and payments mixed by their time.
    val entries: Array<JsDayEntry>,
)

class JsGroupDetailState(
    val header: JsGroupHeader,
    val days: Array<JsExpenseDay>,
    // Why: "Apagar este acerto?", open while not null.
    val paymentToDelete: JsPaymentRow?,
    val recordedPayment: JsPaymentNotice?,
    val exit: JsGroupExit,
    val isEmpty: Boolean,
    val showsSplitBillButton: Boolean,
    val showsBillDeletedNotice: Boolean,
    val isLoaded: Boolean,
)

class GroupDetailPresenter(
    route: JsRoute,
) {
    private val groupRoute = route.route as Route.GroupDetail
    private val holder = ViewModelHolder(GroupDetailViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(groupRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsGroupDetailState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsGroupDetailState(
                header = toJsGroupHeader(state.header),
                days = state.days.map(::toJsExpenseDay).toTypedArray(),
                paymentToDelete = state.paymentToDelete?.let(::toJsPaymentRow),
                recordedPayment = state.recordedPayment?.let(::toJsPaymentNotice),
                exit = toJsGroupExit(state.exit),
                isEmpty = state.isEmpty,
                showsSplitBillButton = state.showsSplitBillButton,
                showsBillDeletedNotice = state.showsBillDeletedNotice,
                isLoaded = state.isLoaded,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "leave-error", "delete-error" and "change-error", a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                GroupDetailUiAction.ShowLeaveError -> "leave-error"
                GroupDetailUiAction.ShowDeleteError -> "delete-error"
                GroupDetailUiAction.ShowChangeError -> "change-error"
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(GroupDetailUiEvent.OnBackClicked)
    }

    fun splitBill() {
        viewModel.onEvent(GroupDetailUiEvent.OnSplitBillClicked)
    }

    fun invite() {
        viewModel.onEvent(GroupDetailUiEvent.OnInviteClicked)
    }

    fun stats() {
        viewModel.onEvent(GroupDetailUiEvent.OnStatsClicked)
    }

    fun members() {
        viewModel.onEvent(GroupDetailUiEvent.OnMembersClicked)
    }

    fun balances() {
        viewModel.onEvent(GroupDetailUiEvent.OnBalancesTabClicked)
    }

    // Why: "Fixar na Início" or "Desafixar da Início", first in the ⋯ menu.
    fun togglePin() {
        viewModel.onEvent(GroupDetailUiEvent.OnPinToggled)
    }

    fun editGroup() {
        viewModel.onEvent(GroupDetailUiEvent.OnEditGroupClicked)
    }

    fun leaveGroup() {
        viewModel.onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)
    }

    fun deleteGroup() {
        viewModel.onEvent(GroupDetailUiEvent.OnDeleteGroupClicked)
    }

    // Why: the main button of the dialog Sair or Apagar opened.
    fun confirmGroupExit() {
        viewModel.onEvent(GroupDetailUiEvent.OnGroupExitConfirmed)
    }

    fun dismissGroupExit() {
        viewModel.onEvent(GroupDetailUiEvent.OnGroupExitDismissed)
    }

    // Why: "Voltar para Grupos" on "Grupo apagado".
    fun backToGroups() {
        viewModel.onEvent(GroupDetailUiEvent.OnBackToGroupsClicked)
    }

    fun openExpense(expenseId: String) {
        viewModel.onEvent(GroupDetailUiEvent.OnExpenseClicked(expenseId))
    }

    fun billDeletedNoticeShown() {
        viewModel.onEvent(GroupDetailUiEvent.OnBillDeletedNoticeShown)
    }

    // Why: "Acertar" on "Seu saldo neste grupo".
    fun settle() {
        viewModel.onEvent(GroupDetailUiEvent.OnSettleClicked)
    }

    fun openPayment(paymentId: String) {
        viewModel.onEvent(GroupDetailUiEvent.OnPaymentClicked(paymentId))
    }

    fun confirmPaymentDelete() {
        viewModel.onEvent(GroupDetailUiEvent.OnPaymentDeleteConfirmed)
    }

    fun dismissPaymentDelete() {
        viewModel.onEvent(GroupDetailUiEvent.OnPaymentDeleteDismissed)
    }

    fun undoPayment() {
        viewModel.onEvent(GroupDetailUiEvent.OnUndoPaymentClicked)
    }

    fun recordedPaymentNoticeShown() {
        viewModel.onEvent(GroupDetailUiEvent.OnRecordedPaymentNoticeShown)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsExpenseDay(day: ExpenseDay): JsExpenseDay = JsExpenseDay(
    dayStartMillis = day.dayStartMillis.toDouble(),
    relativeDay = toJsRelativeDay(day.relativeDay),
    entries = day.entries.map(::toJsDayEntry).toTypedArray(),
)

private fun toJsDayEntry(entry: DayEntry): JsDayEntry = JsDayEntry(
    id = entry.id,
    expense = (entry as? ExpenseRow)?.let(::toJsExpenseRow),
    payment = (entry as? PaymentRow)?.let(::toJsPaymentRow),
)

private fun toJsExpenseRow(row: ExpenseRow): JsExpenseRow = JsExpenseRow(
    id = row.id,
    description = row.description,
    spentAt = row.spentAt.toDouble(),
    payers = row.payers
        .map {
            JsExpenseRowPayer(
                name = it.name,
                isMe = it.isMe,
            )
        }.toTypedArray(),
    totalMinor = row.totalMinor.toDouble(),
    myBalanceMinor = row.myBalanceMinor?.toDouble(),
    isPending = row.isPending,
)

private fun toJsPaymentRow(row: PaymentRow): JsPaymentRow = JsPaymentRow(
    id = row.id,
    paidAt = row.paidAt.toDouble(),
    fromName = row.fromName,
    fromIsMe = row.fromIsMe,
    toName = row.toName,
    toIsMe = row.toIsMe,
    amountMinor = row.amountMinor.toDouble(),
    method = row.method.code,
    isPending = row.isPending,
)

internal fun toJsRelativeDay(day: RelativeDay): String = when (day) {
    RelativeDay.TODAY -> "today"
    RelativeDay.YESTERDAY -> "yesterday"
    RelativeDay.OTHER -> "other"
}
