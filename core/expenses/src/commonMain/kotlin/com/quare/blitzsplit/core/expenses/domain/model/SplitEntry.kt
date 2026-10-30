package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: what each member typed in "Dividir": 1 or 0 for "Igual" (in or out), the part in the minor unit for "Valores",
 * the whole percentage for "Porcentagem" and the number of parts for "Partes". A member at 0 isn't in the split.
 */
data class SplitEntry(
    val memberId: String,
    val value: Long,
)
