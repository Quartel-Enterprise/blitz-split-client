package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.CustomColorDto
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseCustomColorRemoteDataSource(
    private val postgrest: Postgrest,
) : CustomColorRemoteDataSource {
    override suspend fun upsert(colors: List<CustomColorDto>) {
        postgrest.from(TABLE).upsert(colors) {
            onConflict = "user_id,color"
        }
    }

    // Why: a few per user (the palette holds 12, plus the deleted ones), far below one page of the server's max_rows.
    override suspend fun fetch(userId: String): List<CustomColorDto> = postgrest
        .from(TABLE)
        .select {
            filter { eq("user_id", userId) }
        }.decodeList<CustomColorDto>()

    private companion object {
        const val TABLE = "custom_colors"
    }
}
