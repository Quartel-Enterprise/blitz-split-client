package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.CreateExpense
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlin.uuid.Uuid

// Why: the bill exists on the device right away, offline included, and counts in the balances from then on.
class CreateExpenseUseCase(
    private val repository: ExpenseRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : CreateExpense {
    override suspend fun invoke(
        groupId: String,
        description: String,
        amountMinor: Long,
        payers: List<ExpensePayer>,
        splitMode: SplitMode,
        shares: List<ExpenseShare>,
        spentAt: Long,
    ): Result<String> {
        val id = Uuid.random().toString()
        val now = currentTimeMillis()
        return repository
            .createExpense(
                expense = pendingExpenseOf(
                    id = id,
                    groupId = groupId,
                    description = description,
                    amountMinor = amountMinor,
                    payers = payers,
                    splitMode = splitMode,
                    shares = shares,
                    spentAt = spentAt,
                    now = now,
                ),
                createdAt = now,
            ).map { id }
    }
}
