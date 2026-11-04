package com.quare.blitzsplit.core.activity.domain.model

// Why: a pair of the group's Saldos as "Simplificar dívidas" found it before or left it after (13.7).
data class ActivityDebt(
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
)
