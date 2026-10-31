package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.SettlementDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order

class SupabaseSettlementRemoteDataSource(
    private val postgrest: Postgrest,
) : SettlementRemoteDataSource {
    override suspend fun upsertSettlements(settlements: List<SettlementDto>) {
        postgrest.from(SETTLEMENTS).upsert(settlements)
    }

    // Why: page by page after the last id, like the bills, so a payment added meanwhile never shifts a page.
    override suspend fun fetchSettlements(): List<SettlementDto> {
        val rows = mutableListOf<SettlementDto>()
        do {
            val lastId = rows.lastOrNull()?.id
            val page = postgrest
                .from(SETTLEMENTS)
                .select {
                    if (lastId != null) filter { gt("id", lastId) }
                    order(
                        column = "id",
                        order = Order.ASCENDING,
                    )
                    limit(PAGE_SIZE)
                }.decodeList<SettlementDto>()
            rows += page
        } while (page.isNotEmpty())
        return rows
    }

    private companion object {
        const val SETTLEMENTS = "settlements"

        // Why: the API's max_rows, Supabase's default.
        const val PAGE_SIZE = 1_000L
    }
}
