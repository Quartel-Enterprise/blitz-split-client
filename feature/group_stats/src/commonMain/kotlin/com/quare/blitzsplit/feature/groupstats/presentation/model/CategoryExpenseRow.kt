package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category

// Why: a line of "Expenses by category", drawn as the group's Expenses draws a bill; a tap opens it in Bill.
data class CategoryExpenseRow(
    val id: String,
    val description: String,
    // Why: each UI writes the time of day, only while hasSpentTime.
    val spentAt: Long,
    // Why: a bill saved without a time (`SpentDay.hasTime`) shows none.
    val hasSpentTime: Boolean,
    // Why: "Bruno paid", this device's user first, then in the order they were added to the group.
    val payers: List<StatsPayer>,
    val totalMinor: Long,
    // Why: what the bill does to this device's user's balance; null when not in it.
    val myBalanceMinor: Long?,
    // Why: the row's icon, its subcategory when it has one ("Restaurant" under Food), "Other" for none.
    val category: Category,
    // Why: "queued" and the amber dot: saved here and not on the server yet.
    val isPending: Boolean,
)
