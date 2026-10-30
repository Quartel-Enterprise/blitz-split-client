@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupDetailViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

class JsGroupFace(
    val initials: String,
    val color: String,
)

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

class JsExpenseDay(
    val dayStartMillis: Double,
    // Why: "today", "yesterday" or "other", when the page writes the date.
    val relativeDay: String,
    val expenses: Array<JsExpenseRow>,
)

class JsGroupDetailState(
    val groupName: String,
    val groupInitials: String,
    val groupColor: String,
    // Why: the ISO 4217 code ("BRL") and its digits after the decimal separator.
    val currency: String,
    val fractionDigits: Int,
    val faces: Array<JsGroupFace>,
    val extraMemberCount: Int,
    val memberCount: Int,
    val notJoinedCount: Int,
    val totalSpentMinor: Double,
    val days: Array<JsExpenseDay>,
    val isEmpty: Boolean,
    val showsSplitBillButton: Boolean,
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
                groupName = state.groupName,
                groupInitials = state.groupInitials,
                groupColor = state.groupColor.code,
                currency = state.currency.code,
                fractionDigits = state.currency.fractionDigits,
                faces = state.faces.map { JsGroupFace(it.initials, it.color.code) }.toTypedArray(),
                extraMemberCount = state.extraMemberCount,
                memberCount = state.memberCount,
                notJoinedCount = state.notJoinedCount,
                totalSpentMinor = state.totalSpentMinor.toDouble(),
                days = state.days.map(::toJsExpenseDay).toTypedArray(),
                isEmpty = state.isEmpty,
                showsSplitBillButton = state.showsSplitBillButton,
                isLoaded = state.isLoaded,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    // Why: "coming-soon", a toast in the page's words.
    fun observeActions(onAction: (String) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                GroupDetailUiAction.ShowComingSoon -> "coming-soon"
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

    fun openExpense(expenseId: String) {
        viewModel.onEvent(GroupDetailUiEvent.OnExpenseClicked(expenseId))
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsExpenseDay(day: ExpenseDay): JsExpenseDay = JsExpenseDay(
    dayStartMillis = day.dayStartMillis.toDouble(),
    relativeDay = toJsRelativeDay(day.relativeDay),
    expenses = day.expenses.map(::toJsExpenseRow).toTypedArray(),
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

internal fun toJsRelativeDay(day: RelativeDay): String = when (day) {
    RelativeDay.TODAY -> "today"
    RelativeDay.YESTERDAY -> "yesterday"
    RelativeDay.OTHER -> "other"
}
