package com.quare.blitzsplit.core.expenses.domain.model

// Why: one of the payments settling up with someone across groups, closing that group's pair.
data class SettlementLeg(
    val groupId: String,
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
)
