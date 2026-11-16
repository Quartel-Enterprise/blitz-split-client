package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.model.TimeFormatPreference
import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateTimeFormatPreference

class UpdateTimeFormatPreferenceUseCase(
    private val repository: DeviceSettingsRepository,
) : UpdateTimeFormatPreference {
    override suspend fun invoke(timeFormat: TimeFormatPreference) {
        repository.updateTimeFormat(timeFormat)
    }
}
