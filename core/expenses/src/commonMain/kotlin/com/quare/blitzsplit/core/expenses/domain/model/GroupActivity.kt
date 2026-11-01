package com.quare.blitzsplit.core.expenses.domain.model

// Why: when a group was last used, which the home sorts and ages its rows by and Rachar conta lists the groups by.
object GroupActivity {
    // Why: when each one was saved, not its "Quando": a payment recorded today and dated last week is today's.
    fun latestAtOf(
        groupUpdatedAt: Long,
        expenses: List<Expense>,
        settlements: List<Settlement>,
    ): Long = maxOf(
        groupUpdatedAt,
        expenses.maxOfOrNull { it.updatedAt } ?: Long.MIN_VALUE,
        settlements.maxOfOrNull { it.updatedAt } ?: Long.MIN_VALUE,
    )
}
