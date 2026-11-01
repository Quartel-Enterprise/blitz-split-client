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

    /*
     * Why: the one order of the home's rows and of "Rachar em qual grupo?", so the group the home lists first is the
     * one Rachar conta marks. The most recent activity first; in a stable sort (sortedWith), the list's own order
     * (the groups' latest change first) breaks a tie.
     */
    fun <T> compareMostRecentFirst(activityAtOf: (T) -> Long): Comparator<T> = compareByDescending(activityAtOf)
}
