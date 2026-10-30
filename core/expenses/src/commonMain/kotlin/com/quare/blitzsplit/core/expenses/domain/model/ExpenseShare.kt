package com.quare.blitzsplit.core.expenses.domain.model

data class ExpenseShare(
    val memberId: String,
    val amountMinor: Long,
    /*
     * Why: what was typed, so an edit reopens the split as it was: 1 for "Igual", the percentage for "Porcentagem",
     * the number of parts for "Partes", and the part itself for "Valores".
     */
    val weight: Long,
)
