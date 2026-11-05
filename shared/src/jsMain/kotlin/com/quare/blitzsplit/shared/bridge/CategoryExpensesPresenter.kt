@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpenseRow
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpensesUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpensesUiState
import com.quare.blitzsplit.feature.groupstats.presentation.viewmodel.CategoryExpensesViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

// Why: a chip of "Expenses by category".
class JsCategoryFilterChip(
    val category: JsCategory,
    val isSelected: Boolean,
)

// Why: the bills of one day, drawn with the group's Expenses' rows.
class JsCategoryExpenseDay(
    val dayStartMillis: Double,
    // Why: "today", "yesterday" or "other", when the page writes the date.
    val relativeDay: String,
    val expenses: Array<JsExpenseRow>,
)

/*
 * Why: "Expenses by category". The period's days are null for the whole history; periodRangeStyle is "month", "days"
 * or "months".
 */
class JsCategoryExpensesState(
    val groupName: String,
    // Why: the ISO 4217 code ("BRL") and its digits after the decimal separator.
    val currency: String,
    val fractionDigits: Int,
    val isTrip: Boolean,
    val periodStartMillis: Double?,
    val periodEndMillis: Double?,
    val periodRangeStyle: String,
    val chips: Array<JsCategoryFilterChip>,
    val days: Array<JsCategoryExpenseDay>,
    val totalMinor: Double,
    val expenseCount: Int,
    val showsClear: Boolean,
    val isEmpty: Boolean,
    val isLoaded: Boolean,
    // Why: the home tab the group was opened from, which the sidebar selects ("home", "groups"…).
    val homeTab: String,
)

class CategoryExpensesPresenter(
    route: JsRoute,
) {
    private val categoryRoute = route.route as Route.CategoryExpenses
    private val holder = ViewModelHolder(CategoryExpensesViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(categoryRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsCategoryExpensesState) -> Unit): JsWatch {
        val states = viewModel.uiState.map(::toJsCategoryExpensesState)
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(CategoryExpensesUiEvent.OnBackClicked)
    }

    fun openGroup() {
        viewModel.onEvent(CategoryExpensesUiEvent.OnGroupClicked)
    }

    fun toggleCategory(categoryId: String) {
        viewModel.onEvent(CategoryExpensesUiEvent.OnCategoryToggled(categoryId))
    }

    fun clear() {
        viewModel.onEvent(CategoryExpensesUiEvent.OnClearClicked)
    }

    fun openExpense(expenseId: String) {
        viewModel.onEvent(CategoryExpensesUiEvent.OnExpenseClicked(expenseId))
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsCategoryExpensesState(state: CategoryExpensesUiState): JsCategoryExpensesState =
    JsCategoryExpensesState(
        groupName = state.groupName,
        currency = state.currency.code,
        fractionDigits = state.currency.fractionDigits,
        isTrip = state.isTrip,
        periodStartMillis = state.periodStartMillis?.toDouble(),
        periodEndMillis = state.periodEndMillis?.toDouble(),
        periodRangeStyle = state.periodRangeStyle.name.lowercase(),
        chips = state.chips.map { JsCategoryFilterChip(toJsCategory(it.category), it.isSelected) }.toTypedArray(),
        days = state.days
            .map { day ->
                JsCategoryExpenseDay(
                    dayStartMillis = day.dayStartMillis.toDouble(),
                    relativeDay = toJsRelativeDay(day.relativeDay),
                    expenses = day.expenses.map(::toJsCategoryExpenseRow).toTypedArray(),
                )
            }.toTypedArray(),
        totalMinor = state.totalMinor.toDouble(),
        expenseCount = state.expenseCount,
        showsClear = state.showsClear,
        isEmpty = state.isEmpty,
        isLoaded = state.isLoaded,
        homeTab = toJsHomeTab(state.homeTab),
    )

// Why: the group's Expenses' row as the page draws it, which its table takes as it is.
private fun toJsCategoryExpenseRow(row: CategoryExpenseRow): JsExpenseRow = with(row) {
    JsExpenseRow(
        id,
        description,
        spentAt.toDouble(),
        payers.map { JsExpenseRowPayer(it.name, it.isMe) }.toTypedArray(),
        totalMinor.toDouble(),
        myBalanceMinor?.toDouble(),
        toJsCategory(category),
        isPending,
    )
}
