package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.ExpenseDto
import com.quare.blitzsplit.core.expenses.data.dto.ServerBillDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns

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

    override suspend fun fetchExpenses(groupId: String): List<ExpenseDto> = postgrest.fetchAllById<ExpenseDto>(
        table = EXPENSES,
        idOf = { it.id },
        groupId = groupId,
    )

    override suspend fun fetchServerBill(id: String): ServerBillDto? = postgrest
        .from(EXPENSES)
        .select(Columns.list("group_id", "is_deleted")) {
            filter { eq("id", id) }
        }.decodeSingleOrNull<ServerBillDto>()

    private companion object {
        const val EXPENSES = "expenses"
    }
}
