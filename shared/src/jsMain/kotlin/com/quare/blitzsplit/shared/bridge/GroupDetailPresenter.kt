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

// Why: amounts in the currency's minor unit and instants in epoch millis, as numbers JavaScript reads exactly.
class JsExpenseRow(
    val id: String,
    val description: String,
    val spentAt: Double,
    val payerName: String?,
    val payerIsMe: Boolean,
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
                isEmpty = state.isEmpty,
                showsSplitBillButton = state.showsSplitBillButton,
                showsBillDeletedNotice = state.showsBillDeletedNotice,
                isLoaded = state.isLoaded,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "coming-soon" and "change-error", a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                GroupDetailUiAction.ShowComingSoon -> "coming-soon"
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

    fun members() {
        viewModel.onEvent(GroupDetailUiEvent.OnMembersClicked)
    }

    fun balances() {
        viewModel.onEvent(GroupDetailUiEvent.OnBalancesTabClicked)
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
    payerName = row.payerName,
    payerIsMe = row.payerIsMe,
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
