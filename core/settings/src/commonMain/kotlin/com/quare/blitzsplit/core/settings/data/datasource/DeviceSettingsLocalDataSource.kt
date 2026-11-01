package com.quare.blitzsplit.core.settings.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.DeviceSettingsEntity
import kotlinx.coroutines.flow.Flow

interface DeviceSettingsLocalDataSource {
    fun observe(): Flow<DeviceSettingsEntity?>

    // Why: each setting changes only its own column, so picking the grid never resets the theme.
    suspend fun update(transform: (DeviceSettingsEntity) -> DeviceSettingsEntity)
}
