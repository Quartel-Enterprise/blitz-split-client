package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import kotlinx.coroutines.flow.Flow

fun interface ObserveExpense {
    // Why: one bill, null once it is deleted, here or on another device.
    operator fun invoke(expenseId: String): Flow<Expense?>
}
