package com.quare.blitzsplit.feature.groupstats.presentation.model

/*
 * Why: the shares of a ring (by category, by person) as whole percentages that add up to 100, rounded by the largest
 * remainders, so the ring always closes and no share reads further from its amount than it must; all 0 while the
 * total is 0. The previews and the store screenshots round the same way as the screen.
 */
object RoundedPercents {
    private const val PERCENT_UNITS = 100L

    fun of(
        amounts: List<Long>,
        total: Long = amounts.sum(),
    ): List<Int> {
        if (total <= 0) return amounts.map { 0 }
        val exact = amounts.map { it * PERCENT_UNITS }
        val floors = exact.map { (it / total).toInt() }.toMutableList()
        val missing = PERCENT_UNITS.toInt() - floors.sum()
        exact.indices
            .sortedByDescending { exact[it] % total }
            .take(missing.coerceAtLeast(0))
            .forEach { floors[it] += 1 }
        return floors
    }
}
