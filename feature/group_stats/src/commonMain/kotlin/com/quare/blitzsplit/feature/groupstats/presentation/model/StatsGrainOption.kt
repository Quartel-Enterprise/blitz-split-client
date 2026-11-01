package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.model.StatsGrain

// Why: "Dia", "Semana", "Mês" or "Ano"; one the period can't show is disabled, with the reason under the chart.
data class StatsGrainOption(
    val grain: StatsGrain,
    val isEnabled: Boolean,
)
