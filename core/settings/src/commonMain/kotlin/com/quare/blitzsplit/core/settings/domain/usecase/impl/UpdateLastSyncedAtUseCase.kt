package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateLastSyncedAt

class UpdateLastSyncedAtUseCase(
    private val repository: DeviceSettingsRepository,
) : UpdateLastSyncedAt {
    override suspend fun invoke(epochMillis: Long) {
        repository.updateLastSyncedAt(epochMillis)
    }
}
