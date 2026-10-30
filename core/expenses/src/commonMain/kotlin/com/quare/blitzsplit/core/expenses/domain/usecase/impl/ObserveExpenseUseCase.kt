package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpense
import kotlinx.coroutines.flow.Flow

class ObserveExpenseUseCase(
    private val repository: ExpenseRepository,
) : ObserveExpense {
    override fun invoke(expenseId: String): Flow<Expense?> = repository.observeExpense(expenseId)
}
