package com.quare.blitzsplit.core.expenses.domain.model

// Why: "Ana te deve R$ 162,30", one pair of members already netted.
data class Debt(
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
)
