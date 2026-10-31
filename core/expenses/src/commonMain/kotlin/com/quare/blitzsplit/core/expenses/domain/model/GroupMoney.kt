package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: what moves a group's balance and what of it waits to go up, as the device has it now: its bills and payments,
 * each in its list's order.
 */
data class GroupMoney(
    val expenses: List<Expense>,
    val settlements: List<Settlement>,
    // Why: its bills and payments deleted here whose tombstones wait to go up: in no list, still "na fila".
    val pendingDeletionCount: Int,
) {
    companion object {
        // Why: a group with no bill nor payment on the device yet.
        val NONE = GroupMoney(
            expenses = emptyList(),
            settlements = emptyList(),
            pendingDeletionCount = 0,
        )
    }
}
