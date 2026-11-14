package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.CustomColorDao
import com.quare.blitzsplit.core.provider.room.entity.CustomColorEntity
import kotlinx.coroutines.flow.Flow

class RoomCustomColorLocalDataSource(
    private val dao: CustomColorDao,
) : CustomColorLocalDataSource {
    override fun observeAll(): Flow<List<CustomColorEntity>> = dao.observeAll()

    override fun observeHasPending(): Flow<Boolean> = dao.observeHasPending()

    override suspend fun getAll(): List<CustomColorEntity> = dao.getAll()

    override suspend fun getByColor(color: String): CustomColorEntity? = dao.getByColor(color)

    override suspend fun getPending(userId: String): List<CustomColorEntity> = dao.getPending(userId)

    override suspend fun upsert(entities: List<CustomColorEntity>) {
        dao.upsert(entities)
    }

    override suspend fun markSynced(
        color: String,
        syncedUpdatedAt: Long,
    ) {
        dao.markSynced(color, syncedUpdatedAt)
    }

    override suspend fun claimUnowned(ownerUserId: String) {
        dao.claimUnowned(ownerUserId)
    }

    override suspend fun deleteOtherUsersColors(ownerUserId: String) {
        dao.deleteOtherUsersColors(ownerUserId)
    }

    override suspend fun clear() {
        dao.deleteAll()
    }
}
