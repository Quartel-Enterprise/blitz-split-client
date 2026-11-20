package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveLastSyncedAt
import kotlinx.coroutines.flow.Flow

class ObserveLastSyncedAtUseCase(
    private val repository: DeviceSettingsRepository,
) : ObserveLastSyncedAt {
    override fun invoke(): Flow<Long?> = repository.observeLastSyncedAt()
}
