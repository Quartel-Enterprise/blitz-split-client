package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.ExpenseDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order

class SupabaseExpenseRemoteDataSource(
    private val postgrest: Postgrest,
) : ExpenseRemoteDataSource {
    override suspend fun upsertExpenses(expenses: List<ExpenseDto>) {
        postgrest.from(EXPENSES).upsert(expenses)
    }

    /*
     * Why: page by page until one comes back empty: a bill missing from the answer would read as removed on the server
     * and leave the device. Each page starts after the last id of the one before, not at an offset, since other
     * members keep adding bills meanwhile, which would shift an offset and skip a row.
     */
    override suspend fun fetchExpenses(): List<ExpenseDto> {
        val rows = mutableListOf<ExpenseDto>()
        do {
            val lastId = rows.lastOrNull()?.id
            val page = postgrest
                .from(EXPENSES)
                .select {
                    if (lastId != null) filter { gt("id", lastId) }
                    order(
                        column = "id",
                        order = Order.ASCENDING,
                    )
                    limit(PAGE_SIZE)
                }.decodeList<ExpenseDto>()
            rows += page
        } while (page.isNotEmpty())
        return rows
    }

    private companion object {
        const val EXPENSES = "expenses"

        // Why: the API's max_rows, Supabase's default.
        const val PAGE_SIZE = 1_000L
    }
}
