package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

class DeviceSettingsLastSyncRecorder(
    private val deviceSettingsRepository: DeviceSettingsRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : LastSyncRecorder {
    override suspend fun record() {
        deviceSettingsRepository.updateLastSyncedAt(currentTimeMillis())
    }
}
