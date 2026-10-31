package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.SettlementDto

interface SettlementRemoteDataSource {
    suspend fun upsertSettlements(settlements: List<SettlementDto>)

    // Why: every payment of every group the user may see, which the server's row level security decides.
    suspend fun fetchSettlements(): List<SettlementDto>
}
