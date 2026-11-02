package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.CategoryTermDto
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseCategoryTermRemoteDataSource(
    private val postgrest: Postgrest,
) : CategoryTermRemoteDataSource {
    override suspend fun upsert(terms: List<CategoryTermDto>) {
        postgrest.from(TABLE).upsert(terms)
    }

    override suspend fun fetch(userId: String): List<CategoryTermDto> = postgrest
        .from(TABLE)
        .select {
            filter { eq("user_id", userId) }
        }.decodeList<CategoryTermDto>()

    private companion object {
        const val TABLE = "category_terms"
    }
}
