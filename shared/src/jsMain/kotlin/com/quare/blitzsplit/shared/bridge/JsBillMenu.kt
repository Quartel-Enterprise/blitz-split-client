@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.feature.groups.presentation.model.BillMenuEvent

/*
 * Why: a bill's ⋮ on Início's "Últimas despesas" and on "Todas as despesas" (Editar, Duplicar, Compartilhar and Apagar
 * with its "Apagar esta conta?"), the same on both presenters, which hand it their ViewModel's onEvent.
 */
class JsBillMenu internal constructor(
    private val send: (BillMenuEvent) -> Unit,
) {
    fun edit(
        groupId: String,
        expenseId: String,
    ) {
        send(BillMenuEvent.OnEditClicked(groupId, expenseId))
    }

    fun duplicate(
        groupId: String,
        expenseId: String,
    ) {
        send(BillMenuEvent.OnDuplicateClicked(groupId, expenseId))
    }

    fun share(
        groupId: String,
        expenseId: String,
    ) {
        send(BillMenuEvent.OnShareClicked(groupId, expenseId))
    }

    fun askToDelete(
        groupId: String,
        expenseId: String,
    ) {
        send(BillMenuEvent.OnDeleteClicked(groupId, expenseId))
    }

    fun confirmDelete() {
        send(BillMenuEvent.OnDeleteConfirmed)
    }

    fun dismissDelete() {
        send(BillMenuEvent.OnDeleteDismissed)
    }
}
