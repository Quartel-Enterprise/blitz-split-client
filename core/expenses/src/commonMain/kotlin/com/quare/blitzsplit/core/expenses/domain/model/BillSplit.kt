package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: the split math of "Rachar conta", the same on every platform. When a split isn't exact, the cents left over go
 * to whoever paid, while they are in the split; otherwise one each to the first members in it.
 */
object BillSplit {
    const val FULL_PERCENT = 100L

    // Why: each member's part, or null while the split doesn't add up to the total or has nobody in it.
    fun split(
        totalMinor: Long,
        mode: SplitMode,
        entries: List<SplitEntry>,
        payerId: String,
    ): List<ExpenseShare>? {
        val included = entries.filter { it.value > 0 }
        if (totalMinor <= 0 || included.isEmpty() || entries.any { it.value < 0 }) return null
        return when (mode) {
            SplitMode.EQUAL -> splitByWeight(
                totalMinor = totalMinor,
                entries = included.map { it.copy(value = 1) },
                payerId = payerId,
            )

            SplitMode.AMOUNT ->
                included
                    .takeIf { amounts -> amounts.sumOf { it.value } == totalMinor }
                    ?.map {
                        ExpenseShare(
                            memberId = it.memberId,
                            amountMinor = it.value,
                            weight = it.value,
                        )
                    }

            SplitMode.PERCENT ->
                included
                    .takeIf { percentages -> percentages.sumOf { it.value } == FULL_PERCENT }
                    ?.let {
                        splitByWeight(
                            totalMinor = totalMinor,
                            entries = it,
                            payerId = payerId,
                        )
                    }

            SplitMode.SHARES -> splitByWeight(
                totalMinor = totalMinor,
                entries = included,
                payerId = payerId,
            )
        }
    }

    /*
     * Why: "Mudar o total refaz a divisão por valor proporcionalmente, sem jogar fora o que foi ajustado". Parts that
     * added up to the old total add up to the new one, the cents left over going to whoever paid; parts that didn't
     * keep their proportion to the total.
     */
    fun calculateRescaledAmounts(
        entries: List<SplitEntry>,
        oldTotalMinor: Long,
        newTotalMinor: Long,
        payerId: String,
    ): List<SplitEntry> {
        if (oldTotalMinor <= 0 || newTotalMinor <= 0 || oldTotalMinor == newTotalMinor) return entries
        val included = entries.filter { it.value > 0 }
        if (included.isEmpty()) return entries
        val scaled = if (included.sumOf { it.value } == oldTotalMinor) {
            splitByWeight(
                totalMinor = newTotalMinor,
                entries = included,
                payerId = payerId,
            ).associate { it.memberId to it.amountMinor }
        } else {
            included.associate { it.memberId to proportionOf(newTotalMinor, it.value, oldTotalMinor) }
        }
        return entries.map { entry -> scaled[entry.memberId]?.let { entry.copy(value = it) } ?: entry }
    }

    // Why: the total in proportion to each weight, rounded down, with what rounding left over as the class says.
    private fun splitByWeight(
        totalMinor: Long,
        entries: List<SplitEntry>,
        payerId: String,
    ): List<ExpenseShare> {
        val totalWeight = entries.sumOf { it.value }
        val parts = entries.associate { it.memberId to proportionOf(totalMinor, it.value, totalWeight) }.toMutableMap()
        var leftover = totalMinor - parts.values.sum()
        if (payerId in parts) {
            parts[payerId] = parts.getValue(payerId) + leftover
            leftover = 0
        }
        // Why: the Double past a Long's reach may round a part up; the largest parts give that back.
        val givers = parts.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .iterator()
        while (leftover < 0 && givers.hasNext()) {
            val memberId = givers.next()
            parts[memberId] = parts.getValue(memberId) - 1
            leftover++
        }
        val receivers = entries.map { it.memberId }.iterator()
        while (leftover > 0 && receivers.hasNext()) {
            val memberId = receivers.next()
            parts[memberId] = parts.getValue(memberId) + 1
            leftover--
        }
        return entries.map {
            ExpenseShare(
                memberId = it.memberId,
                amountMinor = parts.getValue(it.memberId),
                weight = it.value,
            )
        }
    }

    /*
     * Why: total × weight ÷ of, rounded down. Exact while the product fits a Long; two parts typed in "Valores" near
     * the 100 million limit don't, and the leftover the caller hands out keeps the sum exact anyway. The Double may
     * round up, which makes that leftover negative.
     */
    private fun proportionOf(
        total: Long,
        weight: Long,
        of: Long,
    ): Long = if (weight <= Long.MAX_VALUE / total) {
        total * weight / of
    } else {
        (total.toDouble() * weight / of).toLong()
    }
}
