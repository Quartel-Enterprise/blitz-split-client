package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.expenses.billmenu.BillMenuUseCases
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMyExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMyExpensesIn
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveBillDeletionUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveMapCategoriesUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.expenses.testing.inCurrencyOf
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ExpensePeriod
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesFilter
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesFilters
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.BillMenuEvent
import com.quare.blitzsplit.feature.groups.presentation.model.ExpenseSort
import com.quare.blitzsplit.feature.groups.presentation.model.FilterGroupOption
import com.quare.blitzsplit.feature.groups.presentation.model.PayerFilter
import com.quare.blitzsplit.feature.groups.presentation.model.ShareFilter
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.AllExpensesViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.toInstant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

// Why: "All expenses", which Home's "See all" opens: every bill of this device's user, a page at a time.
class AllExpensesViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var minutes: MutableStateFlow<Int>
    private lateinit var firstDownload: MutableStateFlow<FirstDownloadProgress?>

    // Why: whether the device answers the bill reads; false holds every page, the first included, until it turns true.
    private lateinit var pagesRead: MutableStateFlow<Boolean>
    private lateinit var viewModel: AllExpensesViewModel
    private val actions = mutableListOf<AllExpensesUiAction>()

    // Why: the device's clock, which a test moves on and then ticks a minute for.
    private var clockMillis = 0L

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN bills in two groups WHEN All expenses opens THEN lists them together by day the most recent first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip")),
                storedExpenses = listOf(
                    bill("h1", spentAt = hoursAgo(2)),
                    bill(
                        "t1",
                        groupId = "trip",
                        spentAt = hoursAgo(3),
                    ),
                    bill("h2", spentAt = hoursAgo(20)),
                    bill(
                        "t2",
                        groupId = "trip",
                        spentAt = daysAgo(4),
                    ),
                ),
            )

            // When
            val days = viewModel.uiState.value.days

            // Then
            assertEquals(
                listOf(
                    RelativeDay.TODAY to listOf("h1", "t1"),
                    RelativeDay.YESTERDAY to listOf("h2"),
                    RelativeDay.OTHER to listOf("t2"),
                ),
                days.map { day -> day.relativeDay to day.bills.map { it.id } },
            )
        }

    @Test
    fun `GIVEN a bill Ana paid WHEN All expenses opens THEN its row reads as on Home`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(bill("dinner", paidBy = "Ana")),
        )

        // When
        val row = viewModel.uiState.value.days
            .single()
            .bills
            .single()

        // Then
        assertEquals("trio", row.groupId)
        assertEquals("Apê 302", row.groupName)
        assertEquals(AvatarColor.BLUE, row.groupColor)
        assertEquals("Mercado", row.description)
        assertEquals(9_000L, row.totalMinor)
        assertEquals(GroupCurrency.BRL, row.currency)
        assertEquals(listOf("Ana"), row.payers.map { it.name })
        assertEquals(-3_000L, row.myBalanceMinor)
        assertTrue(row.category.isOther)
        assertFalse(row.isPending)
    }

    @Test
    fun `GIVEN bills of others on the device WHEN All expenses opens THEN lists only the ones Pierre is in`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill("mine", spentAt = hoursAgo(1)),
                    bill(
                        "others",
                        paidBy = "Ana",
                        splitOver = listOf("Ana", "Bruno"),
                        spentAt = hoursAgo(2),
                    ),
                ),
                myMemberIds = setOf("member-Pierre"),
            )

            // When
            val ids = shownIds()

            // Then
            assertEquals(listOf("mine"), ids)
        }

    @Test
    fun `GIVEN a bill Pierre takes no part in read as his WHEN All expenses opens THEN leaves it out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill("mine", spentAt = hoursAgo(1)),
                    bill(
                        "others",
                        paidBy = "Ana",
                        splitOver = listOf("Ana", "Bruno"),
                        spentAt = hoursAgo(2),
                    ),
                ),
            )

            // When
            val ids = shownIds()

            // Then
            assertEquals(listOf("mine"), ids)
        }

    @Test
    fun `GIVEN a bill whose group is not on the device WHEN All expenses opens THEN leaves it out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill("mine", spentAt = hoursAgo(1)),
                    bill(
                        "orphan",
                        groupId = "gone",
                        spentAt = hoursAgo(2),
                    ),
                ),
            )

            // When
            val ids = shownIds()

            // Then
            assertEquals(listOf("mine"), ids)
        }

    @Test
    fun `GIVEN All expenses opening WHEN reading the first page THEN asks the device for 31 bills`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(5),
            )

            // When
            val limits = expenses.myExpenseLimits.toList()

            // Then
            assertEquals(listOf(31), limits)
        }

    @Test
    fun `GIVEN 31 bills WHEN All expenses opens THEN shows 30 and says more are left`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = manyBills(31),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(30, state.days.sumOf { it.bills.size })
        assertTrue(state.hasMore)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `GIVEN exactly 30 bills WHEN All expenses opens THEN shows them all and says none are left`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(30),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(30, state.days.sumOf { it.bills.size })
            assertFalse(state.hasMore)
        }

    @Test
    fun `GIVEN more bills than a page WHEN the list reaches its end THEN asks the device for 61 bills`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(70),
            )

            // When
            viewModel.onEvent(AllExpensesUiEvent.OnEndReached)

            // Then
            assertEquals(listOf(31, 61), expenses.myExpenseLimits)
        }

    @Test
    fun `GIVEN more bills than a page WHEN the next page arrives THEN shows 60 bills and stops loading`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(70),
            )
            pagesRead.value = false
            viewModel.onEvent(AllExpensesUiEvent.OnEndReached)

            // When
            pagesRead.value = true

            // Then
            val state = viewModel.uiState.value
            assertEquals(60, state.days.sumOf { it.bills.size })
            assertTrue(state.hasMore)
            assertFalse(state.isLoadingMore)
            assertFalse(state.showsMoreSkeleton)
        }

    @Test
    fun `GIVEN the next page not read yet WHEN the list reached its end THEN shows it loading with a skeleton row`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(70),
            )
            pagesRead.value = false

            // When
            viewModel.onEvent(AllExpensesUiEvent.OnEndReached)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isLoadingMore)
            assertTrue(state.showsMoreSkeleton)
            assertEquals(30, state.days.sumOf { it.bills.size })
        }

    @Test
    fun `GIVEN the next page loading WHEN the list reaches its end again THEN asks nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(100),
            )
            pagesRead.value = false
            viewModel.onEvent(AllExpensesUiEvent.OnEndReached)

            // When
            viewModel.onEvent(AllExpensesUiEvent.OnEndReached)

            // Then
            assertEquals(listOf(31, 61), expenses.myExpenseLimits)
        }

    @Test
    fun `GIVEN the second page read and more left WHEN the list reaches its end THEN asks for 91 bills`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(100),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnEndReached)

            // When
            viewModel.onEvent(AllExpensesUiEvent.OnEndReached)

            // Then
            assertEquals(listOf(31, 61, 91), expenses.myExpenseLimits)
        }

    @Test
    fun `GIVEN no more bills left WHEN the list reaches its end THEN asks nothing more`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = manyBills(10),
        )

        // When
        viewModel.onEvent(AllExpensesUiEvent.OnEndReached)

        // Then
        assertEquals(listOf(31), expenses.myExpenseLimits)
        assertFalse(viewModel.uiState.value.isLoadingMore)
    }

    @Test
    fun `GIVEN the first page not read yet WHEN the list reaches its end THEN asks nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(70),
                billsRead = false,
            )

            // When
            viewModel.onEvent(AllExpensesUiEvent.OnEndReached)

            // Then
            assertEquals(listOf(31), expenses.myExpenseLimits)
            assertFalse(viewModel.uiState.value.isLoadingMore)
        }

    @Test
    fun `GIVEN a bill on the list WHEN it is deleted elsewhere THEN it leaves the list`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(bill("dinner", spentAt = hoursAgo(1)), bill("taxi", spentAt = hoursAgo(2))),
        )

        // When
        expenses.expenses.update { list -> list.filterNot { it.id == "dinner" } }

        // Then
        assertEquals(listOf("taxi"), shownIds())
    }

    @Test
    fun `GIVEN a bill on the list WHEN it is edited elsewhere THEN its row follows`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(bill("dinner")),
        )

        // When
        expenses.expenses.update { list -> list.map { it.copy(description = "Jantar") } }

        // Then
        assertEquals(
            "Jantar",
            viewModel.uiState.value.days
                .single()
                .bills
                .single()
                .description,
        )
    }

    @Test
    fun `GIVEN the list open WHEN a new bill is added THEN it comes first`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(bill("dinner", spentAt = hoursAgo(2))),
        )

        // When
        expenses.expenses.update { it + bill("lunch", spentAt = hoursAgo(1)).copy(currency = GroupCurrency.BRL) }

        // Then
        assertEquals(listOf("lunch", "dinner"), shownIds())
    }

    @Test
    fun `GIVEN 31 bills WHEN one is deleted elsewhere THEN shows 30 and says none are left`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(31),
            )

            // When
            expenses.expenses.update { it.drop(1) }

            // Then
            val state = viewModel.uiState.value
            assertEquals(30, state.days.sumOf { it.bills.size })
            assertFalse(state.hasMore)
        }

    @Test
    fun `GIVEN the first page not read yet WHEN All expenses opens THEN shows the skeleton and not the empty list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                billsRead = false,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsSkeleton)
            assertFalse(state.showsEmpty)
            assertFalse(state.showsMoreSkeleton)
        }

    @Test
    fun `GIVEN no bills and no first download WHEN All expenses opens THEN shows the empty list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsEmpty)
            assertFalse(state.showsSkeleton)
        }

    @Test
    fun `GIVEN no bills during the first download WHEN the list opens THEN shows the skeleton not the empty list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                downloading = downloadProgress,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsSkeleton)
            assertFalse(state.showsEmpty)
        }

    @Test
    fun `GIVEN bills during the first download WHEN All expenses opens THEN shows them with a skeleton row`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
                downloading = downloadProgress,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsMoreSkeleton)
            assertFalse(state.showsSkeleton)
            assertFalse(state.showsEmpty)
        }

    @Test
    fun `GIVEN bills during the first download WHEN it ends THEN the skeleton row goes`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(bill("dinner")),
            downloading = downloadProgress,
        )

        // When
        firstDownload.value = null

        // Then
        assertFalse(viewModel.uiState.value.showsMoreSkeleton)
    }

    @Test
    fun `GIVEN a bill from this afternoon on an open list WHEN midnight passes THEN its day says yesterday`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner", spentAt = hoursAgo(2))),
            )

            // When
            clockMillis = Instant.parse("2026-11-01T00:01:00Z").toEpochMilliseconds()
            minutes.value += 1

            // Then
            assertEquals(
                RelativeDay.YESTERDAY,
                viewModel.uiState.value.days
                    .single()
                    .relativeDay,
            )
        }

    @Test
    fun `GIVEN All expenses on top WHEN tapping a bill THEN opens its bill over All expenses`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
            )
            val event = AllExpensesUiEvent.OnBillClicked("trio", "dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.AllExpenses.create(groupId = null),
                    Route.BillDetail("trio", "dinner", focusCommentId = null),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a bill All expenses opened beside it WHEN tapping another bill THEN takes the place of that bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner"), bill("lunch")),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnBillClicked("trio", "dinner"))

            // When
            viewModel.onEvent(AllExpensesUiEvent.OnBillClicked("trio", "lunch"))

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.AllExpenses.create(groupId = null),
                    Route.BillDetail("trio", "lunch", focusCommentId = null),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN All expenses on top WHEN tapping a bill THEN tracks bill_clicked from all expenses`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
            )
            val event = AllExpensesUiEvent.OnBillClicked("trio", "dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("bill_clicked" to fromAllExpenses), trackedEvents)
        }

    @Test
    fun `GIVEN All expenses on top WHEN tapping back THEN returns to Home`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))
        val event = AllExpensesUiEvent.OnBackClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a bill opened over the list WHEN a late back on the list comes THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnBillClicked("trio", "dinner"))
            val opened = navigator.backStack.value
            val event = AllExpensesUiEvent.OnBackClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(opened, navigator.backStack.value)
        }

    @Test
    fun `GIVEN All expenses on top WHEN tapping Edit in a bill menu THEN opens Edit bill over the list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
            )
            val event = BillMenuEvent.OnEditClicked("trio", "dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.AllExpenses.create(groupId = null),
                    Route.SplitBill.createEditing("trio", "dinner"),
                ),
                navigator.backStack.value,
            )
            assertEquals(listOf("bill_edit_clicked" to fromAllExpenses), trackedEvents)
        }

    @Test
    fun `GIVEN All expenses on top WHEN tapping Duplicate in a bill menu THEN opens Split a bill over the list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
            )
            val event = BillMenuEvent.OnDuplicateClicked("trio", "dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.AllExpenses.create(groupId = null),
                    Route.SplitBill.createDuplicating("trio", "dinner"),
                ),
                navigator.backStack.value,
            )
            assertEquals(listOf("bill_duplicate_clicked" to fromAllExpenses), trackedEvents)
        }

    @Test
    fun `GIVEN All expenses on top WHEN tapping Share in a bill menu THEN opens Share bill over the list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
            )
            val event = BillMenuEvent.OnShareClicked("trio", "dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.AllExpenses.create(groupId = null),
                    Route.ShareBill(
                        groupId = "trio",
                        expenseId = "dinner",
                    ),
                ),
                navigator.backStack.value,
            )
            assertEquals(listOf("bill_share_clicked" to fromAllExpenses), trackedEvents)
        }

    @Test
    fun `GIVEN a bill opened over the list WHEN a late Edit in a bill menu comes THEN opens and tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnBillClicked("trio", "dinner"))
            val opened = navigator.backStack.value
            trackedEvents.clear()
            val event = BillMenuEvent.OnEditClicked("trio", "dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(opened, navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a bill no longer on the device WHEN tapping Share in its menu THEN opens nothing and tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
            )
            val event = BillMenuEvent.OnShareClicked("trio", "gone")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Route.Home, Route.AllExpenses.create(groupId = null)), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a bill in an unknown currency WHEN tapping Edit in its menu THEN opens nothing and tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(currency = GroupCurrency.fromCode("XYZ"))),
                storedExpenses = listOf(bill("dinner")),
            )
            val event = BillMenuEvent.OnEditClicked("trio", "dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Route.Home, Route.AllExpenses.create(groupId = null)), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a bill in an unknown currency WHEN tapping Duplicate in its menu THEN opens nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(currency = GroupCurrency.fromCode("XYZ"))),
                storedExpenses = listOf(bill("dinner")),
            )
            val event = BillMenuEvent.OnDuplicateClicked("trio", "dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Route.Home, Route.AllExpenses.create(groupId = null)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a bill Pierre paid WHEN tapping Delete in its menu THEN asks with what he stops receiving`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
            )
            val event = BillMenuEvent.OnDeleteClicked("trio", "dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                BillDeletion(
                    description = "Mercado",
                    myBalanceMinor = 6_000L,
                    currency = GroupCurrency.BRL,
                    settledPair = null,
                ),
                viewModel.uiState.value.billToDelete,
            )
            assertFalse(viewModel.uiState.value.isDeletingBill)
            assertEquals(listOf("bill_delete_clicked" to fromAllExpenses), trackedEvents)
        }

    @Test
    fun `GIVEN the delete question WHEN confirming THEN the bill leaves the list and All expenses stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner", spentAt = hoursAgo(1)), bill("taxi", spentAt = hoursAgo(2))),
            )
            viewModel.onEvent(BillMenuEvent.OnDeleteClicked("trio", "dinner"))
            val event = BillMenuEvent.OnDeleteConfirmed

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("dinner"), expenses.deletedIds)
            assertEquals(listOf("taxi"), shownIds())
            assertNull(viewModel.uiState.value.billToDelete)
            assertFalse(viewModel.uiState.value.isDeletingBill)
            assertEquals(listOf(Route.Home, Route.AllExpenses.create(groupId = null)), navigator.backStack.value)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the delete question WHEN confirming THEN tracks bill_delete_confirmed from all expenses`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
            )
            viewModel.onEvent(BillMenuEvent.OnDeleteClicked("trio", "dinner"))
            val event = BillMenuEvent.OnDeleteConfirmed

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(fromAllExpenses),
                trackedEvents.filter { it.first == "bill_delete_confirmed" }.map { it.second },
            )
        }

    @Test
    fun `GIVEN the delete question and a slow delete WHEN confirming THEN Delete waits while deleting`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
            )
            viewModel.onEvent(BillMenuEvent.OnDeleteClicked("trio", "dinner"))
            val asked = viewModel.uiState.value.billToDelete
            expenses.deleteGate = CompletableDeferred()
            val event = BillMenuEvent.OnDeleteConfirmed

            // When
            viewModel.onEvent(event)

            // Then
            assertTrue(viewModel.uiState.value.isDeletingBill)
            assertEquals(asked, viewModel.uiState.value.billToDelete)
        }

    @Test
    fun `GIVEN the device fails to delete WHEN confirming the question THEN says so and keeps the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
            )
            expenses.deleteError = IllegalStateException("disk full")
            viewModel.onEvent(BillMenuEvent.OnDeleteClicked("trio", "dinner"))
            val event = BillMenuEvent.OnDeleteConfirmed

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf<AllExpensesUiAction>(AllExpensesUiAction.ShowBillDeleteError), actions)
            assertNull(viewModel.uiState.value.billToDelete)
            assertEquals(listOf("dinner"), shownIds())
        }

    @Test
    fun `GIVEN the delete question WHEN dismissing it THEN keeps the bill and tracks bill_delete_cancelled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner")),
            )
            viewModel.onEvent(BillMenuEvent.OnDeleteClicked("trio", "dinner"))
            val event = BillMenuEvent.OnDeleteDismissed

            // When
            viewModel.onEvent(event)

            // Then
            assertNull(viewModel.uiState.value.billToDelete)
            assertEquals(listOf("dinner"), shownIds())
            assertEquals(
                listOf(fromAllExpenses),
                trackedEvents.filter { it.first == "bill_delete_cancelled" }.map { it.second },
            )
        }

    @Test
    fun `GIVEN bills WHEN searching a description in upper case and without its accent THEN finds only that bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill("market", spentAt = hoursAgo(1)),
                    bill(
                        "taxi",
                        spentAt = hoursAgo(2),
                        description = "Táxi do aeroporto",
                    ),
                ),
            )
            val event = AllExpensesUiEvent.OnQueryChanged("TAXI")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("taxi"), shownIds())
            assertEquals("TAXI", viewModel.uiState.value.filters.query)
            assertTrue(viewModel.uiState.value.filters.isFiltering)
        }

    @Test
    fun `GIVEN no description holding the query WHEN searching with a typo THEN finds the bill a typo away`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill("market", spentAt = hoursAgo(1)),
                    bill(
                        "dinner",
                        spentAt = hoursAgo(2),
                        description = "Dinner",
                    ),
                ),
            )
            val event = AllExpensesUiEvent.OnQueryChanged("diner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("dinner"), shownIds())
        }

    @Test
    fun `GIVEN one description holding the query WHEN searching THEN leaves out the ones only a typo away`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        spentAt = hoursAgo(1),
                        description = "Dinner",
                    ),
                    bill(
                        "diner",
                        spentAt = hoursAgo(2),
                        description = "Diner da esquina",
                    ),
                ),
            )
            val event = AllExpensesUiEvent.OnQueryChanged("diner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("diner"), shownIds())
        }

    @Test
    fun `GIVEN bills WHEN typing only spaces THEN shows every bill unfiltered and keeps reading a page`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("market", spentAt = hoursAgo(1)), bill("taxi", spentAt = hoursAgo(2))),
            )
            val event = AllExpensesUiEvent.OnQueryChanged("   ")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("market", "taxi"), shownIds())
            assertFalse(viewModel.uiState.value.filters.isFiltering)
            assertEquals(listOf(31), expenses.myExpenseLimits)
        }

    @Test
    fun `GIVEN bills WHEN searching what none holds THEN shows no results and not the empty list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("market")),
            )
            val event = AllExpensesUiEvent.OnQueryChanged("zzzz")

            // When
            viewModel.onEvent(event)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.days.isEmpty())
            assertTrue(state.showsNoResults)
            assertFalse(state.showsEmpty)
        }

    @Test
    fun `GIVEN bills in three groups WHEN picking one group THEN shows only its bills`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio(), trio(id = "trip"), trio(id = "work")),
            storedExpenses = listOf(
                bill("h1", spentAt = hoursAgo(1)),
                bill(
                    "t1",
                    groupId = "trip",
                    spentAt = hoursAgo(2),
                ),
                bill(
                    "w1",
                    groupId = "work",
                    spentAt = hoursAgo(3),
                ),
            ),
        )
        val event = AllExpensesUiEvent.OnGroupToggled("trip")

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf("t1"), shownIds())
        assertEquals(setOf("trip"), viewModel.uiState.value.filters.groupIds)
        assertTrue(viewModel.uiState.value.filters.isFiltering)
    }

    @Test
    fun `GIVEN one group picked WHEN picking a second group THEN shows the bills of both`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio(), trio(id = "trip"), trio(id = "work")),
            storedExpenses = listOf(
                bill("h1", spentAt = hoursAgo(1)),
                bill(
                    "t1",
                    groupId = "trip",
                    spentAt = hoursAgo(2),
                ),
                bill(
                    "w1",
                    groupId = "work",
                    spentAt = hoursAgo(3),
                ),
            ),
        )
        viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trip"))
        val event = AllExpensesUiEvent.OnGroupToggled("work")

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf("t1", "w1"), shownIds())
        assertEquals(setOf("trip", "work"), viewModel.uiState.value.filters.groupIds)
    }

    @Test
    fun `GIVEN one group picked WHEN letting it go THEN shows the bills of every group`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio(), trio(id = "trip")),
            storedExpenses = listOf(
                bill("h1", spentAt = hoursAgo(1)),
                bill(
                    "t1",
                    groupId = "trip",
                    spentAt = hoursAgo(2),
                ),
            ),
        )
        viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trip"))
        val event = AllExpensesUiEvent.OnGroupToggled("trip")

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf("h1", "t1"), shownIds())
        assertTrue(
            viewModel.uiState.value.filters.groupIds
                .isEmpty(),
        )
        assertFalse(viewModel.uiState.value.filters.isFiltering)
    }

    @Test
    fun `GIVEN bills in Food subcategories and elsewhere WHEN picking Food THEN shows Food and its subcategories`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "market",
                        spentAt = hoursAgo(1),
                        categoryId = AppCategory.FOOD_MARKET.key,
                    ),
                    bill(
                        "restaurant",
                        spentAt = hoursAgo(2),
                        categoryId = AppCategory.FOOD_RESTAURANT.key,
                    ),
                    bill(
                        "food",
                        spentAt = hoursAgo(3),
                        categoryId = AppCategory.FOOD.key,
                    ),
                    bill(
                        "fuel",
                        spentAt = hoursAgo(4),
                        categoryId = AppCategory.TRANSPORT_FUEL.key,
                    ),
                    bill("none", spentAt = hoursAgo(5)),
                ),
            )
            val event = AllExpensesUiEvent.OnCategoryToggled(AppCategory.FOOD.key)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("market", "restaurant", "food"), shownIds())
            assertEquals(setOf(AppCategory.FOOD.key), viewModel.uiState.value.filters.categoryIds)
        }

    @Test
    fun `GIVEN a group subcategory under Food WHEN picking Food THEN shows its bills too`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio(), trio(id = "trip")),
            storedExpenses = listOf(
                bill(
                    "acai",
                    spentAt = hoursAgo(1),
                    categoryId = "acai",
                ),
                bill(
                    "fuel",
                    groupId = "trip",
                    spentAt = hoursAgo(2),
                    categoryId = AppCategory.TRANSPORT_FUEL.key,
                ),
            ),
            storedCategories = listOf(
                groupCategory("acai", parentId = AppCategory.FOOD.key),
                groupCategory("boat", groupId = "trip"),
            ),
        )
        val event = AllExpensesUiEvent.OnCategoryToggled(AppCategory.FOOD.key)

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf("acai"), shownIds())
    }

    @Test
    fun `GIVEN a main category of a group with a subcategory WHEN picking it THEN shows the bills of both`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip")),
                storedExpenses = listOf(
                    bill(
                        "bbq",
                        spentAt = hoursAgo(1),
                        categoryId = "bbq",
                    ),
                    bill(
                        "steak",
                        spentAt = hoursAgo(2),
                        categoryId = "steak",
                    ),
                    bill(
                        "boat",
                        groupId = "trip",
                        spentAt = hoursAgo(3),
                        categoryId = "boat",
                    ),
                    bill(
                        "market",
                        spentAt = hoursAgo(4),
                        categoryId = AppCategory.FOOD_MARKET.key,
                    ),
                ),
                storedCategories = listOf(
                    groupCategory("bbq"),
                    groupCategory("steak", parentId = "bbq"),
                    groupCategory("boat", groupId = "trip"),
                ),
            )
            val event = AllExpensesUiEvent.OnCategoryToggled("bbq")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("bbq", "steak"), shownIds())
        }

    @Test
    fun `GIVEN categories of two groups WHEN All expenses opens THEN offers the main categories of every group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip")),
                storedCategories = listOf(
                    groupCategory("bbq"),
                    groupCategory("steak", parentId = "bbq"),
                    groupCategory("boat", groupId = "trip"),
                ),
            )

            // When
            val options = viewModel.uiState.value.categoryOptions

            // Then
            assertEquals(
                AppCategory.mains.map { it.key }.dropLast(1) + listOf("bbq", "boat", AppCategory.OTHER.key),
                options.map { it.id },
            )
        }

    @Test
    fun `GIVEN two groups one without bills WHEN All expenses opens THEN offers both as group options`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    trio(name = "Casa da praia"),
                    trio(
                        id = "trip",
                        name = "Viagem Floripa",
                        color = AvatarColor.GREEN,
                    ),
                ),
                storedExpenses = listOf(bill("market")),
            )

            // When
            val options = viewModel.uiState.value.groupOptions

            // Then
            assertEquals(
                listOf(
                    FilterGroupOption(
                        id = "trio",
                        name = "Casa da praia",
                        initials = "CP",
                        color = AvatarColor.BLUE,
                        photoUrl = null,
                    ),
                    FilterGroupOption(
                        id = "trip",
                        name = "Viagem Floripa",
                        initials = "VF",
                        color = AvatarColor.GREEN,
                        photoUrl = null,
                    ),
                ),
                options,
            )
        }

    @Test
    fun `GIVEN bills Pierre lent owes and broke even on WHEN picking to receive THEN shows only the one he lent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill("lent", spentAt = hoursAgo(1)),
                    bill(
                        "owe",
                        paidBy = "Ana",
                        spentAt = hoursAgo(2),
                    ),
                    bill(
                        "even",
                        splitOver = listOf("Pierre"),
                        spentAt = hoursAgo(3),
                    ),
                ),
            )
            val event = AllExpensesUiEvent.OnShareFilterPicked(ShareFilter.TO_RECEIVE)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("lent"), shownIds())
            assertEquals(ShareFilter.TO_RECEIVE, viewModel.uiState.value.filters.share)
        }

    @Test
    fun `GIVEN bills Pierre lent owes and broke even on WHEN picking to pay THEN shows only the one he owes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill("lent", spentAt = hoursAgo(1)),
                    bill(
                        "owe",
                        paidBy = "Ana",
                        spentAt = hoursAgo(2),
                    ),
                    bill(
                        "even",
                        splitOver = listOf("Pierre"),
                        spentAt = hoursAgo(3),
                    ),
                ),
            )
            val event = AllExpensesUiEvent.OnShareFilterPicked(ShareFilter.TO_PAY)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("owe"), shownIds())
        }

    @Test
    fun `GIVEN to receive picked WHEN picking all THEN shows every bill again`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(
                bill("lent", spentAt = hoursAgo(1)),
                bill(
                    "owe",
                    paidBy = "Ana",
                    spentAt = hoursAgo(2),
                ),
            ),
        )
        viewModel.onEvent(AllExpensesUiEvent.OnShareFilterPicked(ShareFilter.TO_RECEIVE))
        val event = AllExpensesUiEvent.OnShareFilterPicked(ShareFilter.ALL)

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf("lent", "owe"), shownIds())
        assertFalse(viewModel.uiState.value.filters.isFiltering)
    }

    @Test
    fun `GIVEN bills Pierre paid alone with Ana and not at all WHEN picking you paid THEN shows the two he paid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill("alone", spentAt = hoursAgo(1)),
                    bill(
                        "together",
                        spentAt = hoursAgo(2),
                        payers = listOf("Pierre" to 4_500L, "Ana" to 4_500L),
                    ),
                    bill(
                        "ana",
                        paidBy = "Ana",
                        spentAt = hoursAgo(3),
                    ),
                ),
            )
            val event = AllExpensesUiEvent.OnPayerFilterPicked(PayerFilter.YOU)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("alone", "together"), shownIds())
            assertEquals(PayerFilter.YOU, viewModel.uiState.value.filters.payer)
        }

    @Test
    fun `GIVEN bills Pierre paid alone with Ana and not at all WHEN picking others THEN shows the one he did not`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill("alone", spentAt = hoursAgo(1)),
                    bill(
                        "together",
                        spentAt = hoursAgo(2),
                        payers = listOf("Pierre" to 4_500L, "Ana" to 4_500L),
                    ),
                    bill(
                        "ana",
                        paidBy = "Ana",
                        spentAt = hoursAgo(3),
                    ),
                ),
            )
            val event = AllExpensesUiEvent.OnPayerFilterPicked(PayerFilter.OTHERS)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("ana"), shownIds())
        }

    @Test
    fun `GIVEN bills of three days WHEN sorting by oldest THEN lists the days and their bills the oldest first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill("today-late", spentAt = hoursAgo(1)),
                    bill("today-early", spentAt = hoursAgo(2)),
                    bill("yesterday", spentAt = hoursAgo(20)),
                    bill("old", spentAt = daysAgo(4)),
                ),
            )
            val event = AllExpensesUiEvent.OnSortPicked(ExpenseSort.OLDEST)

            // When
            viewModel.onEvent(event)

            // Then
            val state = viewModel.uiState.value
            assertEquals(
                listOf(
                    RelativeDay.OTHER to listOf("old"),
                    RelativeDay.YESTERDAY to listOf("yesterday"),
                    RelativeDay.TODAY to listOf("today-early", "today-late"),
                ),
                state.days.map { day -> day.relativeDay to day.bills.map { it.id } },
            )
            assertTrue(state.showsDayHeadings)
            assertFalse(state.filters.isFiltering)
        }

    @Test
    fun `GIVEN bills of different totals WHEN sorting by largest THEN one list the largest and newest first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "small",
                        amountMinor = 800L,
                        spentAt = hoursAgo(1),
                    ),
                    bill(
                        "mid-new",
                        amountMinor = 5_000L,
                        spentAt = hoursAgo(2),
                    ),
                    bill(
                        "mid-old",
                        amountMinor = 5_000L,
                        spentAt = daysAgo(3),
                    ),
                    bill(
                        "big",
                        amountMinor = 12_000L,
                        spentAt = daysAgo(2),
                    ),
                ),
            )
            val event = AllExpensesUiEvent.OnSortPicked(ExpenseSort.LARGEST)

            // When
            viewModel.onEvent(event)

            // Then
            val state = viewModel.uiState.value
            assertEquals(1, state.days.size)
            assertEquals(0L, state.days.single().dayStartMillis)
            assertEquals(listOf("big", "mid-new", "mid-old", "small"), shownIds())
            assertFalse(state.showsDayHeadings)
        }

    @Test
    fun `GIVEN bills of different totals WHEN sorting by smallest THEN the smallest and newest first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "small",
                        amountMinor = 800L,
                        spentAt = hoursAgo(1),
                    ),
                    bill(
                        "mid-new",
                        amountMinor = 5_000L,
                        spentAt = hoursAgo(2),
                    ),
                    bill(
                        "mid-old",
                        amountMinor = 5_000L,
                        spentAt = daysAgo(3),
                    ),
                    bill(
                        "big",
                        amountMinor = 12_000L,
                        spentAt = daysAgo(2),
                    ),
                ),
            )
            val event = AllExpensesUiEvent.OnSortPicked(ExpenseSort.SMALLEST)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("small", "mid-new", "mid-old", "big"), shownIds())
            assertEquals(1, viewModel.uiState.value.days.size)
        }

    @Test
    fun `GIVEN no bills WHEN sorting by largest THEN lists no day and shows the empty list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            val event = AllExpensesUiEvent.OnSortPicked(ExpenseSort.LARGEST)

            // When
            viewModel.onEvent(event)

            // Then
            val state = viewModel.uiState.value
            assertEquals(emptyList(), state.days)
            assertTrue(state.showsEmpty)
            assertFalse(state.showsNoResults)
        }

    @Test
    fun `GIVEN sorted by largest WHEN a group with no bills is picked THEN lists no day and shows no results`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip")),
                storedExpenses = listOf(bill("market")),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnSortPicked(ExpenseSort.LARGEST))
            val event = AllExpensesUiEvent.OnGroupToggled("trip")

            // When
            viewModel.onEvent(event)

            // Then
            val state = viewModel.uiState.value
            assertEquals(emptyList(), state.days)
            assertTrue(state.showsNoResults)
            assertFalse(state.showsEmpty)
        }

    @Test
    fun `GIVEN a search filters and an order WHEN clearing the filters THEN resets all but the order`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip")),
                storedExpenses = listOf(
                    bill("lent", spentAt = hoursAgo(1)),
                    bill(
                        "owe",
                        paidBy = "Ana",
                        spentAt = hoursAgo(2),
                    ),
                    bill(
                        "trip",
                        groupId = "trip",
                        spentAt = hoursAgo(3),
                        description = "Barco",
                    ),
                ),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnSortPicked(ExpenseSort.OLDEST))
            viewModel.onEvent(AllExpensesUiEvent.OnQueryChanged("merc"))
            viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trio"))
            viewModel.onEvent(AllExpensesUiEvent.OnCategoryToggled(AppCategory.OTHER.key))
            viewModel.onEvent(AllExpensesUiEvent.OnShareFilterPicked(ShareFilter.TO_RECEIVE))
            viewModel.onEvent(AllExpensesUiEvent.OnPayerFilterPicked(PayerFilter.YOU))
            val event = AllExpensesUiEvent.OnClearFiltersClicked

            // When
            viewModel.onEvent(event)

            // Then
            val state = viewModel.uiState.value
            assertEquals(AllExpensesFilters.none.copy(sort = ExpenseSort.OLDEST), state.filters)
            assertFalse(state.filters.isFiltering)
            assertEquals(listOf("trip", "owe", "lent"), shownIds())
        }

    @Test
    fun `GIVEN All expenses open WHEN picking a group THEN reads 31 bills of that group only`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(5),
            )
            val event = AllExpensesUiEvent.OnGroupToggled("trio")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(31), expenses.myExpenseLimits)
            assertEquals(listOf(setOf("trio") to 31), expenses.myExpenseInReads)
        }

    @Test
    fun `GIVEN a group picked WHEN picking a second group THEN reads 31 bills of both`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio(), trio(id = "trip")),
            storedExpenses = manyBills(5) + manyBills(5, groupId = "trip"),
        )
        viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trio"))
        val event = AllExpensesUiEvent.OnGroupToggled("trip")

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(setOf("trio", "trip") to 31, expenses.myExpenseInReads.last())
        assertEquals(10, shownCount())
    }

    @Test
    fun `GIVEN bills of two groups shown WHEN picking one before its window is read THEN keeps the rows until it is`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip")),
                storedExpenses = manyBills(2) + manyBills(2, groupId = "trip"),
            )
            val shownBefore = shownIds()
            pagesRead.value = false

            // When
            viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trip"))

            // Then
            assertEquals(shownBefore, shownIds())
            assertEquals(setOf("trip"), viewModel.uiState.value.filters.groupIds)
            pagesRead.value = true
            assertEquals(listOf("trip-0", "trip-1"), shownIds())
        }

    @Test
    fun `GIVEN a group picked WHEN switching to another before its window is read THEN keeps the rows until it is`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip")),
                storedExpenses = manyBills(2) + manyBills(2, groupId = "trip"),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trio"))
            pagesRead.value = false

            // When
            viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trio"))
            viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trip"))

            // Then
            assertEquals(listOf("trio-0", "trio-1"), shownIds())
            assertEquals(setOf("trip"), viewModel.uiState.value.filters.groupIds)
            pagesRead.value = true
            assertEquals(listOf("trip-0", "trip-1"), shownIds())
        }

    @Test
    fun `GIVEN a group picked WHEN sorting by oldest THEN reads every bill of the device`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = manyBills(5),
        )
        viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trio"))
        val event = AllExpensesUiEvent.OnSortPicked(ExpenseSort.OLDEST)

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf(31, Int.MAX_VALUE), expenses.myExpenseLimits)
    }

    @Test
    fun `GIVEN 40 newer bills elsewhere WHEN picking the group of an older bill THEN shows that bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip")),
                storedExpenses = manyBills(40, groupId = "trip") +
                    bill(
                        "old",
                        spentAt = daysAgo(30),
                    ),
            )
            val event = AllExpensesUiEvent.OnGroupToggled("trio")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("old"), shownIds())
            assertFalse(viewModel.uiState.value.hasMore)
        }

    @Test
    fun `GIVEN All expenses open WHEN sorting by oldest THEN reads every bill of the device`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(5),
            )
            val event = AllExpensesUiEvent.OnSortPicked(ExpenseSort.OLDEST)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(31, Int.MAX_VALUE), expenses.myExpenseLimits)
        }

    @Test
    fun `GIVEN 70 bills whose totals grow with age WHEN sorting by largest THEN shows the 30 largest of every bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = (0 until 70).map { index ->
                    bill(
                        "bill-$index",
                        amountMinor = 100L * (index + 1),
                        spentAt = now - (index + 1) * 60_000L,
                    )
                },
            )
            val event = AllExpensesUiEvent.OnSortPicked(ExpenseSort.LARGEST)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals((69 downTo 40).map { "bill-$it" }, shownIds())
            assertTrue(viewModel.uiState.value.hasMore)
        }

    @Test
    fun `GIVEN 70 bills of a group WHEN picking it THEN shows 30 and says more are left`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = manyBills(70),
        )
        val event = AllExpensesUiEvent.OnGroupToggled("trio")

        // When
        viewModel.onEvent(event)

        // Then
        val state = viewModel.uiState.value
        assertEquals(30, shownCount())
        assertTrue(state.hasMore)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `GIVEN exactly 30 bills of a group WHEN picking it THEN shows them all and says none are left`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(30),
            )
            val event = AllExpensesUiEvent.OnGroupToggled("trio")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(30, shownCount())
            assertFalse(viewModel.uiState.value.hasMore)
        }

    @Test
    fun `GIVEN a group picked with 70 bills WHEN the list reaches its end THEN reads 61 of the group and shows 60`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(70),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trio"))
            val event = AllExpensesUiEvent.OnEndReached

            // When
            viewModel.onEvent(event)

            // Then
            val state = viewModel.uiState.value
            assertEquals(60, shownCount())
            assertTrue(state.hasMore)
            assertFalse(state.isLoadingMore)
            assertFalse(state.showsMoreSkeleton)
            assertEquals(listOf(31, 61), expenses.myExpenseInReads.map { it.second })
            assertEquals(listOf(31), expenses.myExpenseLimits)
        }

    @Test
    fun `GIVEN a group picked and its next page not read yet WHEN the list reached its end THEN shows it loading`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(70),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trio"))
            pagesRead.value = false
            val event = AllExpensesUiEvent.OnEndReached

            // When
            viewModel.onEvent(event)

            // Then
            val state = viewModel.uiState.value
            assertEquals(30, shownCount())
            assertTrue(state.isLoadingMore)
        }

    @Test
    fun `GIVEN a group picked and 60 of 70 shown WHEN the list reaches its end THEN shows 70 and none left`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(70),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trio"))
            viewModel.onEvent(AllExpensesUiEvent.OnEndReached)
            val event = AllExpensesUiEvent.OnEndReached

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(70, shownCount())
            assertFalse(viewModel.uiState.value.hasMore)
        }

    @Test
    fun `GIVEN a group picked and 60 bills shown WHEN picking another group THEN shows the first 30 again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip")),
                storedExpenses = manyBills(70) + manyBills(70, groupId = "trip"),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trio"))
            viewModel.onEvent(AllExpensesUiEvent.OnEndReached)
            val event = AllExpensesUiEvent.OnGroupToggled("trip")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(30, shownCount())
            assertTrue(viewModel.uiState.value.hasMore)
        }

    @Test
    fun `GIVEN a group picked and 60 bills shown WHEN typing a search THEN shows the first 30 again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(70),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trio"))
            viewModel.onEvent(AllExpensesUiEvent.OnEndReached)
            val event = AllExpensesUiEvent.OnQueryChanged("merc")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(30, shownCount())
            assertTrue(viewModel.uiState.value.hasMore)
        }

    @Test
    fun `GIVEN sorted by largest and 60 bills shown WHEN sorting by oldest THEN shows the first 30 again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(70),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnSortPicked(ExpenseSort.LARGEST))
            viewModel.onEvent(AllExpensesUiEvent.OnEndReached)
            val event = AllExpensesUiEvent.OnSortPicked(ExpenseSort.OLDEST)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(30, shownCount())
        }

    @Test
    fun `GIVEN a group picked and 60 bills shown WHEN clearing the filters THEN reads a page of the device again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(70),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trio"))
            viewModel.onEvent(AllExpensesUiEvent.OnEndReached)
            val event = AllExpensesUiEvent.OnClearFiltersClicked

            // When
            viewModel.onEvent(event)

            // Then
            val state = viewModel.uiState.value
            assertEquals(31, expenses.myExpenseLimits.last())
            assertEquals(30, shownCount())
            assertTrue(state.hasMore)
            assertFalse(state.isLoadingMore)
        }

    @Test
    fun `GIVEN back at the default filters WHEN the list reaches its end THEN asks the device for 61 bills`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(70),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trio"))
            viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trio"))
            val event = AllExpensesUiEvent.OnEndReached

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(31, 61), expenses.myExpenseLimits.takeLast(2))
            assertEquals(60, shownCount())
        }

    @Test
    fun `GIVEN every bill still being read WHEN sorting by largest THEN keeps the page shown until they arrive`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = (0 until 70).map { index ->
                    bill(
                        "bill-$index",
                        amountMinor = 100L * (index + 1),
                        spentAt = now - (index + 1) * 60_000L,
                    )
                },
            )
            val shownBefore = shownIds()
            pagesRead.value = false
            val event = AllExpensesUiEvent.OnSortPicked(ExpenseSort.LARGEST)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(shownBefore, shownIds())
        }

    @Test
    fun `GIVEN sorted by largest and 60 bills shown WHEN picking largest again THEN keeps showing 60`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(70),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnSortPicked(ExpenseSort.LARGEST))
            viewModel.onEvent(AllExpensesUiEvent.OnEndReached)
            val readsBefore = expenses.myExpenseLimits.toList()
            val event = AllExpensesUiEvent.OnSortPicked(ExpenseSort.LARGEST)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(60, shownCount())
            assertEquals(readsBefore, expenses.myExpenseLimits)
        }

    @Test
    fun `GIVEN 60 bills shown at the default WHEN picking all for your share THEN keeps 60 and reads nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(70),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnEndReached)
            val event = AllExpensesUiEvent.OnShareFilterPicked(ShareFilter.ALL)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(31, 61), expenses.myExpenseLimits)
            assertEquals(60, shownCount())
        }

    @Test
    fun `GIVEN All expenses open WHEN picking a group THEN tracks the group filter changed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            val event = AllExpensesUiEvent.OnGroupToggled("trio")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(filterChanged("group")), trackedEvents)
        }

    @Test
    fun `GIVEN All expenses open WHEN picking a category THEN tracks the category filter changed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            val event = AllExpensesUiEvent.OnCategoryToggled(AppCategory.FOOD.key)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(filterChanged("category")), trackedEvents)
        }

    @Test
    fun `GIVEN All expenses open WHEN picking your share THEN tracks the share filter changed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            val event = AllExpensesUiEvent.OnShareFilterPicked(ShareFilter.TO_PAY)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(filterChanged("share")), trackedEvents)
        }

    @Test
    fun `GIVEN All expenses open WHEN picking who paid THEN tracks the payer filter changed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            val event = AllExpensesUiEvent.OnPayerFilterPicked(PayerFilter.OTHERS)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(filterChanged("payer")), trackedEvents)
        }

    @Test
    fun `GIVEN All expenses open WHEN sorting by smallest THEN tracks the sort changed with it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            val event = AllExpensesUiEvent.OnSortPicked(ExpenseSort.SMALLEST)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf("all_expenses_sort_changed" to mapOf<String, Any>("sort" to ExpenseSort.SMALLEST)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a group picked WHEN clearing the filters THEN tracks the filters cleared`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trio()))
        viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trio"))
        trackedEvents.clear()
        val event = AllExpensesUiEvent.OnClearFiltersClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf("all_expenses_filters_cleared" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN All expenses open WHEN typing a search THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(trio()),
            storedExpenses = listOf(bill("market")),
        )
        val event = AllExpensesUiEvent.OnQueryChanged("merc")

        // When
        viewModel.onEvent(event)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN See all of a group WHEN All expenses opens before reading THEN that group is already picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip")),
                storedExpenses = listOf(bill("h1"), bill("t1", groupId = "trip")),
                billsRead = false,
                route = Route.AllExpenses.create(groupId = "trip"),
                underneath = listOf(Route.Home, groupScreen("trip")),
            )

            // When
            val filters = viewModel.uiState.value.filters

            // Then
            assertEquals(setOf("trip"), filters.groupIds)
            assertTrue(filters.isFiltering)
        }

    @Test
    fun `GIVEN See all of a group with bills in three groups WHEN All expenses opens THEN shows only its bills`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip"), trio(id = "work")),
                storedExpenses = listOf(
                    bill("h1", spentAt = hoursAgo(1)),
                    bill(
                        "t1",
                        groupId = "trip",
                        spentAt = hoursAgo(2),
                    ),
                    bill(
                        "w1",
                        groupId = "work",
                        spentAt = hoursAgo(3),
                    ),
                    bill(
                        "t2",
                        groupId = "trip",
                        spentAt = hoursAgo(4),
                    ),
                ),
                route = Route.AllExpenses.create(groupId = "trip"),
                underneath = listOf(Route.Home, groupScreen("trip")),
            )

            // When
            val ids = shownIds()

            // Then
            assertEquals(listOf("t1", "t2"), ids)
            assertEquals(setOf("trip"), viewModel.uiState.value.filters.groupIds)
        }

    @Test
    fun `GIVEN 31 recent bills elsewhere WHEN See all of a group opens THEN still shows that group older bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip")),
                storedExpenses = manyBills(31) + bill(
                    "t1",
                    groupId = "trip",
                    spentAt = daysAgo(30),
                ),
                route = Route.AllExpenses.create(groupId = "trip"),
                underneath = listOf(Route.Home, groupScreen("trip")),
            )

            // When
            val ids = shownIds()

            // Then
            assertEquals(listOf("t1"), ids)
            assertFalse(viewModel.uiState.value.hasMore)
        }

    @Test
    fun `GIVEN See all of a group WHEN letting the group go THEN shows the bills of every group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip")),
                storedExpenses = listOf(
                    bill("h1", spentAt = hoursAgo(1)),
                    bill(
                        "t1",
                        groupId = "trip",
                        spentAt = hoursAgo(2),
                    ),
                ),
                route = Route.AllExpenses.create(groupId = "trip"),
                underneath = listOf(Route.Home, groupScreen("trip")),
            )
            val event = AllExpensesUiEvent.OnGroupToggled("trip")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("h1", "t1"), shownIds())
            assertTrue(
                viewModel.uiState.value.filters.groupIds
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.filters.isFiltering)
        }

    @Test
    fun `GIVEN See all of a group WHEN clearing the filters THEN shows the bills of every group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip")),
                storedExpenses = listOf(
                    bill("h1", spentAt = hoursAgo(1)),
                    bill(
                        "t1",
                        groupId = "trip",
                        spentAt = hoursAgo(2),
                    ),
                ),
                route = Route.AllExpenses.create(groupId = "trip"),
                underneath = listOf(Route.Home, groupScreen("trip")),
            )
            val event = AllExpensesUiEvent.OnClearFiltersClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("h1", "t1"), shownIds())
            assertEquals(AllExpensesFilters.none, viewModel.uiState.value.filters)
        }

    @Test
    fun `GIVEN See all of a group with no bills of Pierre WHEN All expenses opens THEN shows no results`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip")),
                storedExpenses = listOf(bill("h1")),
                route = Route.AllExpenses.create(groupId = "trip"),
                underneath = listOf(Route.Home, groupScreen("trip")),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.days.isEmpty())
            assertTrue(state.showsNoResults)
            assertFalse(state.showsEmpty)
            assertFalse(state.showsSkeleton)
        }

    @Test
    fun `GIVEN See all of a group not on the device WHEN All expenses opens THEN keeps it picked and shows nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("h1")),
                route = Route.AllExpenses.create(groupId = "gone"),
                underneath = listOf(Route.Home, groupScreen("gone")),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(setOf("gone"), state.filters.groupIds)
            assertTrue(state.days.isEmpty())
            assertTrue(state.showsNoResults)
            assertFalse(state.showsEmpty)
        }

    @Test
    fun `GIVEN All expenses over a group WHEN tapping back THEN returns to the group screen`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(id = "trip")),
                route = Route.AllExpenses.create(groupId = "trip"),
                underneath = listOf(Route.Home, groupScreen("trip")),
            )
            val event = AllExpensesUiEvent.OnBackClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Route.Home, groupScreen("trip")), navigator.backStack.value)
        }

    @Test
    fun `GIVEN All expenses over a group WHEN tapping a bill THEN opens its bill over All expenses`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(id = "trip")),
                storedExpenses = listOf(bill("dinner", groupId = "trip")),
                route = Route.AllExpenses.create(groupId = "trip"),
                underneath = listOf(Route.Home, groupScreen("trip")),
            )
            val event = AllExpensesUiEvent.OnBillClicked("trip", "dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    groupScreen("trip"),
                    Route.AllExpenses.create(groupId = "trip"),
                    Route.BillDetail("trip", "dinner", focusCommentId = null),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN All expenses over a group WHEN tapping Edit in a bill menu THEN opens Edit bill over the list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(id = "trip")),
                storedExpenses = listOf(bill("dinner", groupId = "trip")),
                route = Route.AllExpenses.create(groupId = "trip"),
                underneath = listOf(Route.Home, groupScreen("trip")),
            )
            val event = BillMenuEvent.OnEditClicked("trip", "dinner")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    groupScreen("trip"),
                    Route.AllExpenses.create(groupId = "trip"),
                    Route.SplitBill.createEditing("trip", "dinner"),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN All expenses opened from Home See all WHEN it opens THEN sits in the Home tab`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))

            // When
            val homeTab = viewModel.uiState.value.homeTab

            // Then
            assertEquals(HomeTab.HOME, homeTab)
        }

    @Test
    fun `GIVEN a group Home opened WHEN its See all opens All expenses THEN sits in the Home tab`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(id = "trip")),
                route = Route.AllExpenses.create(groupId = "trip"),
                underneath = listOf(Route.Home, groupScreen("trip")),
            )

            // When
            val homeTab = viewModel.uiState.value.homeTab

            // Then
            assertEquals(HomeTab.HOME, homeTab)
        }

    @Test
    fun `GIVEN a group opened from the Groups tab WHEN its See all opens All expenses THEN sits in the Groups tab`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(id = "trip")),
                route = Route.AllExpenses.create(groupId = "trip"),
                underneath = listOf(Route.Home, Route.Groups, groupScreen("trip")),
            )

            // When
            val homeTab = viewModel.uiState.value.homeTab

            // Then
            assertEquals(HomeTab.GROUPS, homeTab)
        }

    @Test
    fun `GIVEN bills of October and of late 30 September WHEN picking this month THEN shows only October`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill("today", spentAt = inBrasilia("2026-10-31T09:00")),
                    bill("first", spentAt = inBrasilia("2026-10-01T00:10")),
                    bill("september", spentAt = inBrasilia("2026-09-30T23:30")),
                ),
                timeZone = brasilia,
            )
            val event = AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.THIS_MONTH)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("today", "first"), shownIds())
            assertEquals(ExpensePeriod.THIS_MONTH, viewModel.uiState.value.filters.period)
        }

    @Test
    fun `GIVEN bills around September WHEN picking last month THEN shows September whole by its local days`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill("october", spentAt = inBrasilia("2026-10-01T00:10")),
                    bill("september-last", spentAt = inBrasilia("2026-09-30T23:30")),
                    bill("september-first", spentAt = inBrasilia("2026-09-01T00:10")),
                    bill("august", spentAt = inBrasilia("2026-08-31T23:50")),
                ),
                timeZone = brasilia,
            )
            val event = AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.LAST_MONTH)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("september-last", "september-first"), shownIds())
        }

    @Test
    fun `GIVEN bills of this year and of last December WHEN picking this year THEN leaves last December out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill("october", spentAt = inBrasilia("2026-10-20T10:00")),
                    bill("new-year", spentAt = inBrasilia("2026-01-01T00:10")),
                    bill("new-year-eve", spentAt = inBrasilia("2025-12-31T23:30")),
                ),
                timeZone = brasilia,
            )
            val event = AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.THIS_YEAR)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("october", "new-year"), shownIds())
        }

    @Test
    fun `GIVEN a bill dated next week WHEN picking this year THEN leaves it out as after today`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill("next-week", spentAt = inBrasilia("2026-11-07T10:00")),
                    bill("today", spentAt = inBrasilia("2026-10-31T09:00")),
                ),
                timeZone = brasilia,
            )
            val event = AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.THIS_YEAR)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("today"), shownIds())
        }

    @Test
    fun `GIVEN bills around three days WHEN picking them as custom days THEN shows both ends whole only`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = customRangeBills(),
                timeZone = brasilia,
            )
            val event = AllExpensesUiEvent.OnCustomPeriodPicked(
                startMillis = tenthNoon,
                endMillis = twelfthNoon,
            )

            // When
            viewModel.onEvent(event)

            // Then
            val filters = viewModel.uiState.value.filters
            assertEquals(listOf("last-day", "first-day"), shownIds())
            assertEquals(ExpensePeriod.CUSTOM, filters.period)
            assertEquals(tenthNoon, filters.customStartMillis)
            assertEquals(twelfthNoon, filters.customEndMillis)
        }

    @Test
    fun `GIVEN bills around three days WHEN picking the custom range last day first THEN swaps them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = customRangeBills(),
                timeZone = brasilia,
            )
            val event = AllExpensesUiEvent.OnCustomPeriodPicked(
                startMillis = twelfthNoon,
                endMillis = tenthNoon,
            )

            // When
            viewModel.onEvent(event)

            // Then
            val filters = viewModel.uiState.value.filters
            assertEquals(tenthNoon, filters.customStartMillis)
            assertEquals(twelfthNoon, filters.customEndMillis)
            assertEquals(listOf("last-day", "first-day"), shownIds())
        }

    @Test
    fun `GIVEN no custom days picked yet WHEN picking a custom range THEN changes nothing and reads nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = customRangeBills(),
                timeZone = brasilia,
            )
            val event = AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.CUSTOM)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(AllExpensesFilters.none, viewModel.uiState.value.filters)
            assertEquals(listOf("after", "last-day", "first-day", "before"), shownIds())
            assertEquals(listOf(31), expenses.myExpenseLimits)
        }

    @Test
    fun `GIVEN custom days then this month picked WHEN picking custom again THEN shows those days again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = customRangeBills() + bill("later", spentAt = inBrasilia("2026-10-20T10:00")),
                timeZone = brasilia,
            )
            viewModel.onEvent(
                AllExpensesUiEvent.OnCustomPeriodPicked(
                    startMillis = tenthNoon,
                    endMillis = twelfthNoon,
                ),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.THIS_MONTH))
            val event = AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.CUSTOM)

            // When
            viewModel.onEvent(event)

            // Then
            val filters = viewModel.uiState.value.filters
            assertEquals(ExpensePeriod.CUSTOM, filters.period)
            assertEquals(tenthNoon, filters.customStartMillis)
            assertEquals(twelfthNoon, filters.customEndMillis)
            assertEquals(listOf("last-day", "first-day"), shownIds())
        }

    @Test
    fun `GIVEN bills of last year only WHEN picking last month THEN shows no results and not the empty list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("old", spentAt = inBrasilia("2025-06-10T10:00"))),
                timeZone = brasilia,
            )
            val event = AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.LAST_MONTH)

            // When
            viewModel.onEvent(event)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.filters.isFiltering)
            assertTrue(state.showsNoResults)
            assertFalse(state.showsEmpty)
        }

    @Test
    fun `GIVEN All expenses open WHEN picking this month THEN reads every bill of the device`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(5),
            )
            val event = AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.THIS_MONTH)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(31, Int.MAX_VALUE), expenses.myExpenseLimits)
        }

    @Test
    fun `GIVEN custom days picked WHEN picking the whole history THEN stops filtering and reads a page again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = customRangeBills(),
                timeZone = brasilia,
            )
            viewModel.onEvent(
                AllExpensesUiEvent.OnCustomPeriodPicked(
                    startMillis = tenthNoon,
                    endMillis = twelfthNoon,
                ),
            )
            val event = AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.ALL)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(31, Int.MAX_VALUE, 31), expenses.myExpenseLimits)
            assertFalse(viewModel.uiState.value.filters.isFiltering)
            assertEquals(listOf("after", "last-day", "first-day", "before"), shownIds())
        }

    @Test
    fun `GIVEN 40 bills this month WHEN picking this month THEN shows 30 and says more are left`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(40),
            )
            val event = AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.THIS_MONTH)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(30, shownCount())
            assertTrue(viewModel.uiState.value.hasMore)
        }

    @Test
    fun `GIVEN this month picked with 40 bills WHEN the list reaches its end THEN shows 40 and none left`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = manyBills(40),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.THIS_MONTH))
            val event = AllExpensesUiEvent.OnEndReached

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(40, shownCount())
            assertFalse(viewModel.uiState.value.hasMore)
        }

    @Test
    fun `GIVEN custom days and an order picked WHEN clearing the filters THEN resets the period and keeps the order`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = customRangeBills(),
                timeZone = brasilia,
            )
            viewModel.onEvent(AllExpensesUiEvent.OnSortPicked(ExpenseSort.OLDEST))
            viewModel.onEvent(
                AllExpensesUiEvent.OnCustomPeriodPicked(
                    startMillis = tenthNoon,
                    endMillis = twelfthNoon,
                ),
            )
            val event = AllExpensesUiEvent.OnClearFiltersClicked

            // When
            viewModel.onEvent(event)

            // Then
            val filters = viewModel.uiState.value.filters
            assertEquals(AllExpensesFilters.none.copy(sort = ExpenseSort.OLDEST), filters)
            assertNull(filters.customStartMillis)
            assertEquals(listOf("before", "first-day", "last-day", "after"), shownIds())
        }

    @Test
    fun `GIVEN Food and Transport bills of two months WHEN picking Food and this month THEN shows October Food`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "food-october",
                        spentAt = inBrasilia("2026-10-20T10:00"),
                        categoryId = AppCategory.FOOD_MARKET.key,
                    ),
                    bill(
                        "fuel-october",
                        spentAt = inBrasilia("2026-10-19T10:00"),
                        categoryId = AppCategory.TRANSPORT_FUEL.key,
                    ),
                    bill(
                        "food-september",
                        spentAt = inBrasilia("2026-09-20T10:00"),
                        categoryId = AppCategory.FOOD.key,
                    ),
                ),
                timeZone = brasilia,
            )
            viewModel.onEvent(AllExpensesUiEvent.OnCategoryToggled(AppCategory.FOOD.key))
            val event = AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.THIS_MONTH)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("food-october"), shownIds())
        }

    @Test
    fun `GIVEN bills of two groups and months WHEN picking a group and last month THEN shows its September bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip")),
                storedExpenses = listOf(
                    bill("trio-september", spentAt = inBrasilia("2026-09-21T10:00")),
                    bill(
                        "trip-october",
                        groupId = "trip",
                        spentAt = inBrasilia("2026-10-20T10:00"),
                    ),
                    bill(
                        "trip-september",
                        groupId = "trip",
                        spentAt = inBrasilia("2026-09-20T10:00"),
                    ),
                ),
                timeZone = brasilia,
            )
            viewModel.onEvent(AllExpensesUiEvent.OnGroupToggled("trip"))
            val event = AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.LAST_MONTH)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("trip-september"), shownIds())
        }

    @Test
    fun `GIVEN sorted by largest WHEN picking this month THEN lists only this month largest first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(
                    bill(
                        "small",
                        amountMinor = 800L,
                        spentAt = inBrasilia("2026-10-25T10:00"),
                    ),
                    bill(
                        "big",
                        amountMinor = 12_000L,
                        spentAt = inBrasilia("2026-10-05T10:00"),
                    ),
                    bill(
                        "huge-september",
                        amountMinor = 90_000L,
                        spentAt = inBrasilia("2026-09-30T23:30"),
                    ),
                ),
                timeZone = brasilia,
            )
            viewModel.onEvent(AllExpensesUiEvent.OnSortPicked(ExpenseSort.LARGEST))
            val event = AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.THIS_MONTH)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf("big", "small"), shownIds())
            assertFalse(viewModel.uiState.value.showsDayHeadings)
        }

    @Test
    fun `GIVEN this month picked on its last day WHEN midnight passes THEN its bills leave the list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("today", spentAt = inBrasilia("2026-10-31T09:00"))),
                timeZone = brasilia,
            )
            viewModel.onEvent(AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.THIS_MONTH))

            // When
            clockMillis = inBrasilia("2026-11-01T00:01")
            minutes.value += 1

            // Then
            assertEquals(emptyList(), shownIds())
            assertTrue(viewModel.uiState.value.showsNoResults)
        }

    @Test
    fun `GIVEN All expenses open WHEN picking a period THEN tracks the period filter changed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            val event = AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.LAST_MONTH)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(filterChanged("period")), trackedEvents)
        }

    @Test
    fun `GIVEN All expenses open WHEN picking custom days THEN tracks the period filter changed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            val event = AllExpensesUiEvent.OnCustomPeriodPicked(
                startMillis = tenthNoon,
                endMillis = twelfthNoon,
            )

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(filterChanged("period")), trackedEvents)
        }

    @Test
    fun `GIVEN a statistics category WHEN opening before reading THEN picks its group category and period`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                billsRead = false,
                route = fromStatistics(period = ExpensePeriod.THIS_MONTH),
                underneath = listOf(Route.Home, groupScreen("trio")),
            )

            // When
            val filters = viewModel.uiState.value.filters

            // Then
            assertEquals(setOf("trio"), filters.groupIds)
            assertEquals(setOf(AppCategory.FOOD.key), filters.categoryIds)
            assertEquals(ExpensePeriod.THIS_MONTH, filters.period)
        }

    @Test
    fun `GIVEN a statistics category and bills elsewhere WHEN All expenses opens THEN shows only what it counted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio(), trio(id = "trip")),
                storedExpenses = listOf(
                    bill(
                        "counted",
                        spentAt = inBrasilia("2026-10-20T10:00"),
                        categoryId = AppCategory.FOOD_RESTAURANT.key,
                    ),
                    bill(
                        "september",
                        spentAt = inBrasilia("2026-09-30T23:30"),
                        categoryId = AppCategory.FOOD.key,
                    ),
                    bill(
                        "fuel",
                        spentAt = inBrasilia("2026-10-19T10:00"),
                        categoryId = AppCategory.TRANSPORT_FUEL.key,
                    ),
                    bill(
                        "other-group",
                        groupId = "trip",
                        spentAt = inBrasilia("2026-10-18T10:00"),
                        categoryId = AppCategory.FOOD.key,
                    ),
                ),
                route = fromStatistics(period = ExpensePeriod.THIS_MONTH),
                underneath = listOf(Route.Home, groupScreen("trio")),
                timeZone = brasilia,
            )

            // When
            val ids = shownIds()

            // Then
            assertEquals(listOf("counted"), ids)
        }

    @Test
    fun `GIVEN a statistics category with a period WHEN All expenses opens THEN period category and group lead`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                route = fromStatistics(period = ExpensePeriod.LAST_MONTH),
                underneath = listOf(Route.Home, groupScreen("trio")),
            )

            // When
            val order = viewModel.uiState.value.filterOrder

            // Then
            assertEquals(
                listOf(
                    AllExpensesFilter.PERIOD,
                    AllExpensesFilter.CATEGORY,
                    AllExpensesFilter.GROUP,
                    AllExpensesFilter.SORT,
                    AllExpensesFilter.SHARE,
                    AllExpensesFilter.PAYER,
                ),
                order,
            )
        }

    @Test
    fun `GIVEN a statistics category of all time and no group WHEN All expenses opens THEN only category leads`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                route = fromStatistics(
                    groupId = null,
                    period = ExpensePeriod.ALL,
                ),
            )

            // When
            val order = viewModel.uiState.value.filterOrder

            // Then
            assertEquals(
                listOf(
                    AllExpensesFilter.CATEGORY,
                    AllExpensesFilter.SORT,
                    AllExpensesFilter.GROUP,
                    AllExpensesFilter.PERIOD,
                    AllExpensesFilter.SHARE,
                    AllExpensesFilter.PAYER,
                ),
                order,
            )
        }

    @Test
    fun `GIVEN a category of the statistics with custom days WHEN All expenses opens THEN shows only those days`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = customRangeBills(categoryId = AppCategory.FOOD.key),
                route = fromStatistics(
                    period = ExpensePeriod.CUSTOM,
                    customStartMillis = tenthNoon,
                    customEndMillis = twelfthNoon,
                ),
                underneath = listOf(Route.Home, groupScreen("trio")),
                timeZone = brasilia,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(ExpensePeriod.CUSTOM, state.filters.period)
            assertEquals(tenthNoon, state.filters.customStartMillis)
            assertEquals(twelfthNoon, state.filters.customEndMillis)
            assertEquals(listOf("last-day", "first-day"), shownIds())
            assertEquals(AllExpensesFilter.PERIOD, state.filterOrder.first())
        }

    @Test
    fun `GIVEN statistics custom days without the last WHEN All expenses opens THEN falls back to the whole history`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = customRangeBills(categoryId = AppCategory.FOOD.key),
                route = fromStatistics(
                    groupId = null,
                    period = ExpensePeriod.CUSTOM,
                    customStartMillis = tenthNoon,
                ),
                timeZone = brasilia,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(ExpensePeriod.ALL, state.filters.period)
            assertNull(state.filters.customStartMillis)
            assertNull(state.filters.customEndMillis)
            assertEquals(listOf("after", "last-day", "first-day", "before"), shownIds())
            assertEquals(AllExpensesFilter.CATEGORY, state.filterOrder.first())
        }

    @Test
    fun `GIVEN See all of a group WHEN All expenses opens THEN the chips keep their default order`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                route = Route.AllExpenses.create(groupId = "trio"),
                underneath = listOf(Route.Home, groupScreen("trio")),
            )

            // When
            val order = viewModel.uiState.value.filterOrder

            // Then
            assertEquals(AllExpensesFilter.entries, order)
        }

    @Test
    fun `GIVEN Home See all WHEN picking a period and a category THEN the chips keep their default order`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroups = listOf(trio()))
            viewModel.onEvent(AllExpensesUiEvent.OnCategoryToggled(AppCategory.FOOD.key))
            val event = AllExpensesUiEvent.OnPeriodPicked(ExpensePeriod.THIS_MONTH)

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(AllExpensesFilter.entries, viewModel.uiState.value.filterOrder)
        }

    @Test
    fun `GIVEN a statistics category WHEN clearing the filters THEN the chips keep the order they opened with`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                route = fromStatistics(period = ExpensePeriod.THIS_MONTH),
                underneath = listOf(Route.Home, groupScreen("trio")),
            )
            val opened = viewModel.uiState.value.filterOrder
            val event = AllExpensesUiEvent.OnClearFiltersClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(AllExpensesFilters.none, viewModel.uiState.value.filters)
            assertEquals(opened, viewModel.uiState.value.filterOrder)
            assertEquals(AllExpensesFilter.PERIOD, opened.first())
        }

    private val everyone = listOf("Pierre", "Ana", "Bruno")

    private val fromAllExpenses: Map<String, Any> = mapOf("source" to "all_expenses")

    // Why: Saturday, 31 October 2026, 15:00 in UTC, the time zone the tests count days in.
    private val now = Instant.parse("2026-10-31T15:00:00Z").toEpochMilliseconds()

    private val downloadProgress = FirstDownloadProgress(
        arrivedGroupCount = 1,
        groupCount = 3,
        isWaitingForInternet = false,
    )

    private fun hoursAgo(hours: Int): Long = now - hours.hours.inWholeMilliseconds

    private fun daysAgo(days: Int): Long = now - days.days.inWholeMilliseconds

    // Why: the ids of the rows shown, in the order the list draws them.
    private fun shownIds(): List<String> = viewModel.uiState.value.days
        .flatMap { it.bills }
        .map { it.id }

    private fun shownCount(): Int = viewModel.uiState.value.days
        .sumOf { it.bills.size }

    private fun filterChanged(filter: String): Pair<String, Map<String, Any>> =
        "all_expenses_filter_changed" to mapOf("source" to "all_expenses", "filter" to filter)

    // Why: that many bills of Pierre's in the group, a minute apart, the most recent first.
    private fun manyBills(
        count: Int,
        groupId: String = "trio",
    ): List<Expense> = (0 until count).map { index ->
        bill(
            "$groupId-$index",
            groupId = groupId,
            spentAt = now - (index + 1) * 60_000L,
        )
    }

    // Why: a category created in a group, a main one without parentId.
    private fun groupCategory(
        id: String,
        groupId: String = "trio",
        parentId: String? = null,
    ): GroupCategory = GroupCategory(
        id = id,
        groupId = groupId,
        parentId = parentId,
        name = id,
        icon = null,
        color = null,
        createdByMemberId = null,
        isDeleted = false,
        isPending = false,
    )

    // Why: the group's screen on its Expenses tab, whose "See all" opens All expenses over it.
    private fun groupScreen(id: String): Route.GroupDetail = Route.GroupDetail(
        groupId = id,
        joinedAs = null,
        tab = GroupTab.EXPENSES,
        isOverBill = false,
    )

    // Why: Pierre is this device's user, in a group with Ana and Bruno.
    private fun trio(
        id: String = "trio",
        currency: GroupCurrency = GroupCurrency.BRL,
        name: String = "Apê 302",
        color: AvatarColor = AvatarColor.BLUE,
    ): Group = Group(
        id = id,
        name = name,
        type = GroupType.OTHER,
        color = color,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = currency,
        members = listOf(member("Pierre").copy(isMe = true), member("Ana"), member("Bruno")),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = daysAgo(10),
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    private fun member(name: String): GroupMember = GroupMember(
        id = "member-$name",
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = false,
        hasJoined = false,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    // Why: one payer for the whole bill unless payers says otherwise, split over the members given.
    private fun bill(
        id: String,
        groupId: String = "trio",
        paidBy: String = "Pierre",
        amountMinor: Long = 9000L,
        splitOver: List<String> = everyone,
        spentAt: Long = daysAgo(5),
        description: String = "Mercado",
        categoryId: String? = null,
        payers: List<Pair<String, Long>> = listOf(paidBy to amountMinor),
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = description,
        currency = GroupCurrency.BRL,
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = payers.map { (name, paid) -> ExpensePayer("member-$name", paid) },
        shares = splitOver.map { ExpenseShare("member-$it", amountMinor / splitOver.size, 1L) },
        spentAt = spentAt,
        isPending = false,
        updatedAt = spentAt,
        updatedByMemberId = null,
        items = null,
        categoryId = categoryId,
    )

    // Why: UTC-3 with no summer time, so a bill late in the evening there falls on the next day in UTC.
    private val brasilia = UtcOffset(hours = -3).asTimeZone()

    // Why: noon of 10 and 12 October 2026 in UTC-3, as the custom range picker gives its days.
    private val tenthNoon = inBrasilia("2026-10-10T12:00")
    private val twelfthNoon = inBrasilia("2026-10-12T12:00")

    // Why: a moment as a clock in UTC-3 reads it.
    private fun inBrasilia(dateTime: String): Long = LocalDateTime
        .parse(dateTime)
        .toInstant(brasilia)
        .toEpochMilliseconds()

    // Why: a bill just before, at the very start of, at the very end of and just after 10 to 12 October in UTC-3.
    private fun customRangeBills(categoryId: String? = null): List<Expense> = listOf(
        bill(
            "after",
            spentAt = inBrasilia("2026-10-13T00:10"),
            categoryId = categoryId,
        ),
        bill(
            "last-day",
            spentAt = inBrasilia("2026-10-12T23:50"),
            categoryId = categoryId,
        ),
        bill(
            "first-day",
            spentAt = inBrasilia("2026-10-10T00:10"),
            categoryId = categoryId,
        ),
        bill(
            "before",
            spentAt = inBrasilia("2026-10-09T23:30"),
            categoryId = categoryId,
        ),
    )

    // Why: a main category of the statistics, which opens All expenses with what they showed.
    private fun fromStatistics(
        groupId: String? = "trio",
        period: ExpensePeriod,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
    ): Route.AllExpenses = Route.AllExpenses(
        groupId = groupId,
        categoryId = AppCategory.FOOD.key,
        period = period,
        customStartMillis = customStartMillis,
        customEndMillis = customEndMillis,
    )

    private fun TestScope.prepareScenario(
        storedGroups: List<Group> = emptyList(),
        storedExpenses: List<Expense> = emptyList(),
        myMemberIds: Set<String>? = null,
        billsRead: Boolean = true,
        downloading: FirstDownloadProgress? = null,
        storedCategories: List<GroupCategory> = emptyList(),
        route: Route.AllExpenses = Route.AllExpenses.create(groupId = null),
        // Why: what lies under All expenses: Home, or the group whose "See all" opened it.
        underneath: List<Route> = listOf(Route.Home),
        timeZone: TimeZone = TimeZone.UTC,
    ) {
        groups = FakeGroupRepository(initialGroups = storedGroups)
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses.inCurrencyOf(storedGroups)).also {
            it.myMemberIds.value = myMemberIds
        }
        val settlements = FakeSettlementRepository()
        navigator = BackStackNavigator(startRoute = underneath.first()).also { stack ->
            underneath.drop(1).forEach(stack::navigate)
            stack.navigate(route)
        }
        minutes = MutableStateFlow(0)
        firstDownload = MutableStateFlow(downloading)
        pagesRead = MutableStateFlow(billsRead)
        clockMillis = now
        viewModel = AllExpensesViewModel(
            route = route,
            navigator = navigator,
            // Why: a page asked for while the device holds its reads only arrives once pagesRead turns true.
            observeMyExpenses = ObserveMyExpenses { limit ->
                val page = expenses.observeMyExpenses(limit)
                pagesRead.filter { it }.flatMapLatest { page }
            },
            observeMyExpensesIn = ObserveMyExpensesIn { groupIds, limit ->
                val page = expenses.observeMyExpensesIn(groupIds, limit)
                pagesRead.filter { it }.flatMapLatest { page }
            },
            observeGroups = ObserveGroupsUseCase(groups),
            observeMapCategories = ObserveMapCategoriesUseCase(FakeCategoryRepository(storedCategories)),
            observeFirstDownload = ObserveFirstDownload { firstDownload },
            currentTimeMillis = CurrentTimeMillis { clockMillis },
            currentTimeZone = CurrentTimeZone { timeZone },
            minuteTicks = MinuteTicks { minutes.map { } },
            billMenuUseCases = BillMenuUseCases(
                observeExpense = ObserveExpenseUseCase(expenses),
                observeBillDeletion = ObserveBillDeletionUseCase(groups, expenses, settlements),
                deleteExpense = DeleteExpenseUseCase(expenses, InMemoryDeletedBillRepository()) { clockMillis },
            ),
            trackEvent = { name, params -> trackedEvents += name to params },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
