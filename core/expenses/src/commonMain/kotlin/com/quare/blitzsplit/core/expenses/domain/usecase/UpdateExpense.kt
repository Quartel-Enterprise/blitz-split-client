package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode

fun interface UpdateExpense {
    /*
     * Why: "Editar conta" saves the bill as filled in, the same fields as a new one. One person paid the whole total;
     * shares come from BillSplit and add up to it.
     */
    suspend operator fun invoke(
        expenseId: String,
        groupId: String,
        description: String,
        amountMinor: Long,
        payerId: String,
        splitMode: SplitMode,
        shares: List<ExpenseShare>,
        spentAt: Long,
    ): Result<Unit>
}
