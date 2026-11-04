package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateLastUpdatePromptAt

class UpdateLastUpdatePromptAtUseCase(
    private val repository: DeviceSettingsRepository,
) : UpdateLastUpdatePromptAt {
    override suspend fun invoke(epochMillis: Long) {
        repository.updateLastUpdatePromptAt(epochMillis)
    }
}
