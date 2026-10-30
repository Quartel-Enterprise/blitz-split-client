package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
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
        payerId: String,
        splitMode: SplitMode,
        shares: List<ExpenseShare>,
        spentAt: Long,
    ): Result<String> {
        val id = Uuid.random().toString()
        val now = currentTimeMillis()
        return repository
            .createExpense(
                expense = Expense(
                    id = id,
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
                createdAt = now,
            ).map { id }
    }
}
