package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category

// Why: "Maior despesa" of the period, which opens the bill.
data class StatsBiggestExpense(
    val expenseId: String,
    val description: String,
    val spentAt: Long,
    val payers: List<StatsPayer>,
    val amountMinor: Long,
    val isPending: Boolean,
    // Why: the badge, drawn as the group's Expenses draws the bill: its category, "Other" for none.
    val category: Category,
)
