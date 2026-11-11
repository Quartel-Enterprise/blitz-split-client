package com.quare.blitzsplit.feature.groupstats

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
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
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpenseRow
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpensesUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.CategoryExpensesUiState
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsPayer
import com.quare.blitzsplit.feature.groupstats.presentation.model.StatsRangeStyle
import com.quare.blitzsplit.feature.groupstats.presentation.viewmodel.CategoryExpensesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class CategoryExpensesViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var categories: FakeCategoryRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var listRoute: Route.CategoryExpenses
    private lateinit var viewModel: CategoryExpensesViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Food in this month WHEN the list opens THEN shows only the Food bills by day the most recent first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(listOf("lunch", "market"), listOf("dinner")), idsByDay(state))
            assertEquals(
                listOf(millisOf("2026-10-28T00:00:00Z"), millisOf("2026-10-24T00:00:00Z")),
                state.days.map { it.dayStartMillis },
            )
        }

    @Test
    fun `GIVEN Food tapped in this month WHEN the list opens THEN the total and count are of the Food bills`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(45_000L, state.totalMinor)
            assertEquals(3, state.expenseCount)
            assertFalse(state.isEmpty)
        }

    @Test
    fun `GIVEN bills of Food subcategories WHEN the list opens THEN each row keeps its own subcategory`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val categoryByBill = rowsOf(viewModel.uiState.value).associate { it.id to it.category.id }

            // Then
            assertEquals(
                mapOf(
                    "lunch" to AppCategory.FOOD.key,
                    "market" to AppCategory.FOOD_MARKET.key,
                    "dinner" to AppCategory.FOOD_RESTAURANT.key,
                ),
                categoryByBill,
            )
        }

    @Test
    fun `GIVEN Food tapped WHEN the list opens THEN the chips are the period main categories the largest first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val chips = viewModel.uiState.value.chips

            // Then
            assertEquals(
                listOf(
                    AppCategory.TRAVEL.key to false,
                    AppCategory.FOOD.key to true,
                    AppCategory.TRANSPORT.key to false,
                ),
                chips.map { it.category.id to it.isSelected },
            )
            assertTrue(viewModel.uiState.value.showsClear)
        }

    @Test
    fun `GIVEN Travel tapped in this month WHEN the list opens THEN the hotel of September stays out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(route = thisMonth(AppCategory.TRAVEL.key))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("boat"), rowsOf(state).map { it.id })
            assertEquals(60_000L, state.totalMinor)
        }

    @Test
    fun `GIVEN this month WHEN the list opens THEN the period is its first and last days written as a month`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(millisOf("2026-10-01T00:00:00Z"), state.periodStartMillis)
            assertEquals(millisOf("2026-10-29T00:00:00Z"), state.periodEndMillis)
            assertEquals(StatsRangeStyle.MONTH, state.periodRangeStyle)
        }

    @Test
    fun `GIVEN Travel tapped over the whole trip WHEN the list opens THEN shows the bills of every month`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(route = wholeHistory(AppCategory.TRAVEL.key))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("boat", "hotel"), rowsOf(state).map { it.id })
            assertEquals(140_000L, state.totalMinor)
        }

    @Test
    fun `GIVEN dates picked over a whole month WHEN the list opens THEN names its days as the statistics did`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = Route.CategoryExpenses(
                    groupId = GROUP_ID,
                    categoryId = AppCategory.FOOD.key,
                    currency = GroupCurrency.BRL,
                    periodStartMillis = millisOf("2026-09-01T00:00:00Z"),
                    periodEndMillis = millisOf("2026-09-30T00:00:00Z"),
                    isMonthPeriod = false,
                ),
            )

            // When
            val style = viewModel.uiState.value.periodRangeStyle

            // Then
            assertEquals(StatsRangeStyle.DAYS, style)
        }

    @Test
    fun `GIVEN dates picked over more than 3 months WHEN the list opens THEN names its months`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = Route.CategoryExpenses(
                    groupId = GROUP_ID,
                    categoryId = AppCategory.FOOD.key,
                    currency = GroupCurrency.BRL,
                    periodStartMillis = millisOf("2026-01-01T00:00:00Z"),
                    periodEndMillis = millisOf("2026-10-29T00:00:00Z"),
                    isMonthPeriod = false,
                ),
            )

            // When
            val style = viewModel.uiState.value.periodRangeStyle

            // Then
            assertEquals(StatsRangeStyle.MONTHS, style)
        }

    @Test
    fun `GIVEN the whole trip WHEN the list opens THEN the period has no dates and goes by days`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(route = wholeHistory(AppCategory.FOOD.key))

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.periodStartMillis)
            assertNull(state.periodEndMillis)
            assertEquals(StatsRangeStyle.DAYS, state.periodRangeStyle)
        }

    @Test
    fun `GIVEN the whole trip WHEN a new Food bill arrives THEN the list follows it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(route = wholeHistory(AppCategory.FOOD.key))
        val breakfast = expense("breakfast", 2_000, millisOf("2026-10-29T10:00:00Z"), AppCategory.FOOD_BAKERY.key)

        // When
        expenses.expenses.value = expenses.expenses.value + breakfast

        // Then
        val state = viewModel.uiState.value
        assertEquals(listOf(listOf("breakfast"), listOf("lunch", "market"), listOf("dinner")), idsByDay(state))
        assertEquals(47_000L, state.totalMinor)
        assertEquals(4, state.expenseCount)
    }

    @Test
    fun `GIVEN a bill of a deleted group subcategory of Food WHEN Food opens THEN it counts as Food`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                stored = octoberBills + expense("bbq", 3_000, millisOf("2026-10-27T20:00:00Z"), "bbq"),
                groupCategories = listOf(
                    GroupCategory(
                        "bbq",
                        GROUP_ID,
                        AppCategory.FOOD.key,
                        "Churrasco",
                        null,
                        null,
                        ANA,
                        isDeleted = true,
                        isPending = false,
                    ),
                ),
            )

            // When
            val rows = rowsOf(viewModel.uiState.value)

            // Then
            assertEquals(listOf("lunch", "market", "bbq", "dinner"), rows.map { it.id })
            assertEquals(AppCategory.FOOD.key, rows.first { it.id == "bbq" }.category.id)
        }

    @Test
    fun `GIVEN a bill without a category WHEN Other opens THEN shows that bill`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            route = thisMonth(AppCategory.OTHER.key),
            stored = octoberBills + expense("raffle", 1_000, millisOf("2026-10-26T12:00:00Z"), categoryId = null),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(listOf("raffle"), rowsOf(state).map { it.id })
        assertEquals(AppCategory.OTHER.key, rowsOf(state).single().category.id)
    }

    @Test
    fun `GIVEN Health with no bill in the period WHEN the list opens THEN its chip comes last picked and empty`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(route = thisMonth(AppCategory.HEALTH.key))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                listOf(
                    AppCategory.TRAVEL.key to false,
                    AppCategory.FOOD.key to false,
                    AppCategory.TRANSPORT.key to false,
                    AppCategory.HEALTH.key to true,
                ),
                state.chips.map { it.category.id to it.isSelected },
            )
            assertTrue(state.isEmpty)
            assertTrue(state.showsClear)
            assertEquals(0L, state.totalMinor)
            assertEquals(0, state.expenseCount)
        }

    @Test
    fun `GIVEN Food picked WHEN picking Travel THEN shows the bills of both`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryExpensesUiEvent.OnCategoryToggled(AppCategory.TRAVEL.key))

        // Then
        val state = viewModel.uiState.value
        assertEquals(listOf(listOf("boat", "lunch", "market"), listOf("dinner")), idsByDay(state))
        assertEquals(
            listOf(
                AppCategory.TRAVEL.key to true,
                AppCategory.FOOD.key to true,
                AppCategory.TRANSPORT.key to false,
            ),
            state.chips.map { it.category.id to it.isSelected },
        )
        assertEquals(105_000L, state.totalMinor)
        assertEquals(4, state.expenseCount)
    }

    @Test
    fun `GIVEN Food and Travel picked WHEN letting go of Food THEN shows only Travel`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryExpensesUiEvent.OnCategoryToggled(AppCategory.TRAVEL.key))

        // When
        viewModel.onEvent(CategoryExpensesUiEvent.OnCategoryToggled(AppCategory.FOOD.key))

        // Then
        val state = viewModel.uiState.value
        assertEquals(listOf("boat"), rowsOf(state).map { it.id })
        assertEquals(listOf(AppCategory.TRAVEL.key), state.chips.filter { it.isSelected }.map { it.category.id })
    }

    @Test
    fun `GIVEN only Food picked WHEN letting go of it THEN shows every bill of the period with no chip picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(CategoryExpensesUiEvent.OnCategoryToggled(AppCategory.FOOD.key))

            // Then
            val state = viewModel.uiState.value
            assertEquals(
                listOf(listOf("taxi"), listOf("boat", "lunch", "market"), listOf("dinner")),
                idsByDay(state),
            )
            assertTrue(state.chips.none { it.isSelected })
            assertFalse(state.showsClear)
            assertEquals(109_000L, state.totalMinor)
            assertEquals(5, state.expenseCount)
        }

    @Test
    fun `GIVEN Food picked WHEN picking Travel THEN sends the chip toggled as picked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryExpensesUiEvent.OnCategoryToggled(AppCategory.TRAVEL.key))

        // Then
        assertEquals(
            listOf("category_filter_toggled" to mapOf<String, Any>("is_selected" to true)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN Food picked WHEN letting go of Food THEN sends the chip toggled as let go`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryExpensesUiEvent.OnCategoryToggled(AppCategory.FOOD.key))

        // Then
        assertEquals(
            listOf("category_filter_toggled" to mapOf<String, Any>("is_selected" to false)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN Food and Travel picked WHEN tapping Clear THEN no chip is picked and every bill of the period shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryExpensesUiEvent.OnCategoryToggled(AppCategory.TRAVEL.key))

            // When
            viewModel.onEvent(CategoryExpensesUiEvent.OnClearClicked)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.chips.none { it.isSelected })
            assertFalse(state.showsClear)
            assertEquals(listOf("taxi", "boat", "lunch", "market", "dinner"), rowsOf(state).map { it.id })
        }

    @Test
    fun `GIVEN Food and Travel picked WHEN tapping Clear THEN sends the clear with the two chips picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryExpensesUiEvent.OnCategoryToggled(AppCategory.TRAVEL.key))
            trackedEvents.clear()

            // When
            viewModel.onEvent(CategoryExpensesUiEvent.OnClearClicked)

            // Then
            assertEquals(listOf("category_filter_cleared" to mapOf<String, Any>("count" to 2)), trackedEvents)
        }

    @Test
    fun `GIVEN no chip picked WHEN tapping Clear THEN nothing changes and nothing is sent`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryExpensesUiEvent.OnCategoryToggled(AppCategory.FOOD.key))
        val before = viewModel.uiState.value
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryExpensesUiEvent.OnClearClicked)

        // Then
        assertEquals(before, viewModel.uiState.value)
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a bill paid by Ana a former member and me WHEN the list opens THEN I come first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                stored = listOf(
                    expense(
                        id = "gifts",
                        amountMinor = 3_000,
                        spentAt = millisOf("2026-10-27T12:00:00Z"),
                        categoryId = AppCategory.FOOD.key,
                        payers = listOf(paid(ANA, 1_000), paid(FORMER, 1_000), paid(PIERRE, 1_000)),
                        shares = listOf(share(ANA, 3_000)),
                    ),
                ),
            )

            // When
            val payers = rowsOf(viewModel.uiState.value).single().payers

            // Then
            assertEquals(
                listOf(
                    StatsPayer("Pierre", isMe = true),
                    StatsPayer("Ana", isMe = false),
                    StatsPayer(null, isMe = false),
                ),
                payers,
            )
        }

    @Test
    fun `GIVEN Food bills WHEN the list opens THEN each row has what it does to this device user balance`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val balanceByBill = rowsOf(viewModel.uiState.value).associate { it.id to it.myBalanceMinor }

            // Then
            assertEquals(
                mapOf("lunch" to null, "market" to 2_500L, "dinner" to -10_000L),
                balanceByBill,
            )
        }

    @Test
    fun `GIVEN a taxi in the queue today WHEN Transport opens THEN its row is pending under Today`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(route = thisMonth(AppCategory.TRANSPORT.key))

            // When
            val day = viewModel.uiState.value.days
                .single()

            // Then
            assertEquals(RelativeDay.TODAY, day.relativeDay)
            assertTrue(day.expenses.single().isPending)
        }

    @Test
    fun `GIVEN Food bills already sent WHEN the list opens THEN none is pending and days read Yesterday or a date`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val days = viewModel.uiState.value.days

            // Then
            assertEquals(listOf(RelativeDay.YESTERDAY, RelativeDay.OTHER), days.map { it.relativeDay })
            assertTrue(days.flatMap { it.expenses }.none { it.isPending })
        }

    @Test
    fun `GIVEN a device in Sao Paulo WHEN a dinner is late there THEN it joins that local day`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = wholeHistory(AppCategory.FOOD.key),
                stored = listOf(
                    expense("late-dinner", 5_000, millisOf("2026-10-29T01:00:00Z"), AppCategory.FOOD.key),
                    expense("breakfast", 1_000, millisOf("2026-10-28T11:00:00Z"), AppCategory.FOOD.key),
                ),
                timeZone = saoPaulo,
            )

            // When
            val days = viewModel.uiState.value.days

            // Then
            assertEquals(listOf(listOf("late-dinner", "breakfast")), days.map { day -> day.expenses.map { it.id } })
            assertEquals(millisOf("2026-10-28T03:00:00Z"), days.single().dayStartMillis)
            assertEquals(RelativeDay.YESTERDAY, days.single().relativeDay)
        }

    @Test
    fun `GIVEN this month in Sao Paulo WHEN a bill was on the last evening of September there THEN it stays out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = Route.CategoryExpenses(
                    GROUP_ID,
                    AppCategory.FOOD.key,
                    currency = GroupCurrency.BRL,
                    periodStartMillis = millisOf("2026-10-01T03:00:00Z"),
                    periodEndMillis = millisOf("2026-10-29T03:00:00Z"),
                    isMonthPeriod = false,
                ),
                stored = listOf(
                    expense("september-pizza", 4_000, millisOf("2026-10-01T02:00:00Z"), AppCategory.FOOD.key),
                    expense("october-pizza", 3_000, millisOf("2026-10-01T04:00:00Z"), AppCategory.FOOD.key),
                ),
                timeZone = saoPaulo,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("october-pizza"), rowsOf(state).map { it.id })
            assertEquals(millisOf("2026-10-01T03:00:00Z"), state.periodStartMillis)
            assertEquals(millisOf("2026-10-29T03:00:00Z"), state.periodEndMillis)
        }

    @Test
    fun `GIVEN the bills not read yet WHEN the list opens THEN it is not said to be empty`() = runTest(testDispatcher) {
        // Given
        prepareScenario(areBillsRead = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `GIVEN the first download not read yet WHEN the list opens THEN it is not loaded`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isDownloadRead = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
        assertFalse(state.isEmpty)
        assertFalse(state.isPeriodEmpty)
    }

    @Test
    fun `GIVEN a first download with none of the category bills yet WHEN the list opens THEN it is loading them`() =
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
            assertFalse(state.isEmpty)
            assertFalse(state.isPeriodEmpty)
        }

    @Test
    fun `GIVEN a first download with some of the category bills WHEN the list opens THEN shows them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(downloading = downloadingOnline)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoadingBills)
            assertEquals(listOf(listOf("lunch", "market"), listOf("dinner")), idsByDay(state))
        }

    @Test
    fun `GIVEN nothing picked WHEN the period has no bill left THEN says the period is empty and not the categories`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryExpensesUiEvent.OnClearClicked)

            // When
            expenses.expenses.value = emptyList()
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isPeriodEmpty)
            assertFalse(state.isEmpty)
        }

    @Test
    fun `GIVEN a trip WHEN the list opens THEN shows its name currency and that it is a trip`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("Viagem Floripa", state.groupName)
            assertEquals(GroupCurrency.BRL, state.currency)
            assertTrue(state.isTrip)
            assertTrue(state.isLoaded)
        }

    @Test
    fun `GIVEN a home WHEN the list opens THEN it is not a trip`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            group = trip.copy(
                name = "Apê 302",
                type = GroupType.HOME,
            ),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals("Apê 302", state.groupName)
        assertFalse(state.isTrip)
    }

    @Test
    fun `GIVEN the list over the groups WHEN drawn THEN the rail selects Grupos`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(HomeTab.GROUPS, state.homeTab)
    }

    @Test
    fun `GIVEN the list WHEN tapping a bill THEN opens it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryExpensesUiEvent.OnExpenseClicked("market"))

        // Then
        assertEquals(
            listStack + Route.BillDetail(GROUP_ID, "market", focusCommentId = null),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the list WHEN tapping a bill THEN sends the bill clicked from the list`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryExpensesUiEvent.OnExpenseClicked("market"))

        // Then
        assertEquals(
            listOf("bill_clicked" to mapOf<String, Any>("source" to "category_expenses")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a bill of the group open beside the list WHEN tapping another bill THEN it takes that bill place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryExpensesUiEvent.OnExpenseClicked("market"))

            // When
            viewModel.onEvent(CategoryExpensesUiEvent.OnExpenseClicked("dinner"))

            // Then
            assertEquals(
                listStack + Route.BillDetail(GROUP_ID, "dinner", focusCommentId = null),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a profile over the list WHEN a late bill tap arrives THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Profile)
        val stack = navigator.backStack.value

        // When
        viewModel.onEvent(CategoryExpensesUiEvent.OnExpenseClicked("market"))

        // Then
        assertEquals(stack, navigator.backStack.value)
    }

    @Test
    fun `GIVEN the list WHEN tapping back THEN returns to the statistics`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryExpensesUiEvent.OnBackClicked)

        // Then
        assertEquals(statsStack, navigator.backStack.value)
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the list over the statistics WHEN tapping the group crumb THEN returns to the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(CategoryExpensesUiEvent.OnGroupClicked)

            // Then
            assertEquals(statsStack.dropLast(1), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a bill over the list WHEN a late group crumb arrives THEN nothing changes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.BillDetail(GROUP_ID, "market", focusCommentId = null))
        val stack = navigator.backStack.value

        // When
        viewModel.onEvent(CategoryExpensesUiEvent.OnGroupClicked)

        // Then
        assertEquals(stack, navigator.backStack.value)
    }

    @Test
    fun `GIVEN a profile over the list WHEN a late back arrives THEN nothing changes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Profile)
        val stack = navigator.backStack.value

        // When
        viewModel.onEvent(CategoryExpensesUiEvent.OnBackClicked)

        // Then
        assertEquals(stack, navigator.backStack.value)
    }

    @Test
    fun `GIVEN the list WHEN the group leaves the device THEN it closes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        groups.groups.value = emptyList()

        // Then
        assertEquals(statsStack, navigator.backStack.value)
    }

    @Test
    fun `GIVEN a bill over the list WHEN the group leaves the device THEN the list stays under the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryExpensesUiEvent.OnExpenseClicked("market"))

            // When
            groups.groups.value = emptyList()

            // Then
            assertEquals(
                listStack + Route.BillDetail(GROUP_ID, "market", focusCommentId = null),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a bill over the list WHEN the group leaves and the bill closes THEN the list closes once on top`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryExpensesUiEvent.OnExpenseClicked("market"))
            groups.groups.value = emptyList()

            // When
            navigator.navigateBack()

            // Then
            assertEquals(statsStack, navigator.backStack.value)
        }

    @Test
    fun `GIVEN the list WHEN the rail opens Grupos THEN goes back to the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryExpensesUiEvent.OnRailTabClicked(HomeTab.GROUPS))

        // Then
        assertEquals(listOf<Route>(Route.Groups), navigator.backStack.value)
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a profile over the list WHEN a late rail tab arrives THEN nothing changes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Profile)
        val stack = navigator.backStack.value

        // When
        viewModel.onEvent(CategoryExpensesUiEvent.OnRailTabClicked(HomeTab.GROUPS))

        // Then
        assertEquals(stack, navigator.backStack.value)
    }

    @Test
    fun `GIVEN the list over the groups WHEN its rail opens Buscar THEN the search opens over the groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isOverHome = true)

            // When
            viewModel.onEvent(CategoryExpensesUiEvent.OnRailSearchClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, Route.Search), navigator.backStack.value)
            assertEquals(listOf("search_opened" to mapOf<String, Any>("source" to "category_expenses")), trackedEvents)
        }

    @Test
    fun `GIVEN a bill over the list WHEN a late Buscar arrives THEN nothing changes and nothing is sent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isOverHome = true)
            navigator.navigate(Route.BillDetail(GROUP_ID, "market", focusCommentId = null))
            val stack = navigator.backStack.value

            // When
            viewModel.onEvent(CategoryExpensesUiEvent.OnRailSearchClicked)

            // Then
            assertEquals(stack, navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Food in dollars WHEN the list opens THEN lists only the dollar bills of Food`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = thisMonth(AppCategory.FOOD.key, GroupCurrency.USD),
                stored = octoberBills + usdBills,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(listOf("usd-market")), idsByDay(state))
            assertEquals(7_000L, state.totalMinor)
            assertEquals(1, state.expenseCount)
        }

    @Test
    fun `GIVEN Food in dollars in a group in reais WHEN the list opens THEN the amounts are in dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = thisMonth(AppCategory.FOOD.key, GroupCurrency.USD),
                stored = octoberBills + usdBills,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GroupCurrency.USD, state.currency)
        }

    @Test
    fun `GIVEN Food in reais with dollar bills WHEN the list opens THEN the dollar bills are not added`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(stored = octoberBills + usdBills)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GroupCurrency.BRL, state.currency)
            assertEquals(listOf(listOf("lunch", "market"), listOf("dinner")), idsByDay(state))
            assertEquals(45_000L, state.totalMinor)
            assertEquals(3, state.expenseCount)
        }

    @Test
    fun `GIVEN Food in dollars WHEN the list opens THEN the chips are the main categories of the dollar bills`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = thisMonth(AppCategory.FOOD.key, GroupCurrency.USD),
                stored = octoberBills + usdBills,
            )

            // When
            val chips = viewModel.uiState.value.chips

            // Then
            assertEquals(
                listOf(AppCategory.TRAVEL.key to false, AppCategory.FOOD.key to true),
                chips.map { it.category.id to it.isSelected },
            )
        }

    @Test
    fun `GIVEN Food in a currency the group has no bill in WHEN the list opens THEN it is empty in that currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(route = thisMonth(AppCategory.FOOD.key, GroupCurrency.EUR))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GroupCurrency.EUR, state.currency)
            assertTrue(state.days.isEmpty())
            assertEquals(0L, state.totalMinor)
            assertEquals(0, state.expenseCount)
        }

    @Test
    fun `GIVEN Food in dollars WHEN the bills are not read yet THEN the state already names the dollar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = thisMonth(AppCategory.FOOD.key, GroupCurrency.USD),
                areBillsRead = false,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GroupCurrency.USD, state.currency)
        }

    private fun rowsOf(state: CategoryExpensesUiState): List<CategoryExpenseRow> = state.days.flatMap { it.expenses }

    private fun idsByDay(state: CategoryExpensesUiState): List<List<String>> =
        state.days.map { day -> day.expenses.map { it.id } }

    private fun thisMonth(
        categoryId: String,
        currency: GroupCurrency = GroupCurrency.BRL,
    ): Route.CategoryExpenses = Route.CategoryExpenses(
        groupId = GROUP_ID,
        categoryId = categoryId,
        currency = currency,
        periodStartMillis = millisOf("2026-10-01T00:00:00Z"),
        periodEndMillis = millisOf("2026-10-29T00:00:00Z"),
        isMonthPeriod = true,
    )

    private fun wholeHistory(categoryId: String): Route.CategoryExpenses = Route.CategoryExpenses(
        groupId = GROUP_ID,
        categoryId = categoryId,
        currency = GroupCurrency.BRL,
        periodStartMillis = null,
        periodEndMillis = null,
        isMonthPeriod = false,
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

    private fun paid(
        memberId: String,
        amountMinor: Long,
    ): ExpensePayer = ExpensePayer(
        memberId = memberId,
        amountMinor = amountMinor,
    )

    private fun share(
        memberId: String,
        amountMinor: Long,
    ): ExpenseShare = ExpenseShare(
        memberId = memberId,
        amountMinor = amountMinor,
        weight = 1,
    )

    private fun expense(
        id: String,
        amountMinor: Long,
        spentAt: Long,
        categoryId: String?,
        payers: List<ExpensePayer> = listOf(paid(PIERRE, amountMinor)),
        shares: List<ExpenseShare> = listOf(share(PIERRE, amountMinor)),
        isPending: Boolean = false,
        currency: GroupCurrency = GroupCurrency.BRL,
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = id,
        currency = currency,
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

    // Why: Thursday, 29 October 2026, in the afternoon.
    private val today = millisOf("2026-10-29T15:00:00Z")

    private val saoPaulo = UtcOffset(hours = -3).asTimeZone()

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

    /*
     * Why: October's Food (R$ 450: a restaurant, the market, a lunch Pierre isn't in), Travel (a R$ 600 boat) and
     * Transport (a R$ 40 taxi still in the queue today).
     */
    private val octoberBills = listOf(
        expense(
            id = "dinner",
            amountMinor = 30_000,
            spentAt = millisOf("2026-10-24T20:00:00Z"),
            categoryId = AppCategory.FOOD_RESTAURANT.key,
            payers = listOf(paid(ANA, 30_000)),
            shares = listOf(share(PIERRE, 10_000), share(ANA, 10_000), share(BRUNO, 10_000)),
        ),
        expense(
            id = "market",
            amountMinor = 5_000,
            spentAt = millisOf("2026-10-28T10:00:00Z"),
            categoryId = AppCategory.FOOD_MARKET.key,
            shares = listOf(share(PIERRE, 2_500), share(ANA, 2_500)),
        ),
        expense(
            id = "lunch",
            amountMinor = 10_000,
            spentAt = millisOf("2026-10-28T13:00:00Z"),
            categoryId = AppCategory.FOOD.key,
            payers = listOf(paid(BRUNO, 10_000)),
            shares = listOf(share(ANA, 5_000), share(BRUNO, 5_000)),
        ),
        expense(
            id = "boat",
            amountMinor = 60_000,
            spentAt = millisOf("2026-10-28T15:00:00Z"),
            categoryId = AppCategory.TRAVEL_TOURS.key,
            shares = listOf(share(PIERRE, 15_000), share(ANA, 25_000), share(BRUNO, 20_000)),
        ),
        expense(
            id = "taxi",
            amountMinor = 4_000,
            spentAt = millisOf("2026-10-29T09:00:00Z"),
            categoryId = AppCategory.TRANSPORT_RIDE.key,
            isPending = true,
        ),
    )

    // Why: the R$ 800 hotel booked in September, before this month.
    private val septemberHotel = expense(
        id = "hotel",
        amountMinor = 80_000,
        spentAt = millisOf("2026-09-20T12:00:00Z"),
        categoryId = AppCategory.TRAVEL_LODGING.key,
    )

    // Why: a US$ 70 market and a US$ 900 boat of the same month, in a group whose default currency is the real.
    private val usdBills = listOf(
        expense(
            id = "usd-market",
            amountMinor = 7_000,
            spentAt = millisOf("2026-10-27T10:00:00Z"),
            categoryId = AppCategory.FOOD_MARKET.key,
            currency = GroupCurrency.USD,
        ),
        expense(
            id = "usd-boat",
            amountMinor = 90_000,
            spentAt = millisOf("2026-10-28T16:00:00Z"),
            categoryId = AppCategory.TRAVEL_TOURS.key,
            currency = GroupCurrency.USD,
        ),
    )

    private val statsRoute = Route.GroupStats(GROUP_ID)

    // Why: the statistics over the group's Expenses over Grupos, where back from the list returns.
    private val statsStack: List<Route>
        get() = listOf(
            Route.Groups,
            Route.GroupDetail(
                GROUP_ID,
                joinedAs = null,
                tab = GroupTab.EXPENSES,
                isOverBill = false,
            ),
            statsRoute,
        )

    private val listStack: List<Route>
        get() = statsStack + listRoute

    private val downloadingOnline = FirstDownloadProgress(
        arrivedGroupCount = 0,
        groupCount = 2,
        isWaitingForInternet = false,
    )

    private fun prepareScenario(
        route: Route.CategoryExpenses = thisMonth(AppCategory.FOOD.key),
        group: Group = trip,
        stored: List<Expense> = octoberBills + septemberHotel,
        groupCategories: List<GroupCategory> = emptyList(),
        timeZone: TimeZone = TimeZone.UTC,
        // Why: false while the bills are still being read from the device.
        areBillsRead: Boolean = true,
        // Why: the list of a group a row of Grupos opened, with Início under them, as the app has it.
        isOverHome: Boolean = false,
        // Why: a new device's first download still bringing bills down; null once it is whole.
        downloading: FirstDownloadProgress? = null,
        // Why: false while whether the account's copy is whole isn't known yet.
        isDownloadRead: Boolean = true,
    ) {
        listRoute = route
        groups = FakeGroupRepository(initialGroups = listOf(group))
        expenses = FakeExpenseRepository(initialExpenses = stored)
        categories = FakeCategoryRepository(initialGroupCategories = groupCategories)
        navigator = BackStackNavigator(startRoute = if (isOverHome) Route.Home else Route.Groups).also {
            if (isOverHome) it.navigate(Route.Groups)
            it.navigate(
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                    isOverBill = false,
                ),
            )
            it.navigate(statsRoute)
            it.navigate(route)
        }
        viewModel = CategoryExpensesViewModel(
            route = route,
            navigator = navigator,
            currentTimeMillis = { today },
            currentTimeZone = { timeZone },
            observeGroup = ObserveGroupUseCase(groups),
            observeExpenses = if (areBillsRead) ObserveExpensesUseCase(expenses) else ObserveExpenses { emptyFlow() },
            observeCategoryTree = ObserveCategoryTreeUseCase(categories),
            observeFirstDownload = ObserveFirstDownload { if (isDownloadRead) flowOf(downloading) else emptyFlow() },
            trackEvent = { name, params -> trackedEvents += name to params },
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
