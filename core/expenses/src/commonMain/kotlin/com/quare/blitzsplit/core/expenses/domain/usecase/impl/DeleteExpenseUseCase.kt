package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.DeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteExpense
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

/*
 * Why: the bill leaves the group's list and its balances at once, offline included, and every device once synced.
 * Marked as deleted here before Room tells the screens it went, so none of them takes it for someone else's delete.
 */
class DeleteExpenseUseCase(
    private val repository: ExpenseRepository,
    private val deletedBills: DeletedBillRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : DeleteExpense {
    override suspend fun invoke(expenseId: String): Result<Unit> {
        deletedBills.markDeletedHere(expenseId)
        return repository
            .deleteExpense(
                id = expenseId,
                deletedAt = currentTimeMillis(),
            ).onFailure { deletedBills.unmarkDeletedHere(expenseId) }
    }
}
