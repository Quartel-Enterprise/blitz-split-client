package com.quare.blitzsplit.core.model

// Why: "Barras ou Linha e o agrupamento ficam guardados no aparelho", for every group, never synced, like the theme.
data class StatsChartPreferences(
    val chartType: StatsChartType,
    // Why: null until picked; a grain the period can't show gives way to the one that fits it.
    val grain: StatsGrain?,
) {
    companion object {
        // Why: a fresh install, or codes from a newer version: bars, grouped by what fits the period.
        val initial: StatsChartPreferences = StatsChartPreferences(
            chartType = StatsChartType.BARS,
            grain = null,
        )
    }
}
