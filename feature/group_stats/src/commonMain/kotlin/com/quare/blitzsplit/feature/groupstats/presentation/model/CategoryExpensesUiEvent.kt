package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

// Why: the screen these events come from, as their source param says.
internal const val CATEGORY_EXPENSES_SOURCE = "category_expenses"

sealed interface CategoryExpensesUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back, which each platform also does its own way: where it went is its screen view.
            OnBackClicked -> EventAnalytics.NotTracked

            // Why: a back past the statistics, as the web's breadcrumb goes: where it went is its screen view.
            OnGroupClicked -> EventAnalytics.NotTracked

            // Why: a tab of the home's rail: where it went is its screen view.
            is OnRailTabClicked -> EventAnalytics.NotTracked

            // Why: only a search that opens, never a late tap over what this screen opened.
            OnRailSearchClicked -> EventAnalytics.Manual("search_opened")

            // Why: only a chip of the screen's, with whether it was picked or let go.
            is OnCategoryToggled -> EventAnalytics.Manual("category_filter_toggled")

            // Why: only while a chip is picked, as "Clear" shows only then.
            OnClearClicked -> EventAnalytics.Manual("category_filter_cleared")

            is OnExpenseClicked -> EventAnalytics.Automatic("bill_clicked", mapOf("source" to CATEGORY_EXPENSES_SOURCE))

            // Why: only once the screen opened, as on the group's Expenses: never for a bill gone or a list covered.
            is OnExpenseEditClicked -> EventAnalytics.Manual("bill_edit_clicked")

            is OnExpenseDuplicateClicked -> EventAnalytics.Manual("bill_duplicate_clicked")

            is OnExpenseShareClicked -> EventAnalytics.Manual("bill_share_clicked")

            // Why: only when the question opens, not a second tap before it draws.
            is OnExpenseDeleteClicked -> EventAnalytics.Manual("bill_delete_clicked")

            // Why: only an answer to a question still open and not deleting yet.
            OnExpenseDeleteConfirmed -> EventAnalytics.Manual("bill_delete_confirmed")

            OnExpenseDeleteDismissed -> EventAnalytics.Manual("bill_delete_cancelled")
        }

    data object OnBackClicked : CategoryExpensesUiEvent

    // Why: the group's name in the web's breadcrumb, which goes back to the group past the statistics.
    data object OnGroupClicked : CategoryExpensesUiEvent

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

    // Why: "Edit" on a bill's ⋮ (or its long press, its right click), which opens Edit bill over this list.
    data class OnExpenseEditClicked(
        val expenseId: String,
    ) : CategoryExpensesUiEvent

    // Why: "Duplicate", which opens Split a bill filled in with the bill.
    data class OnExpenseDuplicateClicked(
        val expenseId: String,
    ) : CategoryExpensesUiEvent

    // Why: "Share", which opens Share bill.
    data class OnExpenseShareClicked(
        val expenseId: String,
    ) : CategoryExpensesUiEvent

    // Why: "Delete", which asks "Delete this bill?" first, as Bill does.
    data class OnExpenseDeleteClicked(
        val expenseId: String,
    ) : CategoryExpensesUiEvent

    // Why: "Delete" on "Delete this bill?".
    data object OnExpenseDeleteConfirmed : CategoryExpensesUiEvent

    // Why: "Cancel" on "Delete this bill?", or the dialog closed some other way.
    data object OnExpenseDeleteDismissed : CategoryExpensesUiEvent
}
