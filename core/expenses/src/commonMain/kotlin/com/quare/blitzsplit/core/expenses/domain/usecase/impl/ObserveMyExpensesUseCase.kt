package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMyExpenses
import kotlinx.coroutines.flow.Flow

class ObserveMyExpensesUseCase(
    private val repository: ExpenseRepository,
) : ObserveMyExpenses {
    override fun invoke(limit: Int): Flow<List<Expense>> = repository.observeMyExpenses(limit)
}
