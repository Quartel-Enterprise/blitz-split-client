package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode

fun interface CreateExpense {
    // Why: the new bill's id. Payers and shares each add up to the total, the shares coming from BillSplit.
    suspend operator fun invoke(
        groupId: String,
        description: String,
        amountMinor: Long,
        payers: List<ExpensePayer>,
        splitMode: SplitMode,
        shares: List<ExpenseShare>,
        items: BillItems?,
        spentAt: Long,
    ): Result<String>
}
