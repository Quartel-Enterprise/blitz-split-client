package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.GroupCategoryDto
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseGroupCategoryRemoteDataSource(
    private val postgrest: Postgrest,
) : GroupCategoryRemoteDataSource {
    override suspend fun upsertCategories(categories: List<GroupCategoryDto>) {
        postgrest.from(GROUP_CATEGORIES).upsert(categories)
    }

    override suspend fun fetchCategories(): List<GroupCategoryDto> = postgrest.fetchAllById<GroupCategoryDto>(
        table = GROUP_CATEGORIES,
        idOf = { it.id },
    )

    private companion object {
        const val GROUP_CATEGORIES = "group_categories"
    }
}
