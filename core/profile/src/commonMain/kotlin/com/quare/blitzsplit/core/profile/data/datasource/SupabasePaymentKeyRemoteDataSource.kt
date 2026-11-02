package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.profile.data.dto.PaymentKeyDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order

class SupabasePaymentKeyRemoteDataSource(
    private val postgrest: Postgrest,
) : PaymentKeyRemoteDataSource {
    override suspend fun upsert(keys: List<PaymentKeyDto>) {
        postgrest.from(TABLE).upsert(keys)
    }

    override suspend fun fetch(userId: String): List<PaymentKeyDto> = postgrest
        .from(TABLE)
        .select {
            filter { eq("user_id", userId) }
        }.decodeList<PaymentKeyDto>()

    /*
     * Why: page by page, since the server answers at most max_rows per request, until a page comes back empty: a
     * short page proves nothing if max_rows is below the page asked for. A key missing from the answer would leave
     * the device, since the answer replaces them all.
     */
    override suspend fun fetchOthers(userId: String): List<PaymentKeyDto> {
        val keys = mutableListOf<PaymentKeyDto>()
        do {
            val from = keys.size.toLong()
            val page = postgrest
                .from(TABLE)
                .select {
                    filter {
                        neq("user_id", userId)
                        eq("is_deleted", false)
                    }
                    order(
                        column = "id",
                        order = Order.ASCENDING,
                    )
                    range(
                        from = from,
                        to = from + PAGE_SIZE - 1,
                    )
                }.decodeList<PaymentKeyDto>()
            keys += page
        } while (page.isNotEmpty())
        return keys
    }

    private companion object {
        const val TABLE = "payment_keys"

        // Why: the API's max_rows, Supabase's default.
        const val PAGE_SIZE = 1_000L
    }
}
