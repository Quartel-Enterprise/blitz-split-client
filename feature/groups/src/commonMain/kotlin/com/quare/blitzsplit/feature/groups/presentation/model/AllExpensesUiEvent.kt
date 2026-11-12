package com.quare.blitzsplit.feature.groups.presentation.model

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
}
