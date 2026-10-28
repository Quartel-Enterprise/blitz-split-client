package com.quare.blitzsplit.core.settings.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.DeviceSettingsEntity
import kotlinx.coroutines.flow.Flow

interface DeviceSettingsLocalDataSource {
    fun observe(): Flow<DeviceSettingsEntity?>

    suspend fun upsert(entity: DeviceSettingsEntity)
}
