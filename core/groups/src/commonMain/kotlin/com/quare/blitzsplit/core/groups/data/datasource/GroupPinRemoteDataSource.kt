package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.groups.data.dto.GroupPinDto

interface GroupPinRemoteDataSource {
    // Why: only this user's pins, which the server's row level security decides.
    suspend fun fetchPins(): List<GroupPinDto>

    suspend fun upsertPins(pins: List<GroupPinDto>)
}
