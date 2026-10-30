package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.ExpenseDto

interface ExpenseRemoteDataSource {
    suspend fun upsertExpenses(expenses: List<ExpenseDto>)

    // Why: every bill of every group the user may see, which the server's row level security decides.
    suspend fun fetchExpenses(): List<ExpenseDto>
}
