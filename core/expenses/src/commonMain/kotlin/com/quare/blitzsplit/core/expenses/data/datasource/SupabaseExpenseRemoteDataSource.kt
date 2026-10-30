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
     * Why: page by page, as the groups, until a page comes back empty: a bill missing from the answer would read as
     * removed on the server and leave the device.
     */
    override suspend fun fetchExpenses(): List<ExpenseDto> {
        val rows = mutableListOf<ExpenseDto>()
        do {
            val from = rows.size.toLong()
            val page = postgrest
                .from(EXPENSES)
                .select {
                    order(
                        column = "id",
                        order = Order.ASCENDING,
                    )
                    range(
                        from = from,
                        to = from + PAGE_SIZE - 1,
                    )
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
