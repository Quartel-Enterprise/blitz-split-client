package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.BillPayingMethodDto
import com.quare.blitzsplit.core.expenses.data.dto.PayingMethodDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order

// Why: `payment_methods` and `bill_payment_methods`, which the server only lets their owner read and write.
class SupabasePayingMethodRemoteDataSource(
    private val postgrest: Postgrest,
) : PayingMethodRemoteDataSource {
    override suspend fun upsertMethods(methods: List<PayingMethodDto>) {
        postgrest.from(METHODS).upsert(methods)
    }

    override suspend fun fetchMethods(userId: String): List<PayingMethodDto> = postgrest
        .from(METHODS)
        .select {
            filter { eq("user_id", userId) }
        }.decodeList<PayingMethodDto>()

    override suspend fun upsertMarks(marks: List<BillPayingMethodDto>) {
        postgrest.from(MARKS).upsert(marks)
    }

    // Why: page by page, since a person marks every bill they pay and the server answers at most max_rows at once.
    override suspend fun fetchMarks(userId: String): List<BillPayingMethodDto> {
        val marks = mutableListOf<BillPayingMethodDto>()
        do {
            val from = marks.size.toLong()
            val page = postgrest
                .from(MARKS)
                .select {
                    filter { eq("user_id", userId) }
                    order(
                        column = "expense_id",
                        order = Order.ASCENDING,
                    )
                    range(
                        from = from,
                        to = from + PAGE_SIZE - 1,
                    )
                }.decodeList<BillPayingMethodDto>()
            marks += page
        } while (page.isNotEmpty())
        return marks
    }

    private companion object {
        const val METHODS = "payment_methods"
        const val MARKS = "bill_payment_methods"

        // Why: the API's max_rows, Supabase's default.
        const val PAGE_SIZE = 1_000L
    }
}
