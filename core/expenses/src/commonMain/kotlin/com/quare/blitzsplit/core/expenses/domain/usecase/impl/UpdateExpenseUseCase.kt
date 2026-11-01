package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillItems
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
        payers: List<ExpensePayer>,
        splitMode: SplitMode,
        shares: List<ExpenseShare>,
        items: BillItems?,
        spentAt: Long,
    ): Result<Unit> {
        if (!ExpenseRules.isSplitValid(amountMinor, splitMode, payers, shares, items)) {
            return Result.failure(IllegalArgumentException("The payers or the shares don't add up to the total"))
        }
        val now = currentTimeMillis()
        return repository.updateExpense(
            expense = pendingExpenseOf(
                id = expenseId,
                groupId = groupId,
                description = description,
                amountMinor = amountMinor,
                payers = payers,
                splitMode = splitMode,
                shares = shares,
                items = items,
                spentAt = spentAt,
                now = now,
            ),
            updatedAt = now,
        )
    }
}
