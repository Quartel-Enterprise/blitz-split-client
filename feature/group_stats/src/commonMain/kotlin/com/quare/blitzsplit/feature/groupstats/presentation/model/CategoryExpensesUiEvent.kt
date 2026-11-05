package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "category_expenses"

sealed interface CategoryExpensesUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back, which each platform also does its own way: where it went is its screen view.
            OnBackClicked -> EventAnalytics.NotTracked

            // Why: a tab of the home's rail: where it went is its screen view.
            is OnRailTabClicked -> EventAnalytics.NotTracked

            // Why: only a search that opens, never a late tap over what this screen opened.
            OnRailSearchClicked -> EventAnalytics.Manual("search_opened")

            // Why: only a chip of the screen's, with whether it was picked or let go.
            is OnCategoryToggled -> EventAnalytics.Manual("category_filter_toggled")

            // Why: only while a chip is picked, as "Clear" shows only then.
            OnClearClicked -> EventAnalytics.Manual("category_filter_cleared")

            is OnExpenseClicked -> EventAnalytics.Automatic("bill_clicked", mapOf("source" to SOURCE))
        }

    data object OnBackClicked : CategoryExpensesUiEvent

    // Why: a tab of the home's rail, which Android keeps beside the group's screens.
    data class OnRailTabClicked(
        val tab: HomeTab,
    ) : CategoryExpensesUiEvent

    // Why: the rail's last item, "Search", over the tab the group was opened from.
    data object OnRailSearchClicked : CategoryExpensesUiEvent

    // Why: a chip, which adds its category's bills to the list or takes them out.
    data class OnCategoryToggled(
        val categoryId: String,
    ) : CategoryExpensesUiEvent

    // Why: "Clear", which lets go of every chip, so the list shows every bill of the period.
    data object OnClearClicked : CategoryExpensesUiEvent

    // Why: a line of the list, which opens the bill as the group's Expenses does.
    data class OnExpenseClicked(
        val expenseId: String,
    ) : CategoryExpensesUiEvent
}
