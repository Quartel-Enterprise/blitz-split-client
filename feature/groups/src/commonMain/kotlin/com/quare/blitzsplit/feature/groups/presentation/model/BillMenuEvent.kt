package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

/*
 * Why: a bill's ⋮ (the row's long press or right click) on Início's "Últimas despesas" and on "Todas as despesas",
 * which BillMenuController handles as it does on the group's Despesas. Each event says its source when it is sent.
 */
sealed interface BillMenuEvent :
    HomeUiEvent,
    AllExpensesUiEvent {
    override val analytics: EventAnalytics
        get() = billMenuAnalyticsOf(this)

    // Why: "Editar", which opens Editar conta over the list.
    data class OnEditClicked(
        val groupId: String,
        val expenseId: String,
    ) : BillMenuEvent

    // Why: "Duplicar", which opens Rachar conta filled in with the bill.
    data class OnDuplicateClicked(
        val groupId: String,
        val expenseId: String,
    ) : BillMenuEvent

    // Why: "Compartilhar", which opens Compartilhar conta.
    data class OnShareClicked(
        val groupId: String,
        val expenseId: String,
    ) : BillMenuEvent

    // Why: "Apagar", which asks "Apagar esta conta?" first, as Conta does.
    data class OnDeleteClicked(
        val groupId: String,
        val expenseId: String,
    ) : BillMenuEvent

    // Why: "Apagar" on "Apagar esta conta?".
    data object OnDeleteConfirmed : BillMenuEvent

    // Why: "Cancelar" on "Apagar esta conta?", or the dialog closed some other way.
    data object OnDeleteDismissed : BillMenuEvent
}

internal fun billMenuAnalyticsOf(event: BillMenuEvent): EventAnalytics = when (event) {
    // Why: only once the screen opened, as on the group's Despesas: never for a bill gone or a list covered.
    is BillMenuEvent.OnEditClicked -> EventAnalytics.Manual("bill_edit_clicked")

    is BillMenuEvent.OnDuplicateClicked -> EventAnalytics.Manual("bill_duplicate_clicked")

    is BillMenuEvent.OnShareClicked -> EventAnalytics.Manual("bill_share_clicked")

    // Why: only when the question opens, not a second tap before it draws.
    is BillMenuEvent.OnDeleteClicked -> EventAnalytics.Manual("bill_delete_clicked")

    // Why: only an answer to a question still open and not deleting yet.
    BillMenuEvent.OnDeleteConfirmed -> EventAnalytics.Manual("bill_delete_confirmed")

    BillMenuEvent.OnDeleteDismissed -> EventAnalytics.Manual("bill_delete_cancelled")
}
