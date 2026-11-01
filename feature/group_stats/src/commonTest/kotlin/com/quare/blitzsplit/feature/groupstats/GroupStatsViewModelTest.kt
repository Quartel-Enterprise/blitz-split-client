package com.quare.blitzsplit.feature.groupstats

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.model.StatsChartPreferences
import com.quare.blitzsplit.core.model.StatsChartType
import com.quare.blitzsplit.core.model.StatsGrain
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsGrainNote
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPayer
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeopleView
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriod
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsRangeStyle
import com.quare.blitzsplit.feature.groupstats.presentation.viewmodel.GroupStatsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class GroupStatsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var chart: MutableStateFlow<StatsChartPreferences>
    private lateinit var keptCharts: MutableList<StatsChartPreferences>
    private lateinit var viewModel: GroupStatsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a trip with bills WHEN the statistics open THEN sum the whole trip`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(StatsPeriod.ALL, state.period)
        assertEquals(millisOf("2026-10-24T00:00:00Z"), state.periodStartMillis)
        assertEquals(millisOf("2026-10-28T00:00:00Z"), state.periodEndMillis)
        assertEquals(StatsRangeStyle.DAYS, state.periodRangeStyle)
        assertEquals(90_000L, state.totalMinor)
        assertEquals(2, state.expenseCount)
        assertEquals(3, state.memberCount)
        assertEquals(30_000L, state.averagePerPersonMinor)
        assertEquals(25_000L, state.myShareMinor)
        assertEquals(28, state.mySharePercent)
        assertEquals(60_000L, state.myPaidMinor)
        assertEquals(1, state.myPaidCount)
        assertTrue(state.isTrip)
        assertTrue(state.showsStats)
    }

    @Test
    fun `GIVEN a trip WHEN the statistics open THEN list the trip whole first and the dates last`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val options = viewModel.uiState.value.periodOptions

            // Then
            assertEquals(
                listOf(StatsPeriod.ALL, StatsPeriod.THIS_MONTH, StatsPeriod.LAST_MONTH, StatsPeriod.CUSTOM),
                options.map { it.period },
            )
            assertEquals(listOf(2, 2, 0, 0), options.map { it.expenseCount })
            assertEquals(StatsRangeStyle.MONTH, options[1].rangeStyle)
            assertNull(options[3].startMillis)
        }

    @Test
    fun `GIVEN a bill in the queue WHEN the statistics open THEN count it with the queue`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            stored = tripBills + expense(
                "taxi",
                1_000,
                spentAt = today,
                isPending = true,
            ),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(3, state.expenseCount)
        assertEquals(1, state.pendingCount)
        assertEquals(91_000L, state.totalMinor)
    }

    @Test
    fun `GIVEN Consumo WHEN the statistics open THEN each part comes the largest first`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val people = viewModel.uiState.value.people

        // Then
        assertEquals(listOf(ANA, BRUNO, PIERRE), people.map { it.memberId })
        assertEquals(listOf(35_000L, 30_000L, 25_000L), people.map { it.amountMinor })
        assertEquals(1.0, people.first().barFraction)
        assertTrue(people.single { it.memberId == PIERRE }.isMe)
    }

    @Test
    fun `GIVEN Consumo WHEN picking Pagou THEN shows what each one paid with nothing for who paid nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPeopleViewPicked(StatsPeopleView.PAID))

            // Then
            val state = viewModel.uiState.value
            assertEquals(StatsPeopleView.PAID, state.peopleView)
            assertEquals(listOf(PIERRE, ANA, BRUNO), state.people.map { it.memberId })
            assertEquals(listOf(60_000L, 30_000L, 0L), state.people.map { it.amountMinor })
            assertEquals(0.0, state.people.last().barFraction)
        }

    @Test
    fun `GIVEN a bill split with someone who left WHEN the statistics open THEN shows them as a former member`() =
        runTest(testDispatcher) {
            // Given
            val withFormer = expense(
                id = "dinner",
                amountMinor = 20_000,
                spentAt = today,
                payers = listOf(paid(FORMER, 20_000)),
                shares = listOf(share(FORMER, 10_000), share(PIERRE, 10_000)),
            )
            prepareScenario(stored = listOf(withFormer))

            // When
            val former = viewModel.uiState.value.people
                .single { it.memberId == FORMER }

            // Then
            assertNull(former.name)
            assertEquals(10_000L, former.amountMinor)
        }

    @Test
    fun `GIVEN a trip over two weeks WHEN the statistics open THEN go by day with month and year off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(StatsGrain.DAY, state.grain)
            assertEquals(
                listOf(true, true, false, false),
                state.grains.map { it.isEnabled },
            )
            assertEquals(listOf(StatsGrainNote.MONTH_AND_YEAR_NEED_LONGER_PERIOD), state.grainNotes)
            assertEquals(listOf(30_000L, 0L, 0L, 0L, 60_000L), state.bars.map { it.amountMinor })
            assertEquals(millisOf("2026-10-28T00:00:00Z"), state.peak?.startMillis)
            assertTrue(state.bars.all { it.showsLabel && it.showsDot })
            assertFalse(state.bars.last().isToday)
        }

    @Test
    fun `GIVEN a bill today WHEN the statistics open THEN the last day is today`() = runTest(testDispatcher) {
        // Given
        prepareScenario(stored = tripBills + expense("taxi", 1_000, spentAt = today))

        // When
        val bars = viewModel.uiState.value.bars

        // Then
        assertEquals(6, bars.size)
        assertTrue(bars.last().isToday)
    }

    @Test
    fun `GIVEN by day WHEN picking Semana THEN goes by week and keeps it on the device`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnGrainPicked(StatsGrain.WEEK))

        // Then
        val state = viewModel.uiState.value
        assertEquals(StatsGrain.WEEK, state.grain)
        assertEquals(listOf(30_000L, 60_000L), state.bars.map { it.amountMinor })
        assertEquals(millisOf("2026-10-19T00:00:00Z"), state.bars.first().startMillis)
        assertEquals(millisOf("2026-11-01T00:00:00Z"), state.bars.last().endMillis)
        assertEquals(listOf(StatsChartPreferences(StatsChartType.BARS, StatsGrain.WEEK)), keptCharts)
    }

    @Test
    fun `GIVEN a trip of one month WHEN picking Mes THEN nothing changes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnGrainPicked(StatsGrain.MONTH))

        // Then
        assertEquals(StatsGrain.DAY, viewModel.uiState.value.grain)
        assertTrue(keptCharts.isEmpty())
    }

    @Test
    fun `GIVEN bars WHEN picking Linha THEN draws the line and keeps it on the device`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnChartTypePicked(StatsChartType.LINE))

        // Then
        assertEquals(StatsChartType.LINE, viewModel.uiState.value.chartType)
        assertEquals(listOf(StatsChartPreferences(StatsChartType.LINE, null)), keptCharts)
    }

    @Test
    fun `GIVEN Mes kept on the device WHEN a short trip opens THEN goes by day`() = runTest(testDispatcher) {
        // Given
        prepareScenario(keptChart = StatsChartPreferences(StatsChartType.LINE, StatsGrain.MONTH))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(StatsGrain.DAY, state.grain)
        assertEquals(StatsChartType.LINE, state.chartType)
    }

    @Test
    fun `GIVEN a home over two years WHEN the statistics open THEN count the whole period by month`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                group = home,
                stored = homeBills,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isTrip)
            assertEquals(StatsPeriod.ALL, state.period)
            assertEquals(StatsRangeStyle.MONTHS, state.periodRangeStyle)
            assertEquals(StatsGrain.MONTH, state.grain)
            assertEquals(listOf(false, true, true, true), state.grains.map { it.isEnabled })
            assertEquals(listOf(StatsGrainNote.DAY_NEEDS_SHORT_PERIOD), state.grainNotes)
            assertEquals(
                listOf(
                    StatsPeriod.THIS_YEAR,
                    StatsPeriod.THIS_MONTH,
                    StatsPeriod.LAST_MONTH,
                    StatsPeriod.ALL,
                    StatsPeriod.CUSTOM,
                ),
                state.periodOptions.map { it.period },
            )
        }

    @Test
    fun `GIVEN a home over two years in this year WHEN reading the notes THEN Ano switches the period`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                group = home,
                stored = homeBills,
            )

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.THIS_YEAR))

            // Then
            val state = viewModel.uiState.value
            assertEquals(StatsPeriod.THIS_YEAR, state.period)
            assertEquals(20_000L, state.totalMinor)
            assertEquals(
                listOf(StatsGrainNote.DAY_NEEDS_SHORT_PERIOD, StatsGrainNote.YEAR_SWITCHES_PERIOD),
                state.grainNotes,
            )
        }

    @Test
    fun `GIVEN this year of a home WHEN picking Ano THEN switches to the whole period by year`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                group = home,
                stored = homeBills,
            )
            viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.THIS_YEAR))

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnGrainPicked(StatsGrain.YEAR))

            // Then
            val state = viewModel.uiState.value
            assertEquals(StatsPeriod.ALL, state.period)
            assertEquals(StatsGrain.YEAR, state.grain)
            assertEquals(listOf(10_000L, 20_000L), state.bars.map { it.amountMinor })
        }

    @Test
    fun `GIVEN a trip WHEN picking last month without bills THEN says the period is empty`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.LAST_MONTH))

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isPeriodEmpty)
        assertFalse(state.showsStats)
        assertEquals(millisOf("2026-09-01T00:00:00Z"), state.periodStartMillis)
        assertEquals(millisOf("2026-09-30T00:00:00Z"), state.periodEndMillis)
        assertEquals(millisOf("2026-10-24T00:00:00Z"), state.allStartMillis)
    }

    @Test
    fun `GIVEN an empty last month WHEN tapping Ver toda a viagem THEN shows the whole trip`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.LAST_MONTH))

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnWholePeriodClicked)

            // Then
            assertEquals(StatsPeriod.ALL, viewModel.uiState.value.period)
            assertTrue(viewModel.uiState.value.showsStats)
        }

    @Test
    fun `GIVEN no dates picked yet WHEN picking Escolher datas THEN the period stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.CUSTOM))

        // Then
        assertEquals(StatsPeriod.ALL, viewModel.uiState.value.period)
    }

    @Test
    fun `GIVEN a trip WHEN picking dates backwards THEN counts the days between them`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(
            GroupStatsUiEvent.OnCustomPeriodPicked(
                startMillis = millisOf("2026-10-28T10:00:00Z"),
                endMillis = millisOf("2026-10-20T10:00:00Z"),
            ),
        )

        // Then
        val state = viewModel.uiState.value
        assertEquals(StatsPeriod.CUSTOM, state.period)
        assertEquals(millisOf("2026-10-20T00:00:00Z"), state.periodStartMillis)
        assertEquals(millisOf("2026-10-28T00:00:00Z"), state.periodEndMillis)
        assertEquals(90_000L, state.totalMinor)
        assertEquals(millisOf("2026-10-20T00:00:00Z"), state.periodOptions.last().startMillis)
    }

    @Test
    fun `GIVEN a trip WHEN picking two days of one week across two months THEN says why Semana and Ano are off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(
                GroupStatsUiEvent.OnCustomPeriodPicked(
                    startMillis = millisOf("2026-09-30T10:00:00Z"),
                    endMillis = millisOf("2026-10-01T10:00:00Z"),
                ),
            )

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf(true, false, true, false), state.grains.map { it.isEnabled })
            assertEquals(
                listOf(StatsGrainNote.WEEK_NEEDS_LONGER_PERIOD, StatsGrainNote.YEAR_NEEDS_LONGER_PERIOD),
                state.grainNotes,
            )
        }

    @Test
    fun `GIVEN a trip WHEN picking dates past today THEN the period ends today`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(
            GroupStatsUiEvent.OnCustomPeriodPicked(
                startMillis = millisOf("2026-10-20T10:00:00Z"),
                endMillis = millisOf("2026-11-15T10:00:00Z"),
            ),
        )

        // Then
        val state = viewModel.uiState.value
        assertEquals(millisOf("2026-10-20T00:00:00Z"), state.periodStartMillis)
        assertEquals(millisOf("2026-10-29T00:00:00Z"), state.periodEndMillis)
    }

    @Test
    fun `GIVEN a trip WHEN the statistics open THEN the biggest bill names who paid it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val biggest = viewModel.uiState.value.biggestExpense

        // Then
        assertEquals("boat", biggest?.expenseId)
        assertEquals(60_000L, biggest?.amountMinor)
        assertEquals(listOf(StatsPayer("Pierre", isMe = true)), biggest?.payers)
    }

    @Test
    fun `GIVEN the statistics WHEN tapping the biggest bill THEN opens it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnBiggestExpenseClicked)

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(GROUP_ID, joinedAs = null),
                route,
                Route.BillDetail(GROUP_ID, "boat"),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a group without bills WHEN the statistics open THEN shows nothing to show yet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stored = emptyList())

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isGroupEmpty)
            assertFalse(state.showsPeriodPicker)
            assertFalse(state.isPeriodEmpty)
        }

    @Test
    fun `GIVEN a group without bills WHEN tapping Rachar conta THEN opens a new bill`() = runTest(testDispatcher) {
        // Given
        prepareScenario(stored = emptyList())

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnSplitBillClicked)

        // Then
        assertEquals(
            Route.SplitBill(
                GROUP_ID,
                expenseId = null,
                picksGroupFirst = false,
            ),
            navigator.backStack.value.last(),
        )
    }

    @Test
    fun `GIVEN the statistics WHEN tapping back THEN returns to the group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the statistics WHEN the rail opens Grupos THEN goes back to the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnRailTabClicked(HomeTab.GROUPS))

        // Then
        assertEquals(listOf<Route>(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the statistics over the groups WHEN drawn THEN the rail selects Grupos`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(HomeTab.GROUPS, state.homeTab)
    }

    @Test
    fun `GIVEN the statistics WHEN the group leaves the device THEN they close`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        groups.groups.value = emptyList()

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a bill over the statistics WHEN the group leaves the device THEN they close once on top`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupStatsUiEvent.OnBiggestExpenseClicked)
            groups.groups.value = emptyList()

            // When
            navigator.navigateBack()

            // Then
            assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = null)), navigator.backStack.value)
        }

    private val route = Route.GroupStats(GROUP_ID)

    // Why: Thursday, 29 October 2026, in the afternoon.
    private val today = millisOf("2026-10-29T15:00:00Z")

    private val pierre = member(PIERRE, "Pierre", isMe = true)
    private val ana = member(ANA, "Ana")
    private val bruno = member(BRUNO, "Bruno")

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.PEACH,
        currency = GroupCurrency.BRL,
        members = listOf(pierre, ana, bruno),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )
    private val home = trip.copy(
        name = "Apê 302",
        type = GroupType.HOME,
    )

    // Why: Ana's R$ 300 dinner on the 24th, split equally; Pierre's R$ 600 boat on the 28th, Ana and Bruno more.
    private val tripBills = listOf(
        expense(
            id = "dinner",
            amountMinor = 30_000,
            spentAt = millisOf("2026-10-24T20:00:00Z"),
            payers = listOf(paid(ANA, 30_000)),
            shares = listOf(share(PIERRE, 10_000), share(ANA, 10_000), share(BRUNO, 10_000)),
        ),
        expense(
            id = "boat",
            amountMinor = 60_000,
            spentAt = millisOf("2026-10-28T15:00:00Z"),
            payers = listOf(paid(PIERRE, 60_000)),
            shares = listOf(share(PIERRE, 15_000), share(ANA, 25_000), share(BRUNO, 20_000)),
        ),
    )

    // Why: R$ 100 in March 2025 and R$ 200 in March 2026.
    private val homeBills = listOf(
        expense(
            id = "rent-2025",
            amountMinor = 10_000,
            spentAt = millisOf("2025-03-05T12:00:00Z"),
        ),
        expense(
            id = "rent-2026",
            amountMinor = 20_000,
            spentAt = millisOf("2026-03-05T12:00:00Z"),
        ),
    )

    private fun millisOf(instant: String): Long = Instant.parse(instant).toEpochMilliseconds()

    private fun member(
        id: String,
        name: String,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = true,
        userId = null,
        inviteCode = null,
    )

    private fun share(
        memberId: String,
        amountMinor: Long,
    ): ExpenseShare = ExpenseShare(
        memberId = memberId,
        amountMinor = amountMinor,
        weight = 1,
    )

    private fun paid(
        memberId: String,
        amountMinor: Long,
    ): ExpensePayer = ExpensePayer(
        memberId = memberId,
        amountMinor = amountMinor,
    )

    private fun expense(
        id: String,
        amountMinor: Long,
        spentAt: Long,
        payers: List<ExpensePayer> = listOf(paid(PIERRE, amountMinor)),
        shares: List<ExpenseShare> = listOf(share(PIERRE, amountMinor)),
        isPending: Boolean = false,
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = id,
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = payers,
        shares = shares,
        items = null,
        spentAt = spentAt,
        isPending = isPending,
        updatedAt = spentAt,
        updatedByMemberId = null,
    )

    private fun prepareScenario(
        group: Group = trip,
        stored: List<Expense> = tripBills,
        keptChart: StatsChartPreferences = StatsChartPreferences.initial,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(group))
        expenses = FakeExpenseRepository(initialExpenses = stored)
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.GroupDetail(GROUP_ID, joinedAs = null))
            it.navigate(route)
        }
        chart = MutableStateFlow(keptChart)
        keptCharts = mutableListOf()
        viewModel = GroupStatsViewModel(
            route = route,
            navigator = navigator,
            currentTimeMillis = { today },
            currentTimeZone = { TimeZone.UTC },
            updateStatsChartPreferences = { preferences ->
                keptCharts += preferences
                chart.value = preferences
            },
            observeGroup = ObserveGroupUseCase(groups),
            observeExpenses = ObserveExpensesUseCase(expenses),
            observeStatsChartPreferences = { chart },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val PIERRE = "member-1"
        const val ANA = "member-2"
        const val BRUNO = "member-3"
        const val FORMER = "member-9"
    }
}
