package com.quare.blitzsplit.core.expenses.domain.model

// Why: amounts are whole numbers of the minor unit of the group's currency (centavos for BRL).
data class Expense(
    val id: String,
    val groupId: String,
    val description: String,
    val amountMinor: Long,
    val splitMode: SplitMode,
    // Why: who paid and how much, adding up to the total.
    val payers: List<ExpensePayer>,
    // Why: each member in the split with the part the split gave them, adding up to the total.
    val shares: List<ExpenseShare>,
    // Why: "Quando", in epoch millis.
    val spentAt: Long,
    // Why: created or changed on this device and not up on the server yet ("na fila").
    val isPending: Boolean,
)
