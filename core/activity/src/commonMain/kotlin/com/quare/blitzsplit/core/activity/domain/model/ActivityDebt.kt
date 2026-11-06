package com.quare.blitzsplit.core.activity.domain.model

// Why: a pair of the group's Saldos as "Simplificar dívidas" found it before or left it after (13.7).
data class ActivityDebt(
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
    // Why: the currency the pair is in; null for an event written before balances were per currency (the group's).
    val currency: String? = null,
)
