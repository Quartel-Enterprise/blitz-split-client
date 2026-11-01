@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.model.StatsChartType
import com.quare.blitzsplit.core.model.StatsGrain
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsBar
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeopleView
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriod
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriodOption
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPersonRow
import com.quare.blitzsplit.feature.groupstats.presentation.viewmodel.GroupStatsViewModel
import kotlinx.coroutines.flow.map
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

/*
 * Why: an entry of the period menu. period is "all", "this-year", "this-month", "last-month" or "custom"; the days
 * are null for "custom" before any was picked; rangeStyle is "month", "days" or "months".
 */
class JsStatsPeriodOption(
    val period: String,
    val startMillis: Double?,
    val endMillis: Double?,
    val rangeStyle: String,
    val expenseCount: Int,
)

// Why: a person of "Por pessoa", amounts in the currency's minor unit; name null for "Ex-membro".
class JsStatsPerson(
    val memberId: String,
    val name: String?,
    val initials: String,
    val color: String,
    val isMe: Boolean,
    val amountMinor: Double,
    val barFraction: Double,
)

class JsStatsBar(
    val startMillis: Double,
    val endMillis: Double,
    val amountMinor: Double,
    val isToday: Boolean,
    val isPeak: Boolean,
    val showsLabel: Boolean,
    val showsDot: Boolean,
)

// Why: "Dia", "Semana", "Mês" or "Ano" as "day", "week", "month" or "year".
class JsStatsGrain(
    val grain: String,
    val isEnabled: Boolean,
)

// Why: who paid "Maior despesa", a null name for someone who left the group.
class JsStatsPayer(
    val name: String?,
    val isMe: Boolean,
)

class JsStatsBiggestExpense(
    val expenseId: String,
    val description: String,
    val spentAt: Double,
    val payers: Array<JsStatsPayer>,
    val amountMinor: Double,
    val isPending: Boolean,
)

/*
 * Why: "Estatísticas do grupo". period and the options as in JsStatsPeriodOption; peopleView "consumption" or
 * "paid"; chartType "bars" or "line"; grainNotes the StatsGrainNote names.
 */
class JsGroupStatsState(
    val groupName: String,
    // Why: the ISO 4217 code ("BRL") and its digits after the decimal separator.
    val currency: String,
    val fractionDigits: Int,
    val isTrip: Boolean,
    val period: String,
    val periodStartMillis: Double,
    val periodEndMillis: Double,
    val periodRangeStyle: String,
    val periodOptions: Array<JsStatsPeriodOption>,
    val allStartMillis: Double?,
    val allEndMillis: Double?,
    val allRangeStyle: String,
    val totalMinor: Double,
    val expenseCount: Int,
    val pendingCount: Int,
    val memberCount: Int,
    val averagePerPersonMinor: Double,
    val myShareMinor: Double?,
    val mySharePercent: Int,
    val myPaidMinor: Double?,
    val myPaidCount: Int,
    val peopleView: String,
    val people: Array<JsStatsPerson>,
    val chartType: String,
    val grain: String,
    val grains: Array<JsStatsGrain>,
    val bars: Array<JsStatsBar>,
    val hasSparseLabels: Boolean,
    val grainNotes: Array<String>,
    val biggestExpense: JsStatsBiggestExpense?,
    val isGroupEmpty: Boolean,
    val isPeriodEmpty: Boolean,
    val showsStats: Boolean,
    val showsPeriodPicker: Boolean,
    val isLoaded: Boolean,
)

