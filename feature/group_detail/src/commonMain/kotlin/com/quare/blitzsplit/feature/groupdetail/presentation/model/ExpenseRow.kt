package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category

// Why: "Jantar no Ostradamus · 21:30 · Bruno pagou · R$ 642,00 · −R$ 118,00".
data class ExpenseRow(
    override val id: String,
    val description: String,
    // Why: each UI writes the time of day.
    val spentAt: Long,
    /*
     * Why: "Bruno pagou", "Você e Ana pagaram" or "3 pessoas pagaram": who paid, this device's user first, then in the
     * order they were added to the group, then who left it.
     */
    val payers: List<ExpenseRowPayer>,
    val totalMinor: Long,
    // Why: what the bill does to this device's user's balance, "+R$ 600,00" or "−R$ 118,00"; null when not in it.
    val myBalanceMinor: Long?,
    // Why: the row's icon (6.1), its category as it reads now, "Outros" for none.
    val category: Category,
    // Why: "na fila" and the orange dot: saved here and not on the server yet.
    val isPending: Boolean,
) : DayEntry
