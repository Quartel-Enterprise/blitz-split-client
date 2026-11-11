package com.quare.blitzsplit.feature.groupstats.presentation.model

// Why: "Maior despesa" of the period, which opens the bill.
data class StatsBiggestExpense(
    val expenseId: String,
    val description: String,
    val spentAt: Long,
    // Why: a bill saved without a time (`SpentDay.hasTime`) shows its date alone.
    val hasSpentTime: Boolean,
    val payers: List<StatsPayer>,
    val amountMinor: Long,
    val isPending: Boolean,
)
