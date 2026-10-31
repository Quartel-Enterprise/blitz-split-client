package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.SettlementDto
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseSettlementRemoteDataSource(
    private val postgrest: Postgrest,
) : SettlementRemoteDataSource {
    override suspend fun upsertSettlements(settlements: List<SettlementDto>) {
        postgrest.from(SETTLEMENTS).upsert(settlements)
    }

    override suspend fun fetchSettlements(): List<SettlementDto> = postgrest.fetchAllById<SettlementDto>(
        table = SETTLEMENTS,
        idOf = { it.id },
    )

    private companion object {
        const val SETTLEMENTS = "settlements"
    }
}
