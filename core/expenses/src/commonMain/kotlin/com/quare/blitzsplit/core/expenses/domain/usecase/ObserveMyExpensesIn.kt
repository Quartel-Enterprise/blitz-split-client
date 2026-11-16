package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import kotlinx.coroutines.flow.Flow

fun interface ObserveMyExpensesIn {
    // Why: the first `limit` bills this device's user paid or takes part in, of the groups picked, the most recent first.
    operator fun invoke(
        groupIds: Set<String>,
        limit: Int,
    ): Flow<List<Expense>>
}
