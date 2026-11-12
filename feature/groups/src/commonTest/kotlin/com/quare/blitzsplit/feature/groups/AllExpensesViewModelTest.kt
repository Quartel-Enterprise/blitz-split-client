package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.expenses.billmenu.BillMenuUseCases
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMyExpenses
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
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.MinuteTicks
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiAction
import com.quare.blitzsplit.feature.groups.presentation.model.AllExpensesUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.BillMenuEvent
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
import kotlinx.datetime.TimeZone
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
    fun `GIVEN All expenses on top WHEN tapping a bill THEN opens its bill over its group`() = runTest(testDispatcher) {
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
                Route.AllExpenses,
                Route.GroupDetail.expensesOf("trio"),
                Route.BillDetail("trio", "dinner", focusCommentId = null),
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
    fun `GIVEN a bill already opened over the list WHEN a late tap on another bill comes THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trio()),
                storedExpenses = listOf(bill("dinner"), bill("taxi")),
            )
            viewModel.onEvent(AllExpensesUiEvent.OnBillClicked("trio", "dinner"))
            val opened = navigator.backStack.value
            val event = AllExpensesUiEvent.OnBillClicked("trio", "taxi")

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(opened, navigator.backStack.value)
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
                listOf(Route.Home, Route.AllExpenses, Route.SplitBill.createEditing("trio", "dinner")),
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
                listOf(Route.Home, Route.AllExpenses, Route.SplitBill.createDuplicating("trio", "dinner")),
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
                    Route.AllExpenses,
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
            assertEquals(listOf(Route.Home, Route.AllExpenses), navigator.backStack.value)
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
            assertEquals(listOf(Route.Home, Route.AllExpenses), navigator.backStack.value)
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
            assertEquals(listOf(Route.Home, Route.AllExpenses), navigator.backStack.value)
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
            assertEquals(listOf(Route.Home, Route.AllExpenses), navigator.backStack.value)
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

    // Why: that many bills of Pierre's in the trio, a minute apart, the most recent first.
    private fun manyBills(count: Int): List<Expense> = (0 until count).map { index ->
        bill("bill-$index", spentAt = now - (index + 1) * 60_000L)
    }

    // Why: Pierre is this device's user, in a group with Ana and Bruno.
    private fun trio(
        id: String = "trio",
        currency: GroupCurrency = GroupCurrency.BRL,
    ): Group = Group(
        id = id,
        name = "Apê 302",
        type = GroupType.OTHER,
        color = AvatarColor.BLUE,
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

    // Why: one payer for the whole bill, split over the members given.
    private fun bill(
        id: String,
        groupId: String = "trio",
        paidBy: String = "Pierre",
        amountMinor: Long = 9000L,
        splitOver: List<String> = everyone,
        spentAt: Long = daysAgo(5),
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = "Mercado",
        currency = GroupCurrency.BRL,
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer("member-$paidBy", amountMinor)),
        shares = splitOver.map { ExpenseShare("member-$it", amountMinor / splitOver.size, 1L) },
        spentAt = spentAt,
        isPending = false,
        updatedAt = spentAt,
        updatedByMemberId = null,
        items = null,
        categoryId = null,
    )

    private fun TestScope.prepareScenario(
        storedGroups: List<Group> = emptyList(),
        storedExpenses: List<Expense> = emptyList(),
        myMemberIds: Set<String>? = null,
        billsRead: Boolean = true,
        downloading: FirstDownloadProgress? = null,
    ) {
        groups = FakeGroupRepository(initialGroups = storedGroups)
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses.inCurrencyOf(storedGroups)).also {
            it.myMemberIds.value = myMemberIds
        }
        val settlements = FakeSettlementRepository()
        navigator = BackStackNavigator(startRoute = Route.Home).also { it.navigate(Route.AllExpenses) }
        minutes = MutableStateFlow(0)
        firstDownload = MutableStateFlow(downloading)
        pagesRead = MutableStateFlow(billsRead)
        clockMillis = now
        viewModel = AllExpensesViewModel(
            navigator = navigator,
            // Why: a page asked for while the device holds its reads only arrives once pagesRead turns true.
            observeMyExpenses = ObserveMyExpenses { limit ->
                val page = expenses.observeMyExpenses(limit)
                pagesRead.filter { it }.flatMapLatest { page }
            },
            observeGroups = ObserveGroupsUseCase(groups),
            observeMapCategories = ObserveMapCategoriesUseCase(FakeCategoryRepository()),
            observeFirstDownload = ObserveFirstDownload { firstDownload },
            currentTimeMillis = CurrentTimeMillis { clockMillis },
            currentTimeZone = CurrentTimeZone { TimeZone.UTC },
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
