package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.AppCategoryColorEntity
import kotlinx.coroutines.flow.Flow

interface AppCategoryColorLocalDataSource {
    fun observeColors(): Flow<List<AppCategoryColorEntity>>

    suspend fun setColor(entity: AppCategoryColorEntity)

    suspend fun clearColor(categoryKey: String)
}
