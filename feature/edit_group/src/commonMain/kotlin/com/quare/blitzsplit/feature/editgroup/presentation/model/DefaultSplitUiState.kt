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

data class DefaultSplitUiState(
    // Why: "Apê 302 · para as despesas novas".
    val groupName: String,
    val currency: GroupCurrency,
    val mode: DefaultSplitMode,
    // Why: in the order they were added to the group, the creator first.
    val people: List<DefaultSplitPerson>,
    /*
     * Why: the percentages "Pela renda" shows while no income is typed on this device: the ones the group saved, or
     * the split's as it was when someone switched to it. The incomes stay with whoever typed them.
     */
    val incomeFallback: Map<String, Long>,
    val isOnline: Boolean,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
    val isSaving: Boolean,
    // Why: the device couldn't save the split; cleared by the next try.
    val showsSaveError: Boolean,
    // Why: Salvar stays off until the split differs from what the screen opened with, or someone new is in it.
    val hasChanges: Boolean,
) {
    // Why: each one's weight as Salvar saves it; worked out once per state, since every UI reads it on each keystroke.
    val weights: Map<String, Long> = people.associate { it.id to it.toWeightIn(mode) }.let { typed ->
        if (mode != DefaultSplitMode.INCOME) return@let typed
        val incomes = people.map { it.id to (AmountInput.parse(it.incomeText, currency) ?: 0L) }
        if (incomes.any { (_, income) -> income > 0 }) {
            IncomePercents.of(incomes)
        } else {
            people.associate { it.id to (incomeFallback[it.id] ?: 0L) }
        }
    }

    // Why: "Porcentagem" and "Pela renda" add up to 100; the others don't count percentages.
    val usesPercent: Boolean
        get() = mode == DefaultSplitMode.PERCENT || mode == DefaultSplitMode.INCOME

    val includedCount: Int
        get() = weights.values.count { it > 0 }

    // Why: "A soma dá 90%".
    val percentSum: Long
        get() = if (usesPercent) weights.values.sum() else GroupDefaultSplit.FULL_PERCENT

    // Why: "Faltam 10%" while positive, too much while negative.
    val remainingPercent: Long
        get() = GroupDefaultSplit.FULL_PERCENT - percentSum

    val addsUp: Boolean
        get() = includedCount > 0 && percentSum == GroupDefaultSplit.FULL_PERCENT

    // Why: "A soma dá 90%. Faltam 10%." in place of the example, with Salvar disabled.
    val showsPercentError: Boolean
        get() = isLoaded && usesPercent && percentSum != GroupDefaultSplit.FULL_PERCENT

    /*
     * Why: "Numa conta de R$ 100,00", or for "Igual" the first round amount everyone gets the same of ("Numa conta de
     * R$ 120,00: R$ 20,00 para cada um" among six).
     */
    val exampleTotalMinor: Long = exampleTotalOf(
        mode = mode,
        count = weights.values.count { it > 0 },
        currency = currency,
    )

    // Why: "R$ 20,00 para cada um", Igual's example.
    val exampleEachMinor: Long
        get() = if (includedCount == 0) 0L else exampleTotalMinor / includedCount

    val rows: List<DefaultSplitRow> = rowsOf(this)

    // Why: "Carla entrou no grupo. Ela começa com 1 parte; ajuste se precisar.", until someone saves.
    val joinedPeople: List<DefaultSplitPerson>
        get() = people.filter { it.isNew }

    val modes: List<DefaultSplitMode>
        get() = DefaultSplitMode.entries

    val canSave: Boolean
        get() = isLoaded && hasChanges && addsUp && !isSaving

    // Why: "Sem internet: a divisão nova vale neste aparelho agora e chega aos outros quando ele reconectar."
    val showsOfflineNotice: Boolean
        get() = !isOnline

    // Why: what Salvar saves.
    val defaultSplit: GroupDefaultSplit
        get() = GroupDefaultSplit(
            mode = mode,
            weights = weights,
        )

    // Why: the incomes typed, by member id in minor units, which stay on this device.
    val incomes: Map<String, Long>
        get() = people
            .mapNotNull { person -> AmountInput.parse(person.incomeText, currency)?.let { person.id to it } }
            .toMap()

    private companion object {
        const val BASIS_POINTS = 10_000L
        const val EXAMPLE_UNITS = 100L
        const val ROUND_UNITS = 10L

        fun DefaultSplitPerson.toWeightIn(mode: DefaultSplitMode): Long = when (mode) {
            DefaultSplitMode.EQUAL -> if (isIncluded) 1L else 0L
            DefaultSplitMode.SHARES -> parts
            DefaultSplitMode.PERCENT, DefaultSplitMode.INCOME -> AmountInput.parseWhole(percentText) ?: 0L
        }

        fun exampleTotalOf(
            mode: DefaultSplitMode,
            count: Int,
            currency: GroupCurrency,
        ): Long {
            val unit = ExpenseRules.minorPerMajorFor(currency)
            if (mode != DefaultSplitMode.EQUAL || count == 0) return EXAMPLE_UNITS * unit
            val step = findLeastCommonMultiple(ROUND_UNITS, count.toLong())
            return (EXAMPLE_UNITS + step - 1) / step * step * unit
        }

        fun findLeastCommonMultiple(
            a: Long,
            b: Long,
        ): Long = a / findGreatestCommonDivisor(a, b) * b

        tailrec fun findGreatestCommonDivisor(
            a: Long,
            b: Long,
        ): Long = if (b == 0L) a else findGreatestCommonDivisor(b, a % b)

        // Why: the cents a split can't share evenly go as in Rachar conta, to whoever pays: this device's user by default.
        fun rowsOf(state: DefaultSplitUiState): List<DefaultSplitRow> {
            val splitMode = SplitMode.of(state.mode)
            val entries = state.people.map {
                SplitEntry(
                    memberId = it.id,
                    value = state.weights[it.id] ?: 0L,
                )
            }
            val payerIds = state.people.filter { it.isMe }.map { it.id }
            val example = BillSplit
                .split(state.exampleTotalMinor, splitMode, entries, payerIds)
                ?.associate { it.memberId to it.amountMinor }
            val points = if (state.usesPercent) {
                entries.associate { it.memberId to it.value * (BASIS_POINTS / GroupDefaultSplit.FULL_PERCENT) }
            } else {
                BillSplit
                    .split(BASIS_POINTS, splitMode, entries, payerIds)
                    ?.associate { it.memberId to it.amountMinor }
                    .orEmpty()
            }
            return state.people.zip(entries) { person, entry ->
                DefaultSplitRow(
                    person = person,
                    weight = entry.value,
                    basisPoints = points[person.id] ?: 0L,
                    exampleMinor = example?.get(person.id) ?: if (state.usesPercent) {
                        entry.value * state.exampleTotalMinor / GroupDefaultSplit.FULL_PERCENT
                    } else {
                        null
                    },
                )
            }
        }
    }
}
