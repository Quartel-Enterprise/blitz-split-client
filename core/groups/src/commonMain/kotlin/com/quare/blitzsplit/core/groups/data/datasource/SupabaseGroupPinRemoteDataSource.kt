package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.groups.data.dto.GroupPinDto
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseGroupPinRemoteDataSource(
    private val postgrest: Postgrest,
) : GroupPinRemoteDataSource {
    // Why: a few per user, far below one page of the server's max_rows.
    override suspend fun fetchPins(): List<GroupPinDto> = postgrest
        .from(GROUP_PINS)
        .select()
        .decodeList<GroupPinDto>()

    override suspend fun upsertPins(pins: List<GroupPinDto>) {
        postgrest.from(GROUP_PINS).upsert(pins) {
            onConflict = "user_id,group_id"
        }
    }

    private companion object {
        const val GROUP_PINS = "group_pins"
    }
}
