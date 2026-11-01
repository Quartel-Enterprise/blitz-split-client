package com.quare.blitzsplit.core.contacts.data.datasource

import com.quare.blitzsplit.core.contacts.data.dto.HiddenContactDto
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseHiddenContactRemoteDataSource(
    private val postgrest: Postgrest,
) : HiddenContactRemoteDataSource {
    override suspend fun upsert(rows: List<HiddenContactDto>) {
        postgrest.from(TABLE).upsert(rows) {
            onConflict = "user_id,person_key"
        }
    }

    override suspend fun fetch(userId: String): List<HiddenContactDto> = postgrest
        .from(TABLE)
        .select {
            filter { eq("user_id", userId) }
        }.decodeList<HiddenContactDto>()

    private companion object {
        const val TABLE = "hidden_contacts"
    }
}
