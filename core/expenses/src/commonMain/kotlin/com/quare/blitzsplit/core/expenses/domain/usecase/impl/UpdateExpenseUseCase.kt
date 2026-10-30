package com.quare.blitzsplit.core.expenses.domain.usecase.impl

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
            expense = pendingExpenseOf(
                id = expenseId,
                groupId = groupId,
                description = description,
                amountMinor = amountMinor,
                payerId = payerId,
                splitMode = splitMode,
                shares = shares,
                spentAt = spentAt,
                now = now,
            ),
            updatedAt = now,
        )
    }
}
