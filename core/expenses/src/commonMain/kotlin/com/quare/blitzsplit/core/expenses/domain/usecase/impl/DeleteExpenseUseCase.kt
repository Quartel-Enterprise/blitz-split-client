package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteExpense
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

// Why: the bill leaves the group's list and its balances at once, offline included, and every device once synced.
class DeleteExpenseUseCase(
    private val repository: ExpenseRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : DeleteExpense {
    override suspend fun invoke(expenseId: String): Result<Unit> = repository.deleteExpense(
        id = expenseId,
        deletedAt = currentTimeMillis(),
    )
}
