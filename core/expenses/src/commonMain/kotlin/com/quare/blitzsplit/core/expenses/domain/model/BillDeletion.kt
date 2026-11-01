package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: "Apagar esta conta?", from Conta and from Editar conta: "“Mercado” sai do grupo e a parte de cada um nesta
 * conta deixa de contar", and what it meant for this device's user ("Você deixa de dever R$ 118,00").
 */
data class BillDeletion(
    val description: String,
    // Why: Expense.balanceOf for this device's user, null when they aren't in the bill.
    val myBalanceMinor: Long?,
    // Why: a payment made after the bill that deleting it unsettles (7.4b), null when there is none.
    val settledPair: SettledPairChange?,
) {
    companion object {
        fun of(
            expense: Expense,
            myMemberId: String?,
            settledPair: SettledPairChange?,
        ): BillDeletion = BillDeletion(
            description = expense.description,
            myBalanceMinor = myMemberId?.let(expense::balanceOf),
            settledPair = settledPair,
        )
    }
}