class GroupStatsPresenter(
    route: JsRoute,
) {
    private val statsRoute = route.route as Route.GroupStats
    private val holder = ViewModelHolder(GroupStatsViewModel::class) {
        KoinPlatform.getKoin().get { parametersOf(statsRoute) }
    }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsGroupStatsState) -> Unit): JsWatch {
        val states = viewModel.uiState.map(::toJsGroupStatsState)
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(GroupStatsUiEvent.OnBackClicked)
    }

    // Why: any period's code but "custom", which pickDates sets.
    fun pickPeriod(code: String) {
        val period = StatsPeriod.entries.firstOrNull { toPeriodCode(it) == code } ?: return
        viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(period))
    }

    // Why: any instant of the first and the last day picked, in the browser's time zone.
    fun pickDates(
        startMillis: Double,
        endMillis: Double,
    ) {
        viewModel.onEvent(GroupStatsUiEvent.OnCustomPeriodPicked(startMillis.toLong(), endMillis.toLong()))
    }

    fun showConsumption() {
        viewModel.onEvent(GroupStatsUiEvent.OnPeopleViewPicked(StatsPeopleView.CONSUMPTION))
    }

    fun showPaid() {
        viewModel.onEvent(GroupStatsUiEvent.OnPeopleViewPicked(StatsPeopleView.PAID))
    }

    fun pickChartType(code: String) {
        val chartType = StatsChartType.fromCodeOrNull(code) ?: return
        viewModel.onEvent(GroupStatsUiEvent.OnChartTypePicked(chartType))
    }

    fun pickGrain(code: String) {
        val grain = StatsGrain.fromCodeOrNull(code) ?: return
        viewModel.onEvent(GroupStatsUiEvent.OnGrainPicked(grain))
    }

    fun openBiggestExpense() {
        viewModel.onEvent(GroupStatsUiEvent.OnBiggestExpenseClicked)
    }

    fun splitBill() {
        viewModel.onEvent(GroupStatsUiEvent.OnSplitBillClicked)
    }

    fun showWholePeriod() {
        viewModel.onEvent(GroupStatsUiEvent.OnWholePeriodClicked)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsGroupStatsState(state: GroupStatsUiState): JsGroupStatsState = JsGroupStatsState(
    groupName = state.groupName,
    currency = state.currency.code,
    fractionDigits = state.currency.fractionDigits,
    isTrip = state.isTrip,
    period = toPeriodCode(state.period),
    periodStartMillis = state.periodStartMillis.toDouble(),
    periodEndMillis = state.periodEndMillis.toDouble(),
    periodRangeStyle = state.periodRangeStyle.name.lowercase(),
    periodOptions = state.periodOptions.map(::toJsStatsPeriodOption).toTypedArray(),
    allStartMillis = state.allStartMillis?.toDouble(),
    allEndMillis = state.allEndMillis?.toDouble(),
    allRangeStyle = state.allRangeStyle.name.lowercase(),
    totalMinor = state.totalMinor.toDouble(),
    expenseCount = state.expenseCount,
    pendingCount = state.pendingCount,
    memberCount = state.memberCount,
    averagePerPersonMinor = state.averagePerPersonMinor.toDouble(),
    myShareMinor = state.myShareMinor?.toDouble(),
    mySharePercent = state.mySharePercent,
    myPaidMinor = state.myPaidMinor?.toDouble(),
    myPaidCount = state.myPaidCount,
    peopleView = if (state.peopleView == StatsPeopleView.PAID) "paid" else "consumption",
    people = state.people.map(::toJsStatsPerson).toTypedArray(),
    chartType = state.chartType.code,
    grain = state.grain.code,
    grains = state.grains.map { JsStatsGrain(it.grain.code, it.isEnabled) }.toTypedArray(),
    bars = state.bars.map(::toJsStatsBar).toTypedArray(),
    hasSparseLabels = state.hasSparseLabels,
    grainNotes = state.grainNotes.map { it.name }.toTypedArray(),
    biggestExpense = state.biggestExpense?.let { expense ->
        JsStatsBiggestExpense(
            expenseId = expense.expenseId,
            description = expense.description,
            spentAt = expense.spentAt.toDouble(),
            payers = expense.payers.map { JsStatsPayer(it.name, it.isMe) }.toTypedArray(),
            amountMinor = expense.amountMinor.toDouble(),
            isPending = expense.isPending,
        )
    },
    isGroupEmpty = state.isGroupEmpty,
    isPeriodEmpty = state.isPeriodEmpty,
    showsStats = state.showsStats,
    showsPeriodPicker = state.showsPeriodPicker,
    isLoaded = state.isLoaded,
)

private fun toPeriodCode(period: StatsPeriod): String = period.name.lowercase().replace('_', '-')

private fun toJsStatsPeriodOption(option: StatsPeriodOption): JsStatsPeriodOption = JsStatsPeriodOption(
    period = toPeriodCode(option.period),
    startMillis = option.startMillis?.toDouble(),
    endMillis = option.endMillis?.toDouble(),
    rangeStyle = option.rangeStyle.name.lowercase(),
    expenseCount = option.expenseCount,
)

private fun toJsStatsPerson(row: StatsPersonRow): JsStatsPerson = JsStatsPerson(
    memberId = row.memberId,
    name = row.name,
    initials = row.initials,
    color = row.avatarColor.code,
    isMe = row.isMe,
    amountMinor = row.amountMinor.toDouble(),
    barFraction = row.barFraction,
)

private fun toJsStatsBar(bar: StatsBar): JsStatsBar = JsStatsBar(
    startMillis = bar.startMillis.toDouble(),
    endMillis = bar.endMillis.toDouble(),
    amountMinor = bar.amountMinor.toDouble(),
    isToday = bar.isToday,
    isPeak = bar.isPeak,
    showsLabel = bar.showsLabel,
    showsDot = bar.showsDot,
)
