package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveUsageDataSharing
import kotlinx.coroutines.flow.Flow

class ObserveUsageDataSharingUseCase(
    private val repository: DeviceSettingsRepository,
) : ObserveUsageDataSharing {
    override fun invoke(): Flow<Boolean> = repository.observeUsageDataSharing()
}
