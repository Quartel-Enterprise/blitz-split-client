package com.quare.blitzsplit.core.expenses.domain.model

// Why: "Dívidas simplificadas · 5 pagamentos em vez de 7" (13.4), while the group's "Simplificar dívidas" is on.
data class DebtSimplificationSummary(
    val paymentCount: Int,
    val unsimplifiedPaymentCount: Int,
) {
    // Why: "Já está no mínimo de pagamentos" instead, while simplifying saves none.
    val isAlreadyMinimal: Boolean
        get() = paymentCount >= unsimplifiedPaymentCount

    companion object {
        fun ofOrNull(balances: GroupBalances): DebtSimplificationSummary? = balances
            .takeIf { it.isSimplified }
            ?.let {
                DebtSimplificationSummary(
                    paymentCount = it.debts.size,
                    unsimplifiedPaymentCount = it.unsimplifiedCount,
                )
            }
    }
}
