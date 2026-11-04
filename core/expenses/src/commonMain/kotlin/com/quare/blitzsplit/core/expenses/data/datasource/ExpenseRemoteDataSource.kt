package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.ExpenseDto
import com.quare.blitzsplit.core.expenses.data.dto.ServerBillDto

interface ExpenseRemoteDataSource {
    suspend fun upsertExpenses(expenses: List<ExpenseDto>)

    // Why: every bill of every group the user may see, which the server's row level security decides.
    suspend fun fetchExpenses(): List<ExpenseDto>

    // Why: one bill's group and tombstone, null when row level security hides it.
    suspend fun fetchServerBill(id: String): ServerBillDto?

    // Why: every bill of one group, deleted ones included, for a first download that brings each group whole.
    suspend fun fetchExpenses(groupId: String): List<ExpenseDto>
}
