package com.quare.blitzsplit.feature.groupstats.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.DateSpan
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupSpending
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.PayerOrder
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.StatsGrain
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsBar
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsBiggestExpense
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsCategoryRow
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsGrainNote
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsGrainOption
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPayer
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeopleView
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriod
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPersonRow
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsRangeStyle
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsSubcategory
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlin.math.ceil

/*
 * Why: the statistics of one moment, from the group's bills on the device, today and the device's time zone, so the
 * ViewModel only keeps what the person picked.
 */
internal class StatsCalculator(
    private val group: Group,
    private val expenses: List<Expense>,
    private val today: LocalDate,
    private val timeZone: TimeZone,
    private val categories: CategoryTree,
) {
    // Why: "Toda a viagem" or "Todo o período", from the first to the last day with a bill.
    val allSpan: DateSpan = expenses
        .map { SpentDay.dateOf(it.spentAt, timeZone) }
        .let { dates -> if (dates.isEmpty()) DateSpan(today, today) else DateSpan(dates.min(), dates.max()) }

    // Why: a trip counts itself whole first; any other group starts the year before the rest, as the canvas lists it.
    val periods: List<StatsPeriod> = if (group.type == GroupType.TRIP) {
        listOf(StatsPeriod.ALL, StatsPeriod.THIS_MONTH, StatsPeriod.LAST_MONTH, StatsPeriod.CUSTOM)
    } else {
        listOf(
            StatsPeriod.THIS_YEAR,
            StatsPeriod.THIS_MONTH,
            StatsPeriod.LAST_MONTH,
            StatsPeriod.ALL,
            StatsPeriod.CUSTOM,
        )
    }

    fun findSpanOrNull(
        period: StatsPeriod,
        customSpan: DateSpan?,
    ): DateSpan? {
        val monthStart = LocalDate(today.year, today.month, 1)
        return when (period) {
            StatsPeriod.ALL -> allSpan

            StatsPeriod.THIS_YEAR -> DateSpan(LocalDate(today.year, 1, 1), today)

            StatsPeriod.THIS_MONTH -> DateSpan(monthStart, today)

            StatsPeriod.LAST_MONTH -> DateSpan(
                start = monthStart.minus(DatePeriod(months = 1)),
                end = monthStart.minus(DatePeriod(days = 1)),
            )

            StatsPeriod.CUSTOM -> customSpan
        }
    }

    fun toRangeStyle(
        period: StatsPeriod,
        span: DateSpan,
    ): StatsRangeStyle = when {
        period == StatsPeriod.THIS_MONTH || period == StatsPeriod.LAST_MONTH -> StatsRangeStyle.MONTH
        span.canSplitBy(StatsGrain.DAY) -> StatsRangeStyle.DAYS
        else -> StatsRangeStyle.MONTHS
    }

    fun createSpending(span: DateSpan): GroupSpending = GroupSpending.of(expenses, span, timeZone)

    // Why: "Ano" compares the years of the whole period, so it also shows where picking it switches to that period.
    fun isEnabled(
        grain: StatsGrain,
        span: DateSpan,
    ): Boolean = span.canSplitBy(grain) || (grain == StatsGrain.YEAR && allSpan.canSplitBy(StatsGrain.YEAR))

    // Why: "Abre no que cabe no período (Dia até 3 meses, Mês acima disso)", or the grain kept on the device.
    fun findGrain(
        kept: StatsGrain?,
        span: DateSpan,
    ): StatsGrain {
        val fitting = if (span.canSplitBy(StatsGrain.DAY)) StatsGrain.DAY else StatsGrain.MONTH
        return kept?.takeIf { span.canSplitBy(it) } ?: fitting
    }

    fun createGrainOptions(span: DateSpan): List<StatsGrainOption> = StatsGrain.entries.map { grain ->
        StatsGrainOption(
            grain = grain,
            isEnabled = isEnabled(grain, span),
        )
    }

    fun createGrainNotes(span: DateSpan): List<StatsGrainNote> {
        val longerOff = listOf(StatsGrain.WEEK, StatsGrain.MONTH, StatsGrain.YEAR).filterNot { isEnabled(it, span) }
        val longerNotes = when (longerOff) {
            listOf(StatsGrain.WEEK, StatsGrain.MONTH, StatsGrain.YEAR) ->
                listOf(StatsGrainNote.WEEK_MONTH_AND_YEAR_NEED_LONGER_PERIOD)

            listOf(StatsGrain.MONTH, StatsGrain.YEAR) -> listOf(StatsGrainNote.MONTH_AND_YEAR_NEED_LONGER_PERIOD)

            // Why: a period in one week that crosses into another month (Oct 31 to Nov 1) has Mês but not Semana.
            else -> listOfNotNull(
                StatsGrainNote.WEEK_NEEDS_LONGER_PERIOD.takeIf { StatsGrain.WEEK in longerOff },
                StatsGrainNote.YEAR_NEEDS_LONGER_PERIOD.takeIf { StatsGrain.YEAR in longerOff },
            )
        }
        return listOfNotNull(
            StatsGrainNote.DAY_NEEDS_SHORT_PERIOD.takeUnless { span.canSplitBy(StatsGrain.DAY) },
        ) + longerNotes + listOfNotNull(
            StatsGrainNote.YEAR_SWITCHES_PERIOD.takeIf {
                isEnabled(StatsGrain.YEAR, span) && !span.canSplitBy(StatsGrain.YEAR)
            },
        )
    }

    fun createBars(
        spending: GroupSpending,
        span: DateSpan,
        grain: StatsGrain,
    ): List<StatsBar> {
        val buckets = span.splitBy(grain)
        // Why: each bill's day once, into the bucket holding it (the buckets follow each other in order).
        val amountsByBucket = LongArray(buckets.size)
        spending.expenses.forEach { expense ->
            val date = SpentDay.dateOf(expense.spentAt, timeZone)
            val index = buckets.binarySearch { bucket ->
                when {
                    bucket.end < date -> -1
                    bucket.start > date -> 1
                    else -> 0
                }
            }
            if (index >= 0) amountsByBucket[index] += expense.amountMinor
        }
        val amounts = amountsByBucket.toList()
        val peak = amounts.indexOf(amounts.maxOrNull()?.takeIf { it > 0 })
        val labelStep = if (buckets.size <= maxLabelsOf(grain)) 1 else ceil(buckets.size / SPARSE_LABELS).toInt()
        return buckets.mapIndexed { index, bucket ->
            StatsBar(
                startMillis = SpentDay.startOf(bucket.start, timeZone),
                endMillis = SpentDay.startOf(bucket.end, timeZone),
                amountMinor = amounts[index],
                isToday = grain == StatsGrain.DAY && bucket.start == today,
                isPeak = index == peak,
                showsLabel = index % labelStep == 0,
                showsDot = buckets.size <= MAX_DOTS || index == peak,
            )
        }
    }

    fun createPeople(
        spending: GroupSpending,
        view: StatsPeopleView,
    ): List<StatsPersonRow> {
        val amounts = if (view == StatsPeopleView.CONSUMPTION) spending.consumedByMember else spending.paidByMember
        val memberIds = group.members.map { it.id }.toSet()
        val members = group.members.map { member ->
            StatsPersonRow(
                memberId = member.id,
                name = member.name,
                initials = Initials.ofMember(member.name, member.isMe),
                avatarColor = member.avatarColor,
                isMe = member.isMe,
                amountMinor = amounts[member.id] ?: 0L,
                barFraction = 0.0,
            )
        }
        val formerIds = (spending.consumedByMember.keys + spending.paidByMember.keys).filterNot { it in memberIds }
        val formers = formerIds.map { id ->
            StatsPersonRow(
                memberId = id,
                name = null,
                initials = "",
                avatarColor = AvatarColor.entries.first(),
                isMe = false,
                amountMinor = amounts[id] ?: 0L,
                barFraction = 0.0,
            )
        }
        val rows = (members + formers).sortedByDescending { it.amountMinor }
        val largest = rows.firstOrNull()?.amountMinor ?: 0L
        return rows.map { row ->
            row.copy(barFraction = if (largest > 0) row.amountMinor.toDouble() / largest else 0.0)
        }
    }

    fun createBiggestOrNull(spending: GroupSpending): StatsBiggestExpense? {
        val expense = spending.biggest ?: return null
        val membersById = group.members.associateBy { it.id }
        return StatsBiggestExpense(
            expenseId = expense.id,
            description = expense.description,
            spentAt = expense.spentAt,
            payers = PayerOrder(group.members).sort(expense.payers.map { it.memberId }).map { id ->
                StatsPayer(
                    name = membersById[id]?.name,
                    isMe = membersById[id]?.isMe == true,
                )
            },
            amountMinor = expense.amountMinor,
            isPending = expense.isPending,
        )
    }

    /*
     * Why: each bill under the category it reads as (a deleted group category as its parent), percentages rounded so
     * they add up to 100 by the largest remainders.
     */
    fun createCategories(spending: GroupSpending): List<StatsCategoryRow> {
        val byCategory = spending.expenses.groupBy { categories.resolve(it.categoryId) }
        val byMain = byCategory.entries.groupBy({ categories.mainOf(it.key) }, { it })
        val total = spending.totalMinor
        val mains = byMain
            .map { (main, entries) ->
                val subcategories = entries
                    .filter { (category, _) -> category.id != main.id }
                    .map { (category, bills) -> StatsSubcategory(category, bills.sumOf { it.amountMinor }) }
                    .sortedByDescending { it.amountMinor }
                main to (entries.sumOf { (_, bills) -> bills.sumOf { it.amountMinor } } to subcategories)
            }.sortedByDescending { it.second.first }
        val percents = findRoundedPercents(mains.map { it.second.first }, total)
        val largest = mains.firstOrNull()?.second?.first ?: 0L
        return mains.mapIndexed { index, (main, amounts) ->
            StatsCategoryRow(
                category = main,
                amountMinor = amounts.first,
                percent = percents[index],
                barFraction = if (largest > 0) amounts.first.toDouble() / largest else 0.0,
                subcategories = amounts.second,
            )
        }
    }

    private fun findRoundedPercents(
        amounts: List<Long>,
        total: Long,
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

    private fun maxLabelsOf(grain: StatsGrain): Int = when (grain) {
        StatsGrain.WEEK -> MAX_WEEK_LABELS
        StatsGrain.MONTH -> MAX_MONTH_LABELS
        else -> MAX_LABELS
    }

    private companion object {
        const val PERCENT_UNITS = 100L
        const val MAX_DOTS = 12
        const val MAX_LABELS = 7

        // Why: "19 a 25 out" is long, so few weeks fit under a phone's chart.
        const val MAX_WEEK_LABELS = 3

        // Why: "jan" to "dez" fit a year under a phone's chart.
        const val MAX_MONTH_LABELS = 12
        const val SPARSE_LABELS = 6.0
    }
}
