package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import kotlinx.coroutines.flow.Flow

fun interface ObserveMyExpenses {
    // Why: the first `limit` bills this device's user paid or takes part in, across groups, the most recent first.
    operator fun invoke(limit: Int): Flow<List<Expense>>
}
