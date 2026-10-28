package com.quare.blitzsplit.core.settings.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.DeviceSettingsDao
import com.quare.blitzsplit.core.provider.room.entity.DeviceSettingsEntity
import kotlinx.coroutines.flow.Flow

class RoomDeviceSettingsLocalDataSource(
    private val dao: DeviceSettingsDao,
) : DeviceSettingsLocalDataSource {
    override fun observe(): Flow<DeviceSettingsEntity?> = dao.observe()

    override suspend fun upsert(entity: DeviceSettingsEntity) {
        dao.upsert(entity)
    }
}
