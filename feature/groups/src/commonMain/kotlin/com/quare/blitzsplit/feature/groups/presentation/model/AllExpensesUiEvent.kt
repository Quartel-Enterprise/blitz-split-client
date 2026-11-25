package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.ExpensePeriod
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

// Why: the screen these events come from, as their source param says.
internal const val ALL_EXPENSES_SOURCE = "all_expenses"

sealed interface AllExpensesUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back, which each platform also does its own way: where it went is its screen view.
            OnBackClicked -> EventAnalytics.NotTracked

            is OnBillClicked -> EventAnalytics.Automatic("bill_clicked", mapOf("source" to ALL_EXPENSES_SOURCE))

            // Why: a scroll reaching the list's end, not a tap.
            OnEndReached -> EventAnalytics.NotTracked

            // Why: a value typed letter by letter; what it finds shows in the taps that follow.
            is OnQueryChanged -> EventAnalytics.NotTracked

            is OnGroupToggled -> filterChanged("group")

            is OnCategoryToggled -> filterChanged("category")

            // Why: only a period that changed, never "Pick dates…" before its days.
            is OnPeriodPicked -> EventAnalytics.Manual("all_expenses_filter_changed")

            is OnCustomPeriodPicked -> EventAnalytics.Manual("all_expenses_filter_changed")

            is OnShareFilterPicked -> filterChanged("share")

            is OnPayerToggled -> filterChanged("payer")

            OnAnyPayerPicked -> filterChanged("payer")

            is OnSortPicked -> EventAnalytics.Automatic("all_expenses_sort_changed", mapOf("sort" to sort))

            OnClearFiltersClicked -> EventAnalytics.Automatic("all_expenses_filters_cleared")

            is BillMenuEvent -> billMenuAnalyticsOf(this)
        }

    data object OnBackClicked : AllExpensesUiEvent

    // Why: a row, which opens its Conta over its group, as from Início.
    data class OnBillClicked(
        val groupId: String,
        val expenseId: String,
    ) : AllExpensesUiEvent

    // Why: the list scrolled near its last row, which reads the next page from the device.
    data object OnEndReached : AllExpensesUiEvent

    // Why: the search field over the list, which finds bills by their description as the home's search does.
    data class OnQueryChanged(
        val query: String,
    ) : AllExpensesUiEvent

    // Why: a group of the Group filter, picked or let go; several may be picked.
    data class OnGroupToggled(
        val groupId: String,
    ) : AllExpensesUiEvent

    // Why: a main category of the Category filter, picked or let go; several may be picked.
    data class OnCategoryToggled(
        val categoryId: String,
    ) : AllExpensesUiEvent

    // Why: a period of the Period filter; CUSTOM only once its days were picked (OnCustomPeriodPicked).
    data class OnPeriodPicked(
        val period: ExpensePeriod,
    ) : AllExpensesUiEvent

    // Why: "Pick dates…"' days: a moment of the first and the last (noon, as the pickers give them).
    data class OnCustomPeriodPicked(
        val startMillis: Long,
        val endMillis: Long,
    ) : AllExpensesUiEvent

    data class OnShareFilterPicked(
        val share: ShareFilter,
    ) : AllExpensesUiEvent

    // Why: a person of Who paid, picked or let go (FilterPayerOption.key); several may be picked.
    data class OnPayerToggled(
        val key: String,
    ) : AllExpensesUiEvent

    // Why: Who paid's "Anyone", which lets go of every person picked.
    data object OnAnyPayerPicked : AllExpensesUiEvent

    data class OnSortPicked(
        val sort: ExpenseSort,
    ) : AllExpensesUiEvent

    // Why: "Clear filters", beside the filters while one is on and on "No expenses match"; the order stays.
    data object OnClearFiltersClicked : AllExpensesUiEvent
}

private fun filterChanged(filter: String): EventAnalytics =
    EventAnalytics.Automatic("all_expenses_filter_changed", mapOf("source" to ALL_EXPENSES_SOURCE, "filter" to filter))
