package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.BillPayingMethodDto
import com.quare.blitzsplit.core.expenses.data.dto.PayingMethodDto
import io.github.jan.supabase.postgrest.Postgrest

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

    // Why: page by page, since a person marks every bill they pay; the server's RLS answers only their own marks.
    override suspend fun fetchMarks(userId: String): List<BillPayingMethodDto> =
        postgrest.fetchAllById<BillPayingMethodDto>(
            table = MARKS,
            idOf = { it.expenseId },
            idColumn = "expense_id",
        )

    private companion object {
        const val METHODS = "payment_methods"
        const val MARKS = "bill_payment_methods"
    }
}
