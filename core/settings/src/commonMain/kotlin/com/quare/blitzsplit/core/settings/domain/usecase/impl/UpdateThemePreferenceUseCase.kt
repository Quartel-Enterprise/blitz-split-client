package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateThemePreference

class UpdateThemePreferenceUseCase(
    private val repository: DeviceSettingsRepository,
) : UpdateThemePreference {
    override suspend fun invoke(theme: ThemePreference) {
        repository.updateTheme(theme)
    }
}
