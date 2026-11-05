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
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsCategoryTarget
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeopleView
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriod
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriodOption
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPersonRow
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsSettler
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
    val photoUrl: String?,
    val isMe: Boolean,
    val amountMinor: Double,
    val barFraction: Double,
    // Why: the row opens their profile (openPerson), Perfil for isMe; "Ex-membro" has none.
    val opensProfile: Boolean,
)

// Why: a person of "Acertos": averageDays 0 is "no mesmo dia", openMinor 0 shows nothing open.
class JsStatsSettler(
    val memberId: String,
    val name: String,
    val initials: String,
    val color: String,
    val photoUrl: String?,
    val isMe: Boolean,
    val averageDays: Int,
    val openMinor: Double,
)

// Why: "Acerta mais rápido" and "Demora mais para acertar", always two different people.
class JsStatsSettling(
    val fastest: JsStatsSettler,
    val slowest: JsStatsSettler,
)

class JsStatsBar(
    val startMillis: Double,
    val endMillis: Double,
    val amountMinor: Double,
    val expenseCount: Int,
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
    // Why: "Total gasto até agora" (5.1d) while a new device's first download still brings bills down.
    val isFirstDownload: Boolean,
    // Why: the bills still coming wait for the internet, said over the skeletons.
    val isWaitingForInternet: Boolean,
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
    // Why: the chart's value lines from 0 up, the last its top; empty while nothing was spent.
    val chartTicksMinor: Array<Double>,
    // Why: the dashed average line; null with one bar or nothing spent.
    val chartAverageMinor: Double?,
    val chartTopMinor: Double,
    val hasSparseLabels: Boolean,
    val grainNotes: Array<String>,
    val biggestExpense: JsStatsBiggestExpense?,
    // Why: "Acertos", null until two people had a debt in the period.
    val settling: JsStatsSettling?,
    // Why: "Por categoria" and the pie, the largest first.
    val categories: Array<JsStatsCategory>,
    val isGroupEmpty: Boolean,
    val isPeriodEmpty: Boolean,
    // Why: skeletons in place of either empty state while a first download may still bring the period's bills.
    val isLoadingBills: Boolean,
    val showsStats: Boolean,
    val showsPeriodPicker: Boolean,
    val isLoaded: Boolean,
    // Why: the home tab the group was opened from, which the sidebar selects ("home", "groups"…).
    val homeTab: String,
)

class JsStatsSubcategory(
    val category: JsCategory,
    val amountMinor: Double,
)

class JsStatsCategory(
    val category: JsCategory,
    val amountMinor: Double,
    val percent: Int,
    val barFraction: Double,
    val subcategories: Array<JsStatsSubcategory>,
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

    fun openPerson(memberId: String) {
        viewModel.onEvent(GroupStatsUiEvent.OnPersonClicked(memberId))
    }

    // Why: a main category of "By category"; target is "ring", "legend" or "row".
    fun openCategory(
        categoryId: String,
        target: String,
    ) {
        val statsTarget = StatsCategoryTarget.entries.firstOrNull { it.name.lowercase() == target } ?: return
        viewModel.onEvent(GroupStatsUiEvent.OnCategoryClicked(categoryId, statsTarget))
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
    isFirstDownload = state.isFirstDownload,
    isWaitingForInternet = state.isWaitingForInternet,
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
    chartTicksMinor = state.chartTicksMinor.map { it.toDouble() }.toTypedArray(),
    chartAverageMinor = state.chartAverageMinor?.toDouble(),
    chartTopMinor = state.chartTopMinor.toDouble(),
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
    settling = state.settling?.let { JsStatsSettling(toJsStatsSettler(it.fastest), toJsStatsSettler(it.slowest)) },
    categories = state.categories
        .map { row ->
            JsStatsCategory(
                category = toJsCategory(row.category),
                amountMinor = row.amountMinor.toDouble(),
                percent = row.percent,
                barFraction = row.barFraction,
                subcategories = row.subcategories
                    .map { JsStatsSubcategory(toJsCategory(it.category), it.amountMinor.toDouble()) }
                    .toTypedArray(),
            )
        }.toTypedArray(),
    isGroupEmpty = state.isGroupEmpty,
    isPeriodEmpty = state.isPeriodEmpty,
    isLoadingBills = state.isLoadingBills,
    showsStats = state.showsStats,
    showsPeriodPicker = state.showsPeriodPicker,
    isLoaded = state.isLoaded,
    homeTab = toJsHomeTab(state.homeTab),
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
    photoUrl = row.photoUrl,
    isMe = row.isMe,
    amountMinor = row.amountMinor.toDouble(),
    barFraction = row.barFraction,
    opensProfile = row.opensProfile,
)

private fun toJsStatsSettler(settler: StatsSettler): JsStatsSettler = JsStatsSettler(
    memberId = settler.memberId,
    name = settler.name,
    initials = settler.initials,
    color = settler.avatarColor.code,
    photoUrl = settler.photoUrl,
    isMe = settler.isMe,
    averageDays = settler.averageDays,
    openMinor = settler.openMinor.toDouble(),
)

private fun toJsStatsBar(bar: StatsBar): JsStatsBar = JsStatsBar(
    startMillis = bar.startMillis.toDouble(),
    endMillis = bar.endMillis.toDouble(),
    amountMinor = bar.amountMinor.toDouble(),
    expenseCount = bar.expenseCount,
    isToday = bar.isToday,
    isPeak = bar.isPeak,
    showsLabel = bar.showsLabel,
    showsDot = bar.showsDot,
)
