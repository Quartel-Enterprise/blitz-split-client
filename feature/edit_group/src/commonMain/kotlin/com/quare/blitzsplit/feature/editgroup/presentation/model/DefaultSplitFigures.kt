package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.AmountInput
import com.quare.blitzsplit.core.expenses.domain.model.BillSplit
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.model.IncomePercents
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency

// Why: what "Divisão padrão"'s form gives as it is typed, which DefaultSplitUiState reads.
data class DefaultSplitFigures(
    // Why: each one's weight as Salvar saves it.
    val weights: Map<String, Long>,
    // Why: the incomes typed, by member id in minor units, which stay on this device.
    val incomes: Map<String, Long>,
    val exampleTotalMinor: Long,
    val rows: List<DefaultSplitRow>,
) {
    companion object {
        private const val BASIS_POINTS = 10_000L
        private const val EXAMPLE_UNITS = 100L
        private const val ROUND_UNITS = 10L

        fun isPercentBased(mode: DefaultSplitMode): Boolean =
            mode == DefaultSplitMode.PERCENT || mode == DefaultSplitMode.INCOME

        fun of(
            mode: DefaultSplitMode,
            people: List<DefaultSplitPerson>,
            incomeFallback: Map<String, Long>,
            currency: GroupCurrency,
        ): DefaultSplitFigures {
            val incomes = people
                .mapNotNull { person -> AmountInput.parse(person.incomeText, currency)?.let { person.id to it } }
                .toMap()
            val weights = weightsOf(mode, people, incomes, incomeFallback)
            val exampleTotalMinor = exampleTotalOf(
                mode = mode,
                count = weights.values.count { it > 0 },
                currency = currency,
            )
            return DefaultSplitFigures(
                weights = weights,
                incomes = incomes,
                exampleTotalMinor = exampleTotalMinor,
                rows = rowsOf(mode, people, weights, exampleTotalMinor),
            )
        }

        private fun weightsOf(
            mode: DefaultSplitMode,
            people: List<DefaultSplitPerson>,
            incomes: Map<String, Long>,
            incomeFallback: Map<String, Long>,
        ): Map<String, Long> {
            if (mode != DefaultSplitMode.INCOME) return people.associate { it.id to it.toWeightIn(mode) }
            return if (incomes.values.any { it > 0 }) {
                IncomePercents.of(people.map { it.id to (incomes[it.id] ?: 0L) })
            } else {
                people.associate { it.id to (incomeFallback[it.id] ?: 0L) }
            }
        }

        private fun DefaultSplitPerson.toWeightIn(mode: DefaultSplitMode): Long = when (mode) {
            DefaultSplitMode.EQUAL -> if (isIncluded) 1L else 0L
            DefaultSplitMode.SHARES -> parts
            DefaultSplitMode.PERCENT, DefaultSplitMode.INCOME -> AmountInput.parseWhole(percentText) ?: 0L
        }

        private fun exampleTotalOf(
            mode: DefaultSplitMode,
            count: Int,
            currency: GroupCurrency,
        ): Long {
            val unit = ExpenseRules.minorPerMajorFor(currency)
            if (mode != DefaultSplitMode.EQUAL || count == 0) return EXAMPLE_UNITS * unit
            val step = findLeastCommonMultiple(ROUND_UNITS, count.toLong())
            return (EXAMPLE_UNITS + step - 1) / step * step * unit
        }

        private fun findLeastCommonMultiple(
            a: Long,
            b: Long,
        ): Long = a / findGreatestCommonDivisor(a, b) * b

        private tailrec fun findGreatestCommonDivisor(
            a: Long,
            b: Long,
        ): Long = if (b == 0L) a else findGreatestCommonDivisor(b, a % b)

        // Why: the cents a split can't share evenly go as in Rachar conta, to whoever pays: this device's user by default.
        private fun rowsOf(
            mode: DefaultSplitMode,
            people: List<DefaultSplitPerson>,
            weights: Map<String, Long>,
            exampleTotalMinor: Long,
        ): List<DefaultSplitRow> {
            val splitMode = SplitMode.of(mode)
            val usesPercent = isPercentBased(mode)
            val entries = people.map {
                SplitEntry(
                    memberId = it.id,
                    value = weights[it.id] ?: 0L,
                )
            }
            val payerIds = people.filter { it.isMe }.map { it.id }
            val example = BillSplit
                .split(exampleTotalMinor, splitMode, entries, payerIds)
                ?.associate { it.memberId to it.amountMinor }
            val points = if (usesPercent) {
                entries.associate { it.memberId to it.value * (BASIS_POINTS / GroupDefaultSplit.FULL_PERCENT) }
            } else {
                BillSplit
                    .split(BASIS_POINTS, splitMode, entries, payerIds)
                    ?.associate { it.memberId to it.amountMinor }
                    .orEmpty()
            }
            return people.zip(entries) { person, entry ->
                DefaultSplitRow(
                    person = person,
                    weight = entry.value,
                    basisPoints = points[person.id] ?: 0L,
                    exampleMinor = example?.get(person.id) ?: if (usesPercent) {
                        entry.value * exampleTotalMinor / GroupDefaultSplit.FULL_PERCENT
                    } else {
                        null
                    },
                )
            }
        }
    }
}
