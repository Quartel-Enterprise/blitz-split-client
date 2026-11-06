package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.PushSettingsDao
import com.quare.blitzsplit.core.provider.room.entity.PushSettingsEntity
import kotlinx.coroutines.flow.Flow

class RoomPushSettingsLocalDataSource(
    private val dao: PushSettingsDao,
) : PushSettingsLocalDataSource {
    override fun observe(): Flow<PushSettingsEntity?> = dao.observe()

    override suspend fun upsert(entity: PushSettingsEntity) {
        dao.upsert(entity)
    }
}
