package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import kotlinx.coroutines.flow.Flow

fun interface ObserveExpenses {
    // Why: the group's bills, the most recent first.
    operator fun invoke(groupId: String): Flow<List<Expense>>
}
