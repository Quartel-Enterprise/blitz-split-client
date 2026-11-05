package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveLastNotificationPromptAt
import kotlinx.coroutines.flow.Flow

class ObserveLastNotificationPromptAtUseCase(
    private val repository: DeviceSettingsRepository,
) : ObserveLastNotificationPromptAt {
    override fun invoke(): Flow<Long?> = repository.observeLastNotificationPromptAt()
}
