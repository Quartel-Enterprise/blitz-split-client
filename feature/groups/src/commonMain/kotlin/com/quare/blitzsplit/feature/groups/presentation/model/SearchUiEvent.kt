package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

/*
 * Why: what "Buscar" (5.2) takes, the same wherever it opens: in Grupos' place (GroupsUiEvent.OnSearch) or over
 * Início, Saldos, Contatos and Atividade (Route.Search).
 */
sealed interface SearchUiEvent : UiEvent {
    data class OnQueryChanged(
        val query: String,
    ) : SearchUiEvent

    // Why: "Tudo", "Grupos", "Pessoas" or "Despesas".
    data class OnScopeChanged(
        val scope: SearchScope,
    ) : SearchUiEvent

    data class OnGroupClicked(
        val groupId: String,
    ) : SearchUiEvent

    // Why: a person found opens the members of the group they were found in.
    data class OnPersonClicked(
        val groupId: String,
    ) : SearchUiEvent

    // Why: a bill found opens over its group, so back goes to the group's bills, as from Despesas.
    data class OnBillClicked(
        val groupId: String,
        val expenseId: String,
    ) : SearchUiEvent

    // Why: back or ✕ on Android, Esc or a click outside on the web, the field cancelled on Apple.
    data object OnClosed : SearchUiEvent
}
