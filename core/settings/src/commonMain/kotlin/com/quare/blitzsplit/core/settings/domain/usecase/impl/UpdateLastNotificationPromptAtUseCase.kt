package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateLastNotificationPromptAt

class UpdateLastNotificationPromptAtUseCase(
    private val repository: DeviceSettingsRepository,
) : UpdateLastNotificationPromptAt {
    override suspend fun invoke(epochMillis: Long) {
        repository.updateLastNotificationPromptAt(epochMillis)
    }
}
