package com.quare.blitzsplit.core.settings.data.datasource

import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.provider.room.dao.DeviceSettingsDao
import com.quare.blitzsplit.core.provider.room.entity.DeviceSettingsEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomDeviceSettingsLocalDataSource(
    private val dao: DeviceSettingsDao,
) : DeviceSettingsLocalDataSource {
    private val writeLock = Mutex()

    private val emptySettings = DeviceSettingsEntity(
        id = SINGLE_ROW_ID,
        theme = ThemePreference.SYSTEM.code,
        groupsDisplay = null,
        groupGridSize = null,
    )

    override fun observe(): Flow<DeviceSettingsEntity?> = dao.observe()

    override suspend fun update(transform: (DeviceSettingsEntity) -> DeviceSettingsEntity) {
        writeLock.withLock {
            dao.upsert(transform(dao.get() ?: emptySettings))
        }
    }
}
