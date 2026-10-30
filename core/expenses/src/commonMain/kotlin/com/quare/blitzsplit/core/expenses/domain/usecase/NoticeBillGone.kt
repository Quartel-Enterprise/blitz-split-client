package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface NoticeBillGone {
    /*
     * Why: a bill open on "Conta" or "Editar conta" went: unless this device deleted it, Despesas do grupo says "Esta
     * conta foi apagada" once it is back on top.
     */
    operator fun invoke(
        groupId: String,
        expenseId: String,
    )
}
