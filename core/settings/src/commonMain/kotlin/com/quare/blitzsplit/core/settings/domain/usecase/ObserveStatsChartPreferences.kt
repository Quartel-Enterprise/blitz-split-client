package com.quare.blitzsplit.core.settings.domain.usecase

import com.quare.blitzsplit.core.model.StatsChartPreferences
import kotlinx.coroutines.flow.Flow

fun interface ObserveStatsChartPreferences {
    operator fun invoke(): Flow<StatsChartPreferences>
}
