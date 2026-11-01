package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.model.StatsChartPreferences
import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateStatsChartPreferences

class UpdateStatsChartPreferencesUseCase(
    private val repository: DeviceSettingsRepository,
) : UpdateStatsChartPreferences {
    override suspend fun invoke(preferences: StatsChartPreferences) {
        repository.updateStatsChart(preferences)
    }
}
