package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveLastUpdatePromptAt
import kotlinx.coroutines.flow.Flow

class ObserveLastUpdatePromptAtUseCase(
    private val repository: DeviceSettingsRepository,
) : ObserveLastUpdatePromptAt {
    override fun invoke(): Flow<Long?> = repository.observeLastUpdatePromptAt()
}
