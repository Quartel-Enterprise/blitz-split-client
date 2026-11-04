package com.quare.blitzsplit.core.groups.domain.model

// Why: one payment of the group's Saldos, "Diego te deve R$ 64,00", as "Simplificar dívidas" sends it to the server.
data class SimplifyDebtsPair(
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
)
