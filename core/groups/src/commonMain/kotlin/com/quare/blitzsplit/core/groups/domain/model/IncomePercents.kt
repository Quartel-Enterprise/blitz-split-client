package com.quare.blitzsplit.core.groups.domain.model

/*
 * Why: "Pela renda" is a shortcut: each one's income gives their whole percentage of every bill, which is all the
 * group keeps. The percentages add up to 100: each one gets the whole part of theirs, and the points left go to the
 * largest remainders, the first in the list on a tie.
 */
object IncomePercents {
    // Why: by member id, in the order given; all 0 while nobody has an income.
    fun of(incomes: List<Pair<String, Long>>): Map<String, Long> {
        val total = incomes.sumOf { (_, income) -> income.coerceAtLeast(0L) }
        if (total == 0L) return incomes.associate { (id, _) -> id to 0L }
        val full = GroupDefaultSplit.FULL_PERCENT
        val exact = incomes.map { (id, income) -> id to income.coerceAtLeast(0L) * full }
        val whole = exact.associate { (id, scaled) -> id to scaled / total }.toMutableMap()
        val left = full - whole.values.sum()
        exact
            .withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<Pair<String, Long>>> { it.value.second % total }
                    .thenBy { it.index },
            ).take(left.toInt())
            .forEach { (_, entry) -> whole[entry.first] = whole.getValue(entry.first) + 1 }
        return incomes.associate { (id, _) -> id to whole.getValue(id) }
    }
}
