package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.expenses.data.dto.CustomColorDto

interface CustomColorRemoteDataSource {
    suspend fun upsert(colors: List<CustomColorDto>)

    // Why: only this user's palette, which the server's row level security decides too.
    suspend fun fetch(userId: String): List<CustomColorDto>
}
