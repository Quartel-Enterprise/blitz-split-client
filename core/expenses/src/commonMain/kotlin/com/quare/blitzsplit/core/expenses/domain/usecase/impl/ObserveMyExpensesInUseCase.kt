package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMyExpensesIn
import kotlinx.coroutines.flow.Flow

class ObserveMyExpensesInUseCase(
    private val repository: ExpenseRepository,
) : ObserveMyExpensesIn {
    override fun invoke(
        groupIds: Set<String>,
        limit: Int,
    ): Flow<List<Expense>> = repository.observeMyExpensesIn(groupIds, limit)
}
