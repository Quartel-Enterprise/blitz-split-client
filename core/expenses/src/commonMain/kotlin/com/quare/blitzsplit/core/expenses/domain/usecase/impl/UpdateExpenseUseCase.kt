package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.UpdateExpense
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

/*
 * Why: on the device right away, offline included, and the balances count the new version from then on. Who created
 * the bill and when stay as they were: the old app lost both on every edit.
 */
class UpdateExpenseUseCase(
    private val repository: ExpenseRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : UpdateExpense {
    override suspend fun invoke(
        expenseId: String,
        groupId: String,
        description: String,
        amountMinor: Long,
        payerId: String,
        splitMode: SplitMode,
        shares: List<ExpenseShare>,
        spentAt: Long,
    ): Result<Unit> {
        val now = currentTimeMillis()
        return repository.updateExpense(
            expense = Expense(
                id = expenseId,
                groupId = groupId,
                // Why: trimmed again after the cap, which may end on a space.
                description = ExpenseRules.toCapped(description.trim()).trimEnd(),
                amountMinor = amountMinor,
                splitMode = splitMode,
                payers = listOf(
                    ExpensePayer(
                        memberId = payerId,
                        amountMinor = amountMinor,
                    ),
                ),
                shares = shares,
                spentAt = spentAt,
                isPending = true,
                updatedAt = now,
                updatedByMemberId = null,
            ),
            updatedAt = now,
        )
    }
}
