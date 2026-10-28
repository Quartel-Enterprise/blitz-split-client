package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.model.ThemePreference
import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveThemePreference
import kotlinx.coroutines.flow.Flow

class ObserveThemePreferenceUseCase(
    private val repository: DeviceSettingsRepository,
) : ObserveThemePreference {
    override fun invoke(): Flow<ThemePreference> = repository.observeTheme()
}
