package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode

fun interface UpdateExpense {
    /*
     * Why: "Editar conta" saves the bill as filled in, the same fields as a new one. Payers and shares each add up to
     * the total, the shares coming from BillSplit.
     */
    suspend operator fun invoke(
        expenseId: String,
        groupId: String,
        description: String,
        amountMinor: Long,
        payers: List<ExpensePayer>,
        splitMode: SplitMode,
        shares: List<ExpenseShare>,
        spentAt: Long,
    ): Result<Unit>
}
