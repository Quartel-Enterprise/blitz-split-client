package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateUsageDataSharing

class UpdateUsageDataSharingUseCase(
    private val repository: DeviceSettingsRepository,
) : UpdateUsageDataSharing {
    override suspend fun invoke(isOn: Boolean) {
        repository.updateUsageDataSharing(isOn)
    }
}
