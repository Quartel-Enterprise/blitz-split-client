package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.AppCategoryColorDao
import com.quare.blitzsplit.core.provider.room.entity.AppCategoryColorEntity
import kotlinx.coroutines.flow.Flow

class RoomAppCategoryColorLocalDataSource(
    private val dao: AppCategoryColorDao,
) : AppCategoryColorLocalDataSource {
    override fun observeColors(): Flow<List<AppCategoryColorEntity>> = dao.observeColors()

    override suspend fun setColor(entity: AppCategoryColorEntity) {
        dao.upsert(entity)
    }

    override suspend fun clearColor(categoryKey: String) {
        dao.delete(categoryKey)
    }
}
