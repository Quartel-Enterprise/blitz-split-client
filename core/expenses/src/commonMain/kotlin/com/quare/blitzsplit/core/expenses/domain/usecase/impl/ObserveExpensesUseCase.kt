package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import kotlinx.coroutines.flow.Flow

class ObserveExpensesUseCase(
    private val repository: ExpenseRepository,
) : ObserveExpenses {
    override fun invoke(groupId: String): Flow<List<Expense>> = repository.observeExpenses(groupId)
}
