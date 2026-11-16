package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.model.TimeFormatPreference
import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveTimeFormatPreference
import kotlinx.coroutines.flow.Flow

class ObserveTimeFormatPreferenceUseCase(
    private val repository: DeviceSettingsRepository,
) : ObserveTimeFormatPreference {
    override fun invoke(): Flow<TimeFormatPreference> = repository.observeTimeFormat()
}
