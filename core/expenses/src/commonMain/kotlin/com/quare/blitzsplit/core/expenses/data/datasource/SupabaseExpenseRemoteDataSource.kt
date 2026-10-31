package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.ExpenseDto
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseExpenseRemoteDataSource(
    private val postgrest: Postgrest,
) : ExpenseRemoteDataSource {
    override suspend fun upsertExpenses(expenses: List<ExpenseDto>) {
        postgrest.from(EXPENSES).upsert(expenses)
    }

    override suspend fun fetchExpenses(): List<ExpenseDto> = postgrest.fetchAllById<ExpenseDto>(
        table = EXPENSES,
        idOf = { it.id },
    )

    private companion object {
        const val EXPENSES = "expenses"
    }
}
