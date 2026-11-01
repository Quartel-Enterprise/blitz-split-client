package com.quare.blitzsplit.core.settings.domain.usecase.impl

import com.quare.blitzsplit.core.model.StatsChartPreferences
import com.quare.blitzsplit.core.settings.domain.repository.DeviceSettingsRepository
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveStatsChartPreferences
import kotlinx.coroutines.flow.Flow

class ObserveStatsChartPreferencesUseCase(
    private val repository: DeviceSettingsRepository,
) : ObserveStatsChartPreferences {
    override fun invoke(): Flow<StatsChartPreferences> = repository.observeStatsChart()
}
