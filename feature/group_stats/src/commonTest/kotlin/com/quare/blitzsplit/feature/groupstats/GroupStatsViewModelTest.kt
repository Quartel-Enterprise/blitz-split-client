package com.quare.blitzsplit.feature.groupstats

import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityUnreadCount
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettlements
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettlementsUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.model.StatsChartPreferences
import com.quare.blitzsplit.core.model.StatsChartType
import com.quare.blitzsplit.core.model.StatsGrain
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsGrainNote
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPayer
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeopleView
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPeriod
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsRangeStyle
import com.quare.blitzsplit.feature.groupstats.presentation.viewmodel.GroupStatsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class GroupStatsViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val activityUnread = MutableStateFlow(0)
    private val firstDownload = MutableStateFlow<FirstDownloadProgress?>(null)
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var chart: MutableStateFlow<StatsChartPreferences>
    private lateinit var keptCharts: MutableList<StatsChartPreferences>
    private lateinit var categories: FakeCategoryRepository
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
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
                route,
                Route.BillDetail(GROUP_ID, "boat", focusCommentId = null),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a first download and no bill yet WHEN the statistics open THEN skeletons not nothing to show`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                stored = emptyList(),
                downloading = downloadingOnline,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoadingBills)
            assertFalse(state.isGroupEmpty)
            assertFalse(state.isPeriodEmpty)
        }

    @Test
    fun `GIVEN a first download WHEN picking last month without bills THEN skeletons instead of the empty period`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(downloading = downloadingOnline)

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.LAST_MONTH))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isLoadingBills)
            assertFalse(state.isPeriodEmpty)
        }

    @Test
    fun `GIVEN a first download waiting for the internet WHEN the statistics open THEN say the rest waits for it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                stored = emptyList(),
                downloading = downloadingOnline.copy(isWaitingForInternet = true),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isWaitingForInternet)
            assertTrue(state.isLoadingBills)
        }

    @Test
    fun `GIVEN whether the copy is whole not read yet WHEN the group has no bill THEN nothing is drawn`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                stored = emptyList(),
                isDownloadReadAtStart = false,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertFalse(state.isGroupEmpty)
        }

    @Test
    fun `GIVEN no bill while a first download runs WHEN it ends THEN says nothing to show yet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                stored = emptyList(),
                downloading = downloadingOnline,
            )

            // When
            firstDownload.value = null

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isGroupEmpty)
            assertFalse(state.isLoadingBills)
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
                scansReceipt = false,
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
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            ),
            navigator.backStack.value,
        )
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
    fun `GIVEN unread activity counted WHEN another period redraws the statistics THEN the rail keeps counting it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            activityUnread.value = 3

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.THIS_MONTH))

            // Then
            assertEquals(3, viewModel.uiState.value.activityUnreadCount)
        }

    @Test
    fun `GIVEN no first download WHEN the statistics open THEN say Total gasto`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isFirstDownload)
    }

    @Test
    fun `GIVEN a first download pending WHEN the statistics open THEN say Total gasto ate agora`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                downloading = FirstDownloadProgress(
                    arrivedGroupCount = 1,
                    groupCount = 7,
                    isWaitingForInternet = false,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isFirstDownload)
        }

    @Test
    fun `GIVEN a first download pending WHEN it ends THEN the statistics say Total gasto`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            downloading = FirstDownloadProgress(
                arrivedGroupCount = 1,
                groupCount = 7,
                isWaitingForInternet = false,
            ),
        )

        // When
        firstDownload.value = null

        // Then
        assertFalse(viewModel.uiState.value.isFirstDownload)
    }

    @Test
    fun `GIVEN a first download pending WHEN another bill comes down THEN the statistics still say ate agora`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                downloading = FirstDownloadProgress(
                    arrivedGroupCount = 1,
                    groupCount = null,
                    isWaitingForInternet = false,
                ),
            )

            // When
            expenses.expenses.value = tripBills + expense("taxi", 1_000, spentAt = today)

            // Then
            assertEquals(3, viewModel.uiState.value.expenseCount)
            assertTrue(viewModel.uiState.value.isFirstDownload)
        }

    @Test
    fun `GIVEN a first download pending WHEN picking another period THEN the statistics still say ate agora`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                downloading = FirstDownloadProgress(
                    arrivedGroupCount = 1,
                    groupCount = 7,
                    isWaitingForInternet = false,
                ),
            )

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.THIS_MONTH))

            // Then
            assertEquals(StatsPeriod.THIS_MONTH, viewModel.uiState.value.period)
            assertTrue(viewModel.uiState.value.isFirstDownload)
        }

    @Test
    fun `GIVEN the statistics WHEN the group leaves the device THEN they close`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        groups.groups.value = emptyList()

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            ),
            navigator.backStack.value,
        )
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
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the trip with no payments WHEN the statistics open THEN Acertos ranks who settled all first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val settling = assertNotNull(viewModel.uiState.value.settling)

            // Then
            assertEquals(PIERRE, settling.fastest.memberId)
            assertTrue(settling.fastest.isMe)
            assertEquals(4, settling.fastest.averageDays)
            assertEquals(0L, settling.fastest.openMinor)
            assertEquals(BRUNO, settling.slowest.memberId)
            assertEquals("Bruno", settling.slowest.name)
            assertFalse(settling.slowest.isMe)
            assertEquals(30_000L, settling.slowest.openMinor)
        }

    @Test
    fun `GIVEN the payments not read yet WHEN the bills are THEN nothing is drawn with Acertos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(arePaymentsReadAtStart = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertNull(state.settling)
        }

    @Test
    fun `GIVEN Acertos WHEN the payment of Ana arrives THEN she paid on the day with nothing open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            settlements.settlements.value = listOf(anaPaysTheBoat)

            // Then
            val fastest = viewModel.uiState.value.settling
                ?.fastest
            assertEquals(ANA, fastest?.memberId)
            assertEquals(0, fastest?.averageDays)
            assertEquals(0L, fastest?.openMinor)
        }

    @Test
    fun `GIVEN a payment still in the queue WHEN the statistics open THEN Acertos counts it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(payments = listOf(anaPaysTheBoat.copy(isPending = true)))

            // When
            val fastest = viewModel.uiState.value.settling
                ?.fastest

            // Then
            assertEquals(ANA, fastest?.memberId)
            assertEquals(0, fastest?.averageDays)
            assertEquals(0L, fastest?.openMinor)
        }

    @Test
    fun `GIVEN the whole trip WHEN picking only the boat day THEN Acertos counts only the debts of that day`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(
                GroupStatsUiEvent.OnCustomPeriodPicked(
                    startMillis = millisOf("2026-10-28T10:00:00Z"),
                    endMillis = millisOf("2026-10-28T10:00:00Z"),
                ),
            )

            // Then
            val settling = assertNotNull(viewModel.uiState.value.settling)
            assertEquals(ANA, settling.fastest.memberId)
            assertEquals(BRUNO, settling.slowest.memberId)
            assertEquals(1, settling.slowest.averageDays)
            assertEquals(20_000L, settling.slowest.openMinor)
        }

    @Test
    fun `GIVEN the trip WHEN picking a month with no bills THEN Acertos is hidden`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.LAST_MONTH))

        // Then
        assertNull(viewModel.uiState.value.settling)
    }

    // Why: Ana pays Pierre the R$ 50 the boat left on her, the evening of the boat.
    private val anaPaysTheBoat = Settlement(
        id = "ana-boat",
        groupId = GROUP_ID,
        fromMemberId = ANA,
        toMemberId = PIERRE,
        amountMinor = 5_000,
        method = SettlementMethod.PIX,
        paidAt = millisOf("2026-10-28T18:00:00Z"),
        isPending = false,
        updatedAt = millisOf("2026-10-28T18:00:00Z"),
    )

    @Test
    fun `GIVEN Por pessoa with someone who left WHEN the statistics open THEN only the members open a profile`() =
        runTest(testDispatcher) {
            // Given
            val withFormer = expense(
                id = "dinner",
                amountMinor = 30_000,
                spentAt = today,
                payers = listOf(paid(FORMER, 30_000)),
                shares = listOf(share(FORMER, 10_000), share(PIERRE, 10_000), share(ANA, 10_000)),
            )
            prepareScenario(stored = listOf(withFormer))

            // When
            val people = viewModel.uiState.value.people

            // Then
            assertEquals(
                mapOf(PIERRE to true, ANA to true, BRUNO to true, FORMER to false),
                people.associate { it.memberId to it.opensProfile },
            )
        }

    @Test
    fun `GIVEN Pagou with someone who left WHEN picking it THEN the former member still opens no profile`() =
        runTest(testDispatcher) {
            // Given
            val withFormer = expense(
                id = "dinner",
                amountMinor = 20_000,
                spentAt = today,
                payers = listOf(paid(FORMER, 20_000)),
                shares = listOf(share(PIERRE, 20_000)),
            )
            prepareScenario(stored = listOf(withFormer))

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPeopleViewPicked(StatsPeopleView.PAID))

            // Then
            val people = viewModel.uiState.value.people
            assertFalse(people.single { it.memberId == FORMER }.opensProfile)
            assertTrue(people.single { it.memberId == PIERRE }.opensProfile)
        }

    @Test
    fun `GIVEN a guest in Por pessoa WHEN tapping their row THEN opens their contact profile by name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(group = trip.copy(members = listOf(pierre, ana, member(BRUNO, "Bruno  Lima"))))

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPersonClicked(BRUNO))

            // Then
            assertEquals(statsStack + Route.ContactProfile("name:bruno lima"), navigator.backStack.value)
        }

    @Test
    fun `GIVEN someone who joined in Por pessoa WHEN tapping their row THEN opens their contact profile by account`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(group = trip.copy(members = listOf(pierre, ana.copy(userId = "user-ana"), bruno)))

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPersonClicked(ANA))

            // Then
            assertEquals(statsStack + Route.ContactProfile("user:user-ana"), navigator.backStack.value)
        }

    @Test
    fun `GIVEN this device user in Por pessoa WHEN tapping their row THEN opens Perfil`() = runTest(testDispatcher) {
        // Given
        prepareScenario(group = trip.copy(members = listOf(pierre.copy(userId = "user-pierre"), ana, bruno)))

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnPersonClicked(PIERRE))

        // Then
        assertEquals(statsStack + Route.Profile, navigator.backStack.value)
    }

    @Test
    fun `GIVEN someone who left in Por pessoa WHEN tapping their row THEN nothing opens`() = runTest(testDispatcher) {
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
        viewModel.onEvent(GroupStatsUiEvent.OnPersonClicked(FORMER))

        // Then
        assertEquals(statsStack, navigator.backStack.value)
    }

    @Test
    fun `GIVEN the statistics WHEN tapping a person nobody in the group is THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPersonClicked("member-unknown"))

            // Then
            assertEquals(statsStack, navigator.backStack.value)
        }

    @Test
    fun `GIVEN Maior despesa beside the statistics WHEN tapping a person THEN their profile takes the bill place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupStatsUiEvent.OnBiggestExpenseClicked)

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPersonClicked(ANA))

            // Then
            assertEquals(statsStack + Route.ContactProfile("name:ana"), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Maior despesa beside the statistics WHEN tapping your own row THEN Perfil opens over the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupStatsUiEvent.OnBiggestExpenseClicked)

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPersonClicked(PIERRE))

            // Then
            assertEquals(
                statsStack + Route.BillDetail(GROUP_ID, "boat", focusCommentId = null) + Route.Profile,
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a profile over the statistics WHEN a late person tap arrives THEN nothing more opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPersonClicked(ANA))

            // Then
            assertEquals(statsStack + Route.Profile, navigator.backStack.value)
        }

    @Test
    fun `GIVEN the whole trip WHEN picking Este mes THEN sends the period picked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.THIS_MONTH))

        // Then
        assertEquals(
            listOf("stats_period_selected" to mapOf<String, Any>("period" to StatsPeriod.THIS_MONTH)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN this month WHEN picking the whole trip from the menu THEN sends the whole period picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.THIS_MONTH))
            trackedEvents.clear()

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.ALL))

            // Then
            assertEquals(
                listOf("stats_period_selected" to mapOf<String, Any>("period" to StatsPeriod.ALL)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a home WHEN picking Este ano THEN sends the year picked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            group = home,
            stored = homeBills,
        )

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.THIS_YEAR))

        // Then
        assertEquals(
            listOf("stats_period_selected" to mapOf<String, Any>("period" to StatsPeriod.THIS_YEAR)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a trip WHEN picking last month without bills THEN still sends the period picked once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.LAST_MONTH))

            // Then
            assertEquals(
                listOf("stats_period_selected" to mapOf<String, Any>("period" to StatsPeriod.LAST_MONTH)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a trip WHEN picking the days of Escolher datas THEN sends the custom period with no dates`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(
                GroupStatsUiEvent.OnCustomPeriodPicked(
                    startMillis = millisOf("2026-10-20T10:00:00Z"),
                    endMillis = millisOf("2026-10-28T10:00:00Z"),
                ),
            )

            // Then
            assertEquals(listOf("stats_custom_period_selected" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN a trip WHEN picking days past today THEN still sends only the custom period with no dates`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(
                GroupStatsUiEvent.OnCustomPeriodPicked(
                    startMillis = millisOf("2026-11-15T10:00:00Z"),
                    endMillis = millisOf("2026-10-20T10:00:00Z"),
                ),
            )

            // Then
            assertEquals(listOf("stats_custom_period_selected" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN Consumo WHEN picking Pagou THEN sends the people view picked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnPeopleViewPicked(StatsPeopleView.PAID))

        // Then
        assertEquals(
            listOf("stats_people_view_selected" to mapOf<String, Any>("view" to StatsPeopleView.PAID)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN Pagou WHEN picking Consumo THEN sends Consumo as the people view picked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupStatsUiEvent.OnPeopleViewPicked(StatsPeopleView.PAID))
        trackedEvents.clear()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnPeopleViewPicked(StatsPeopleView.CONSUMPTION))

        // Then
        assertEquals(
            listOf("stats_people_view_selected" to mapOf<String, Any>("view" to StatsPeopleView.CONSUMPTION)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN bars WHEN picking Linha THEN sends the chart type picked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnChartTypePicked(StatsChartType.LINE))

        // Then
        assertEquals(
            listOf("stats_chart_type_selected" to mapOf<String, Any>("chart_type" to StatsChartType.LINE)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a line WHEN picking Barras THEN sends Barras as the chart type picked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(keptChart = StatsChartPreferences(StatsChartType.LINE, null))

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnChartTypePicked(StatsChartType.BARS))

        // Then
        assertEquals(
            listOf("stats_chart_type_selected" to mapOf<String, Any>("chart_type" to StatsChartType.BARS)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN Consumo and bars WHEN picking Consumo or Barras again THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnPeopleViewPicked(StatsPeopleView.CONSUMPTION))
        viewModel.onEvent(GroupStatsUiEvent.OnChartTypePicked(StatsChartType.BARS))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN by day WHEN picking Semana THEN sends the grain picked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnGrainPicked(StatsGrain.WEEK))

        // Then
        assertEquals(
            listOf("stats_grain_selected" to mapOf<String, Any>("grain" to StatsGrain.WEEK)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN Semana picked WHEN picking Semana again THEN sends nothing more`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupStatsUiEvent.OnGrainPicked(StatsGrain.WEEK))
        trackedEvents.clear()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnGrainPicked(StatsGrain.WEEK))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a trip of one month WHEN picking Mes the period cannot show THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnGrainPicked(StatsGrain.MONTH))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN this year of a home WHEN picking Ano THEN sends only the grain though the period switches`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                group = home,
                stored = homeBills,
            )
            viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.THIS_YEAR))
            trackedEvents.clear()

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnGrainPicked(StatsGrain.YEAR))

            // Then
            assertEquals(
                listOf("stats_grain_selected" to mapOf<String, Any>("grain" to StatsGrain.YEAR)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Por pessoa WHEN tapping Ana THEN sends the person clicked from the statistics without her id`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPersonClicked(ANA))

            // Then
            assertEquals(listOf("person_clicked" to mapOf<String, Any>("source" to "group_stats")), trackedEvents)
        }

    @Test
    fun `GIVEN someone who left in Por pessoa WHEN tapping their row THEN still sends the person clicked once`() =
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
            viewModel.onEvent(GroupStatsUiEvent.OnPersonClicked(FORMER))

            // Then
            assertEquals(listOf("person_clicked" to mapOf<String, Any>("source" to "group_stats")), trackedEvents)
        }

    @Test
    fun `GIVEN the statistics WHEN tapping Maior despesa THEN sends the bill clicked from the statistics`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnBiggestExpenseClicked)

            // Then
            assertEquals(listOf("bill_clicked" to mapOf<String, Any>("source" to "group_stats")), trackedEvents)
        }

    @Test
    fun `GIVEN a group without bills WHEN tapping Rachar conta THEN sends the split bill clicked from there`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stored = emptyList())

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(
                listOf("split_bill_clicked" to mapOf<String, Any>("source" to "group_stats")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN an empty last month WHEN tapping Ver toda a viagem THEN sends the whole period clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.LAST_MONTH))
            trackedEvents.clear()

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnWholePeriodClicked)

            // Then
            assertEquals(listOf("stats_whole_period_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the statistics WHEN tapping back THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnBackClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the statistics WHEN the rail opens Grupos THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(GroupStatsUiEvent.OnRailTabClicked(HomeTab.GROUPS))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the statistics WHEN they open and a sync brings a bill THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        expenses.expenses.value = tripBills + expense(
            id = "taxi",
            amountMinor = 5_000,
            spentAt = today,
            payers = listOf(paid(ANA, 5_000)),
            shares = listOf(share(ANA, 2_500), share(PIERRE, 2_500)),
        )

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a trip with bills on some days WHEN the statistics open THEN each day counts its bills`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stored = busyTripBills)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(1, 0, 1, 0, 2), state.bars.map { it.expenseCount })
            assertEquals(state.expenseCount, state.bars.sumOf { it.expenseCount })
        }

    @Test
    fun `GIVEN a trip with bills on some days WHEN the statistics open THEN the lines step up to cover the top day`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stored = busyTripBills)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(0L, 25_000L, 50_000L, 75_000L), state.chartTicksMinor)
            assertEquals(75_000L, state.chartTopMinor)
        }

    @Test
    fun `GIVEN a trip with bills on some days WHEN the statistics open THEN the average counts the empty days too`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stored = busyTripBills)

            // When
            val average = viewModel.uiState.value.chartAverageMinor

            // Then
            assertEquals(21_000L, average)
        }

    @Test
    fun `GIVEN a trip WHEN picking a single day THEN its one bar has the lines and no average`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stored = busyTripBills)

            // When
            viewModel.onEvent(
                GroupStatsUiEvent.OnCustomPeriodPicked(
                    startMillis = millisOf("2026-10-28T10:00:00Z"),
                    endMillis = millisOf("2026-10-28T20:00:00Z"),
                ),
            )

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf(70_000L), state.bars.map { it.amountMinor })
            assertEquals(listOf(2), state.bars.map { it.expenseCount })
            assertEquals(listOf(0L, 25_000L, 50_000L, 75_000L), state.chartTicksMinor)
            assertNull(state.chartAverageMinor)
        }

    @Test
    fun `GIVEN every bill on one day WHEN picking this month THEN the other days count none and lower the average`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                stored = listOf(
                    expense("boat", 60_000, millisOf("2026-10-28T15:00:00Z")),
                    expense("lunch", 10_000, millisOf("2026-10-28T12:00:00Z")),
                ),
            )

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.THIS_MONTH))

            // Then
            val state = viewModel.uiState.value
            assertEquals(List(29) { day -> if (day == 27) 2 else 0 }, state.bars.map { it.expenseCount })
            assertEquals(listOf(0L, 25_000L, 50_000L, 75_000L), state.chartTicksMinor)
            assertEquals(2_414L, state.chartAverageMinor)
        }

    @Test
    fun `GIVEN a bill of millions WHEN the statistics open THEN the lines climb by R 500 thousand`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                stored = listOf(
                    expense("car", 123_456_789, millisOf("2026-10-24T12:00:00Z")),
                    expense("fuel", 1_000_000, millisOf("2026-10-28T12:00:00Z")),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(1, 0, 0, 0, 1), state.bars.map { it.expenseCount })
            assertEquals(listOf(0L, 50_000_000L, 100_000_000L, 150_000_000L), state.chartTicksMinor)
            assertEquals(24_891_358L, state.chartAverageMinor)
        }

    @Test
    fun `GIVEN by day WHEN picking Semana THEN each week sums the bills of its days with its lines and average`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stored = busyTripBills)

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnGrainPicked(StatsGrain.WEEK))

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf(30_000L, 75_000L), state.bars.map { it.amountMinor })
            assertEquals(listOf(1, 3), state.bars.map { it.expenseCount })
            assertEquals(listOf(0L, 25_000L, 50_000L, 75_000L), state.chartTicksMinor)
            assertEquals(52_500L, state.chartAverageMinor)
        }

    @Test
    fun `GIVEN a home over two years WHEN the statistics open by month THEN each month counts its bills`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                group = home,
                stored = homeBills + expense("market", 5_000, millisOf("2026-03-20T12:00:00Z")),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(StatsGrain.MONTH, state.grain)
            assertEquals(listOf(1) + List(11) { 0 } + listOf(2), state.bars.map { it.expenseCount })
            assertEquals(listOf(0L, 10_000L, 20_000L, 30_000L), state.chartTicksMinor)
            assertEquals(2_692L, state.chartAverageMinor)
        }

    @Test
    fun `GIVEN a trip WHEN picking last month without bills THEN the chart has no lines and no average`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupStatsUiEvent.OnPeriodPicked(StatsPeriod.LAST_MONTH))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.bars.all { it.expenseCount == 0 })
            assertTrue(state.chartTicksMinor.isEmpty())
            assertNull(state.chartAverageMinor)
            assertEquals(1L, state.chartTopMinor)
        }

    @Test
    fun `GIVEN the statistics WHEN a sync brings a bill THEN the counts lines and average follow it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            expenses.expenses.value = tripBills + expense("lunch", 10_000, millisOf("2026-10-28T12:00:00Z"))

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf(1, 0, 0, 0, 2), state.bars.map { it.expenseCount })
            assertEquals(listOf(0L, 25_000L, 50_000L, 75_000L), state.chartTicksMinor)
            assertEquals(20_000L, state.chartAverageMinor)
        }

    // Why: the trip's dinner and boat, a R$ 100 lunch on the boat day and a R$ 50 taxi still in the queue on the 26th.
    private val busyTripBills: List<Expense>
        get() = tripBills + listOf(
            expense("lunch", 10_000, millisOf("2026-10-28T12:00:00Z")),
            expense("taxi", 5_000, millisOf("2026-10-26T10:00:00Z"), isPending = true),
        )

    private val route = Route.GroupStats(GROUP_ID)

    // Why: the back stack the statistics open on, over the group's Despesas over Grupos.
    private val statsStack: List<Route>
        get() = listOf(
            Route.Groups,
            Route.GroupDetail(
                GROUP_ID,
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
            route,
        )

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
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(pierre, ana, bruno),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
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
        photoUrl = null,
    )

    private fun share(
        memberId: String,
        amountMinor: Long,
    ): ExpenseShare = ExpenseShare(
        memberId = memberId,
        amountMinor = amountMinor,
        weight = 1,
    )

    @Test
    fun `GIVEN bills in categories WHEN the statistics open THEN Por categoria sums mains and subs`() =
        runTest(testDispatcher) {
            // Given
            val at = millisOf("2026-10-28T12:00:00Z")
            prepareScenario(
                stored = listOf(
                    expense("rent", 7_000, at, categoryId = AppCategory.HOME_RENT.key),
                    expense("condo", 1_000, at, categoryId = AppCategory.HOME_CONDO.key),
                    expense("market", 1_500, at, categoryId = AppCategory.FOOD_MARKET.key),
                    expense("bbq", 300, at, categoryId = "bbq"),
                    expense("raffle", 200, at),
                ),
                groupCategories = listOf(
                    GroupCategory(
                        "bbq",
                        GROUP_ID,
                        AppCategory.FOOD.key,
                        "Churrasco",
                        null,
                        ANA,
                        isDeleted = true,
                        isPending = false,
                    ),
                ),
            )

            // When
            val rows = viewModel.uiState.value.categories

            // Then
            assertEquals(
                listOf(AppCategory.HOME.key to 8_000L, AppCategory.FOOD.key to 1_800L, AppCategory.OTHER.key to 200L),
                rows.map { it.category.id to it.amountMinor },
            )
            assertEquals(listOf(80, 18, 2), rows.map { it.percent })
            assertEquals(
                listOf(AppCategory.HOME_RENT.key, AppCategory.HOME_CONDO.key),
                rows.first().subcategories.map { it.category.id },
            )
            assertEquals(listOf(AppCategory.FOOD_MARKET.key), rows[1].subcategories.map { it.category.id })
            assertEquals(
                AppCategory.HOME.key,
                viewModel.uiState.value.topCategory
                    ?.category
                    ?.id,
            )
            assertEquals(1.0, rows.first().barFraction)
        }

    @Test
    fun `GIVEN three equal categories WHEN rounding THEN the percentages still add up to 100`() =
        runTest(testDispatcher) {
            // Given
            val at = millisOf("2026-10-28T12:00:00Z")
            prepareScenario(
                stored = listOf(
                    expense("a", 100, at, categoryId = AppCategory.HOME.key),
                    expense("b", 100, at, categoryId = AppCategory.FOOD.key),
                    expense("c", 100, at, categoryId = AppCategory.LEISURE.key),
                ),
            )

            // When
            val percents = viewModel.uiState.value.categories
                .map { it.percent }

            // Then
            assertEquals(100, percents.sum())
        }

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
        categoryId: String? = null,
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = id,
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = payers,
        shares = shares,
        items = null,
        categoryId = categoryId,
        spentAt = spentAt,
        isPending = isPending,
        updatedAt = spentAt,
        updatedByMemberId = null,
    )

    private val downloadingOnline = FirstDownloadProgress(
        arrivedGroupCount = 1,
        groupCount = 7,
        isWaitingForInternet = false,
    )

    private fun prepareScenario(
        group: Group = trip,
        stored: List<Expense> = tripBills,
        keptChart: StatsChartPreferences = StatsChartPreferences.initial,
        groupCategories: List<GroupCategory> = emptyList(),
        downloading: FirstDownloadProgress? = null,
        isDownloadReadAtStart: Boolean = true,
        payments: List<Settlement> = emptyList(),
        arePaymentsReadAtStart: Boolean = true,
    ) {
        firstDownload.value = downloading
        categories = FakeCategoryRepository(initialGroupCategories = groupCategories)
        groups = FakeGroupRepository(initialGroups = listOf(group))
        expenses = FakeExpenseRepository(initialExpenses = stored)
        settlements = FakeSettlementRepository(initialSettlements = payments)
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            )
            it.navigate(route)
        }
        chart = MutableStateFlow(keptChart)
        keptCharts = mutableListOf()
        viewModel = GroupStatsViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
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
            observeSettlements = if (arePaymentsReadAtStart) {
                ObserveSettlementsUseCase(settlements)
            } else {
                ObserveSettlements { emptyFlow() }
            },
            observeStatsChartPreferences = { chart },
            observeCategoryTree = ObserveCategoryTreeUseCase(categories),
            observeActivityUnreadCount = ObserveActivityUnreadCount { activityUnread },
            observeFirstDownload = ObserveFirstDownload { if (isDownloadReadAtStart) firstDownload else emptyFlow() },
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
