package com.quare.blitzsplit.core.settings.domain.usecase

import com.quare.blitzsplit.core.model.StatsChartPreferences

fun interface UpdateStatsChartPreferences {
    suspend operator fun invoke(preferences: StatsChartPreferences)
}
