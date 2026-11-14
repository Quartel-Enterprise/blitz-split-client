package com.quare.blitzsplit.core.expenses.export

// Why: a member's amount in minor units: what they paid or their share of a bill, or their balance in a currency.
data class ExportAmount(
    val memberId: String,
    val amountMinor: Long,
)
