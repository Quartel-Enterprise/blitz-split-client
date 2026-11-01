package com.quare.blitzsplit.feature.groupstats.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityUnreadCount
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.DateSpan
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.StatsChartPreferences
import com.quare.blitzsplit.core.model.StatsChartType
import com.quare.blitzsplit.core.model.StatsGrain
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.findHomeTabUnder
import com.quare.blitzsplit.core.navigation.openHomeTab
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveStatsChartPreferences
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateStatsChartPreferences
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeopleView
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriod
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriodOption
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsRangeStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: "Estatísticas do grupo" (6.6), from the bills on the device, offline included, so a sync bringing other
 * members' bills redraws it. The period and Consumo or Pagou are only this screen's; Barras or Linha and the grain
 * are kept on the device for every group.
 */
class GroupStatsViewModel(
    private val route: Route.GroupStats,
    private val navigator: Navigator,
    private val currentTimeMillis: CurrentTimeMillis,
    private val currentTimeZone: CurrentTimeZone,
    private val updateStatsChartPreferences: UpdateStatsChartPreferences,
    observeGroup: ObserveGroup,
    observeExpenses: ObserveExpenses,
    observeStatsChartPreferences: ObserveStatsChartPreferences,
    observeActivityUnreadCount: ObserveActivityUnreadCount,
) : ViewModel() {
    val uiState: StateFlow<GroupStatsUiState>
        field = MutableStateFlow(
            GroupStatsUiState(
                groupName = "",
                currency = GroupCurrency.BRL,
                isTrip = true,
                period = StatsPeriod.ALL,
                periodStartMillis = 0L,
                periodEndMillis = 0L,
                periodRangeStyle = StatsRangeStyle.DAYS,
                periodOptions = emptyList(),
                allStartMillis = null,
                allEndMillis = null,
                allRangeStyle = StatsRangeStyle.DAYS,
                totalMinor = 0L,
                expenseCount = 0,
                pendingCount = 0,
                memberCount = 0,
                averagePerPersonMinor = 0L,
                myShareMinor = null,
                mySharePercent = 0,
                myPaidMinor = null,
                myPaidCount = 0,
                peopleView = StatsPeopleView.CONSUMPTION,
                people = emptyList(),
                chartType = StatsChartType.BARS,
                grain = StatsGrain.DAY,
                grains = emptyList(),
                bars = emptyList(),
                grainNotes = emptyList(),
                biggestExpense = null,
                isLoaded = false,
                homeTab = navigator.backStack.value.findHomeTabUnder(route),
                activityUnreadCount = 0,
            ),
        )

    val stateWatcher: FlowWatcher<GroupStatsUiState> = FlowWatcher(uiState)

    private var group: Group? = null
    private var expenses: List<Expense> = emptyList()
    private var chart = StatsChartPreferences.initial

    // Why: the last pick not stored yet; older echoes of the database never undo it while it is saving.
    private var pendingChart: StatsChartPreferences? = null
    private var period = StatsPeriod.ALL
    private var customSpan: DateSpan? = null
    private var peopleView = StatsPeopleView.CONSUMPTION

    // Why: the group went while a bill covered this screen, which then leaves as soon as it is on top again.
    private var isGone = false

    init {
        combine(
            observeGroup(route.groupId),
            observeExpenses(route.groupId),
            observeStatsChartPreferences(),
            ::Triple,
        ).onEach { (group, expenses, chart) ->
            this.group = group
            this.expenses = expenses
            keepStoredChart(chart)
            isGone = group == null
            if (group == null) leave() else render()
        }.launchIn(viewModelScope)
        navigator.backStack
            .onEach { if (isGone) leave() }
            .launchIn(viewModelScope)
        observeActivityUnreadCount()
            .onEach { count -> uiState.update { it.copy(activityUnreadCount = count) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: GroupStatsUiEvent) {
        when (event) {
            GroupStatsUiEvent.OnBackClicked -> leave()

            is GroupStatsUiEvent.OnRailTabClicked -> if (isOnTop()) navigator.openHomeTab(event.tab)

            is GroupStatsUiEvent.OnPeriodPicked -> pickPeriod(event.period)

            is GroupStatsUiEvent.OnCustomPeriodPicked -> pickCustomPeriod(event.startMillis, event.endMillis)

            is GroupStatsUiEvent.OnPeopleViewPicked -> {
                peopleView = event.view
                render()
            }

            is GroupStatsUiEvent.OnChartTypePicked -> keepChart(chart.copy(chartType = event.chartType))

            is GroupStatsUiEvent.OnGrainPicked -> pickGrain(event.grain)

            GroupStatsUiEvent.OnBiggestExpenseClicked -> openBiggestExpense()

            GroupStatsUiEvent.OnSplitBillClicked -> open(
                Route.SplitBill(
                    groupId = route.groupId,
                    expenseId = null,
                    picksGroupFirst = false,
                ),
            )

            GroupStatsUiEvent.OnWholePeriodClicked -> pickPeriod(StatsPeriod.ALL)
        }
    }

    private fun pickPeriod(period: StatsPeriod) {
        if (period == StatsPeriod.CUSTOM && customSpan == null) return
        this.period = period
        render()
    }

    // Why: "Escolher datas…" never goes past today, whatever a typed date (the web's date field) says.
    private fun pickCustomPeriod(
        startMillis: Long,
        endMillis: Long,
    ) {
        val timeZone = currentTimeZone()
        val today = SpentDay.dateOf(currentTimeMillis(), timeZone)
        val first = minOf(SpentDay.dateOf(startMillis, timeZone), today)
        val last = minOf(SpentDay.dateOf(endMillis, timeZone), today)
        customSpan = DateSpan(minOf(first, last), maxOf(first, last))
        period = StatsPeriod.CUSTOM
        render()
    }

    // Why: "Escolher Ano troca o período para Todo o período"; a grain the period can't show does nothing.
    private fun pickGrain(grain: StatsGrain) {
        val calculator = createCalculatorOrNull() ?: return
        val span = calculator.findSpanOrNull(period, customSpan) ?: return
        if (!calculator.isEnabled(grain, span)) return
        if (!span.canSplitBy(grain)) period = StatsPeriod.ALL
        keepChart(chart.copy(grain = grain))
    }

    private fun keepStoredChart(stored: StatsChartPreferences) {
        val pending = pendingChart
        if (pending == null || pending == stored) {
            pendingChart = null
            chart = stored
        }
    }

    private fun keepChart(chart: StatsChartPreferences) {
        this.chart = chart
        pendingChart = chart
        render()
        viewModelScope.launch { updateStatsChartPreferences(chart) }
    }

    private fun createCalculatorOrNull(): StatsCalculator? {
        val group = group ?: return null
        val timeZone = currentTimeZone()
        return StatsCalculator(
            group = group,
            expenses = expenses,
            today = SpentDay.dateOf(currentTimeMillis(), timeZone),
            timeZone = timeZone,
        )
    }

    private fun render() {
        val group = group ?: return
        val calculator = createCalculatorOrNull() ?: return
        val timeZone = currentTimeZone()
        val span = calculator.findSpanOrNull(period, customSpan) ?: calculator.allSpan
        val spending = calculator.createSpending(span)
        val grain = calculator.findGrain(chart.grain, span)
        val me = group.members.firstOrNull { it.isMe }
        val myShare = me?.let { spending.consumedByMember[it.id] ?: 0L }
        val memberCount = group.members.size
        uiState.value = GroupStatsUiState(
            groupName = group.name,
            currency = group.currency,
            isTrip = group.type == GroupType.TRIP,
            period = period,
            periodStartMillis = SpentDay.startOf(span.start, timeZone),
            periodEndMillis = SpentDay.startOf(span.end, timeZone),
            periodRangeStyle = calculator.toRangeStyle(period, span),
            periodOptions = calculator.periods.map { option ->
                val optionSpan = calculator.findSpanOrNull(option, customSpan)
                StatsPeriodOption(
                    period = option,
                    startMillis = optionSpan?.let { SpentDay.startOf(it.start, timeZone) },
                    endMillis = optionSpan?.let { SpentDay.startOf(it.end, timeZone) },
                    rangeStyle = optionSpan?.let { calculator.toRangeStyle(option, it) } ?: StatsRangeStyle.DAYS,
                    expenseCount = optionSpan?.let { calculator.createSpending(it).expenses.size } ?: 0,
                )
            },
            allStartMillis = SpentDay.startOf(calculator.allSpan.start, timeZone).takeIf { expenses.isNotEmpty() },
            allEndMillis = SpentDay.startOf(calculator.allSpan.end, timeZone).takeIf { expenses.isNotEmpty() },
            allRangeStyle = calculator.toRangeStyle(StatsPeriod.ALL, calculator.allSpan),
            totalMinor = spending.totalMinor,
            expenseCount = spending.expenses.size,
            pendingCount = spending.pendingCount,
            memberCount = memberCount,
            averagePerPersonMinor = toAveragePerPerson(spending.totalMinor, memberCount),
            myShareMinor = myShare,
            mySharePercent = if (myShare != null && spending.totalMinor > 0) {
                toRoundedQuotient(myShare * PERCENT, spending.totalMinor).toInt()
            } else {
                0
            },
            myPaidMinor = me?.let { spending.paidByMember[it.id] ?: 0L },
            myPaidCount = me?.let { spending.countPaidBy(it.id) } ?: 0,
            peopleView = peopleView,
            people = calculator.createPeople(spending, peopleView),
            chartType = chart.chartType,
            grain = grain,
            grains = calculator.createGrainOptions(span),
            bars = calculator.createBars(spending, span, grain),
            grainNotes = calculator.createGrainNotes(span),
            biggestExpense = calculator.createBiggestOrNull(spending),
            isLoaded = true,
            homeTab = uiState.value.homeTab,
            activityUnreadCount = uiState.value.activityUnreadCount,
        )
    }

    private fun openBiggestExpense() {
        val expense = uiState.value.biggestExpense ?: return
        open(
            Route.BillDetail(
                groupId = route.groupId,
                expenseId = expense.expenseId,
            ),
        )
    }

    private fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    private fun open(next: Route) {
        if (isOnTop()) navigator.navigate(next)
    }

    // Why: only while it is still on top: a native back (a swipe, the browser's) may have closed it.
    private fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }

    private fun toAveragePerPerson(
        totalMinor: Long,
        memberCount: Int,
    ): Long = if (memberCount > 0) toRoundedQuotient(totalMinor, memberCount.toLong()) else 0L

    private fun toRoundedQuotient(
        dividend: Long,
        divisor: Long,
    ): Long = (dividend + divisor / 2) / divisor

    private companion object {
        const val PERCENT = 100L
    }
}
