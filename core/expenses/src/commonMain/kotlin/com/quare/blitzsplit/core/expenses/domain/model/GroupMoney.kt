package com.quare.blitzsplit.core.expenses.domain.model

// Why: what moves a group's balance, as the device has it now: its bills and payments, each in its list's order.
data class GroupMoney(
    val expenses: List<Expense>,
    val settlements: List<Settlement>,
) {
    companion object {
        // Why: a group with no bill nor payment on the device yet.
        val NONE = GroupMoney(
            expenses = emptyList(),
            settlements = emptyList(),
        )
    }
}
