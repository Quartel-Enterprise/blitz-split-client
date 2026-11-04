package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateDynamicColors

class UpdateDynamicColorsUseCase(
    private val repository: DeviceSettingsRepository,
) : UpdateDynamicColors {
    override suspend fun invoke(isOn: Boolean) {
        repository.updateDynamicColors(isOn)
    }
}
