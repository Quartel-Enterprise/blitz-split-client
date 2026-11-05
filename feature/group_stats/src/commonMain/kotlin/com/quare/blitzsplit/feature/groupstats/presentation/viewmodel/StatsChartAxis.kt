package com.quare.blitzsplit.feature.groupstats.presentation.viewmodel

import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsBar

/*
 * Why: the value lines of "Over time" and its average, so the three UIs draw the same round steps ("R$ 500",
 * "R$ 1 mil", "R$ 1,5 mil") and the same dashed mean.
 */
internal object StatsChartAxis {
    // Why: at most three steps above zero, so the lines' labels fit a phone's 128 dp chart.
    private const val MAX_STEPS = 3
    private const val TEN = 10L

    // Why: past this the round steps would overflow a Long, so one line at the top stands for the scale.
    private const val MAX_STEPPED = Long.MAX_VALUE / 100

    // Why: 1, 2, 2.5 and 5 of each power of ten, as tenths, so a step reads round in any currency.
    private val niceTenths = listOf(10L, 20L, 25L, 50L, 100L)

    // Why: 0 and each step up to the first at or above the most expensive bar; empty while nothing was spent.
    fun createTicks(bars: List<StatsBar>): List<Long> {
        val largest = bars.maxOfOrNull { it.amountMinor } ?: 0L
        if (largest <= 0L) return emptyList()
        if (largest > MAX_STEPPED) return listOf(0L, largest)
        val step = findStep(largest)
        val steps = (largest + step - 1) / step
        return (0L..steps).map { it * step }
    }

    /*
     * Why: the mean of every bar, the empty ones too, rounded; only once there are two bars and it rounds above zero,
     * so the chart never says "Average per day: R$ 0".
     */
    fun createAverageOrNull(bars: List<StatsBar>): Long? {
        if (bars.size < 2) return null
        val total = bars.sumOf { it.amountMinor }
        return ((total + bars.size / 2) / bars.size).takeIf { it > 0L }
    }

    private fun findStep(largest: Long): Long {
        val rough = (largest + MAX_STEPS - 1) / MAX_STEPS
        var magnitude = 1L
        while (magnitude * TEN <= rough) magnitude *= TEN
        return niceTenths
            .filter { tenths -> (magnitude * tenths) % TEN == 0L }
            .map { tenths -> magnitude * tenths / TEN }
            .first { it >= rough }
    }
}
