package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.GroupCategoryDto

interface GroupCategoryRemoteDataSource {
    suspend fun upsertCategories(categories: List<GroupCategoryDto>)

    // Why: every category of the user's groups, deleted ones included, page by page.
    suspend fun fetchCategories(): List<GroupCategoryDto>
}
