package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.CategoryTermDto

interface CategoryTermRemoteDataSource {
    suspend fun upsert(terms: List<CategoryTermDto>)

    suspend fun fetch(userId: String): List<CategoryTermDto>
}
