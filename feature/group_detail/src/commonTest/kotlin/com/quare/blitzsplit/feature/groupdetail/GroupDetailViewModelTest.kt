package com.quare.blitzsplit.feature.groupdetail

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.DebtKind
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupDeletedNotice
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitDialog
import com.quare.blitzsplit.core.expenses.domain.model.GroupFace
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeDeletedBillNoticeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveDeletedBillNoticeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettlementsUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.data.repository.InMemoryGroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeGroupDeletionUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupDeletionsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObservePinnedGroupIdsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.SetGroupPinnedUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupPinRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup
import com.quare.blitzsplit.core.sync.domain.usecase.LeaveGroup
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRowPayer
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupDetailViewModel
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupScreenUseCases
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class GroupDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var pins: FakeGroupPinRepository
    private lateinit var viewModel: GroupDetailViewModel
    private lateinit var account: FakeAccountRepository
    private val actions = mutableListOf<GroupDetailUiAction>()
    private val deletedBills = InMemoryDeletedBillRepository()
    private val recordedPayments = InMemoryRecordedPaymentRepository()
    private lateinit var settlements: FakeSettlementRepository
    private val deletions = InMemoryGroupDeletionRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Room not read yet WHEN the screen opens THEN nothing is loaded`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
        assertFalse(state.isEmpty)
        assertFalse(state.showsSplitBillButton)
    }

    @Test
    fun `GIVEN a group on the device WHEN the screen opens THEN shows its name initials color and currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals("Viagem de Floripa", state.header.groupName)
            assertEquals("VF", state.header.groupInitials)
            assertEquals(AvatarColor.LILAC, state.header.groupColor)
            assertEquals(GroupCurrency.USD, state.header.currency)
        }

    @Test
    fun `GIVEN a group with four members and two joined WHEN the screen opens THEN counts them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(4, state.header.memberCount)
            assertEquals(2, state.header.notJoinedCount)
        }

    @Test
    fun `GIVEN a group with four members WHEN the screen opens THEN shows all four faces and no extra count`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                listOf(
                    GroupFace("A", AvatarColor.PEACH),
                    GroupFace("P", AvatarColor.BLUE),
                    GroupFace("B", AvatarColor.GREEN),
                    GroupFace("C", AvatarColor.LILAC),
                ),
                state.header.faces,
            )
            assertEquals(0, state.header.extraMemberCount)
        }

    @Test
    fun `GIVEN a group with six members WHEN the screen opens THEN shows the first four faces and two more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(members = trip.members + diego + elisa))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("A", "P", "B", "C"), state.header.faces.map { it.initials })
            assertEquals(2, state.header.extraMemberCount)
            assertEquals(6, state.header.memberCount)
            assertEquals(4, state.header.notJoinedCount)
        }

    @Test
    fun `GIVEN a member whose name starts with a space WHEN the screen opens THEN the face shows the first letter`() =
        runTest(testDispatcher) {
            // Given
            val spaced = member("member-9", "  eva", AvatarColor.SAND)
            prepareScenario(storedGroup = trip.copy(members = listOf(spaced)))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(GroupFace("E", AvatarColor.SAND)), state.header.faces)
        }

    @Test
    fun `GIVEN a group without bills WHEN the screen opens THEN shows the empty state without the split button`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = emptyList())

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertTrue(state.isEmpty)
            assertFalse(state.showsSplitBillButton)
            assertTrue(state.days.isEmpty())
            assertEquals(0L, state.header.totalSpentMinor)
        }

    @Test
    fun `GIVEN a group with bills WHEN the screen opens THEN shows the split button and no empty state`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(expense("expense-1", spentAt = thisMorning)))

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isEmpty)
            assertTrue(state.showsSplitBillButton)
        }

    @Test
    fun `GIVEN bills in this group and another WHEN the screen opens THEN the total adds only this group bills`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "expense-1",
                        spentAt = thisMorning,
                        amountMinor = 64_200,
                    ),
                    expense(
                        "expense-2",
                        spentAt = yesterdayEvening,
                        amountMinor = 12_345,
                    ),
                    expense(
                        "expense-3",
                        spentAt = thisMorning,
                        amountMinor = 99_999,
                        groupId = "group-2",
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(76_545L, state.header.totalSpentMinor)
            assertEquals(listOf("expense-1", "expense-2"), state.days.flatMap { day -> day.entries.map { it.id } })
        }

    @Test
    fun `GIVEN bills today yesterday and a week ago WHEN the screen opens THEN groups them by day most recent first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense("week-ago", spentAt = weekAgo),
                    expense("today", spentAt = thisMorning),
                    expense("yesterday", spentAt = yesterdayEvening),
                ),
            )

            // When
            val days = viewModel.uiState.value.days

            // Then
            assertEquals(
                listOf(RelativeDay.TODAY, RelativeDay.YESTERDAY, RelativeDay.OTHER),
                days.map { it.relativeDay },
            )
            assertEquals(
                listOf(
                    Instant.parse("2026-10-30T00:00:00Z").toEpochMilliseconds(),
                    Instant.parse("2026-10-29T00:00:00Z").toEpochMilliseconds(),
                    Instant.parse("2026-10-23T00:00:00Z").toEpochMilliseconds(),
                ),
                days.map { it.dayStartMillis },
            )
            assertEquals(listOf(listOf("today"), listOf("yesterday"), listOf("week-ago")), days.map { it.ids() })
        }

    @Test
    fun `GIVEN two bills on the same day WHEN the screen opens THEN they share the day with the most recent first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense("breakfast", spentAt = thisMorning),
                    expense("lunch", spentAt = thisAfternoon),
                ),
            )

            // When
            val days = viewModel.uiState.value.days

            // Then
            assertEquals(1, days.size)
            assertEquals(listOf("lunch", "breakfast"), days.single().ids())
        }

    @Test
    fun `GIVEN a bill late at night in Sao Paulo WHEN the screen opens THEN it counts on its local day`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                timeZone = saoPaulo,
                storedExpenses = listOf(
                    expense("late-dinner", spentAt = lateEveningInSaoPaulo),
                    expense("breakfast", spentAt = thisMorningInSaoPaulo),
                ),
            )

            // When
            val days = viewModel.uiState.value.days

            // Then
            assertEquals(listOf(listOf("breakfast"), listOf("late-dinner")), days.map { it.ids() })
            assertEquals(listOf(RelativeDay.TODAY, RelativeDay.YESTERDAY), days.map { it.relativeDay })
            assertEquals(
                listOf(
                    Instant.parse("2026-10-30T03:00:00Z").toEpochMilliseconds(),
                    Instant.parse("2026-10-29T03:00:00Z").toEpochMilliseconds(),
                ),
                days.map { it.dayStartMillis },
            )
        }

    @Test
    fun `GIVEN a bill Bruno paid that this device user shares WHEN the screen opens THEN shows what is owed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "dinner",
                        description = "Jantar no Ostradamus",
                        spentAt = thisMorning,
                        amountMinor = 64_200,
                        payers = listOf(ExpensePayer(BRUNO, 64_200)),
                        shares = listOf(
                            share(ANA, 16_050),
                            share(PIERRE, 16_050),
                            share(BRUNO, 16_050),
                            share(CARLA, 16_050),
                        ),
                        isPending = true,
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.days
                .single()
                .entries
                .filterIsInstance<ExpenseRow>()
                .single()

            // Then
            assertEquals(
                ExpenseRow(
                    id = "dinner",
                    description = "Jantar no Ostradamus",
                    spentAt = thisMorning,
                    payers = listOf(
                        ExpenseRowPayer(
                            name = "Bruno",
                            isMe = false,
                        ),
                    ),
                    totalMinor = 64_200,
                    myBalanceMinor = -16_050,
                    isPending = true,
                ),
                row,
            )
        }

    @Test
    fun `GIVEN a bill this device user paid for everyone WHEN the screen opens THEN shows what the others owe`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "market",
                        spentAt = thisMorning,
                        amountMinor = 80_000,
                        payers = listOf(ExpensePayer(PIERRE, 80_000)),
                        shares = listOf(
                            share(ANA, 20_000),
                            share(PIERRE, 20_000),
                            share(BRUNO, 20_000),
                            share(CARLA, 20_000),
                        ),
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.days
                .single()
                .entries
                .filterIsInstance<ExpenseRow>()
                .single()

            // Then
            assertEquals(
                listOf(
                    ExpenseRowPayer(
                        name = "Pierre",
                        isMe = true,
                    ),
                ),
                row.payers,
            )
            assertEquals(60_000L, row.myBalanceMinor)
            assertFalse(row.isPending)
        }

    @Test
    fun `GIVEN a bill this device user paid without a share WHEN the screen opens THEN the balance is all paid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "gift",
                        spentAt = thisMorning,
                        amountMinor = 10_000,
                        payers = listOf(ExpensePayer(PIERRE, 10_000)),
                        shares = listOf(share(ANA, 5_000), share(BRUNO, 5_000)),
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.days
                .single()
                .entries
                .filterIsInstance<ExpenseRow>()
                .single()

            // Then
            assertTrue(row.payers.single().isMe)
            assertEquals(10_000L, row.myBalanceMinor)
        }

    @Test
    fun `GIVEN a bill this device user paid only their own share of WHEN the screen opens THEN the balance is zero`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "split-payment",
                        spentAt = thisMorning,
                        amountMinor = 10_000,
                        payers = listOf(ExpensePayer(ANA, 5_000), ExpensePayer(PIERRE, 5_000)),
                        shares = listOf(share(ANA, 5_000), share(PIERRE, 5_000)),
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.days
                .single()
                .entries
                .filterIsInstance<ExpenseRow>()
                .single()

            // Then
            assertEquals(0L, row.myBalanceMinor)
        }

    @Test
    fun `GIVEN a bill two people paid with this device user second WHEN the screen opens THEN lists them before Ana`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "hotel",
                        spentAt = thisMorning,
                        amountMinor = 100_000,
                        payers = listOf(ExpensePayer(ANA, 60_000), ExpensePayer(PIERRE, 40_000)),
                        shares = listOf(
                            share(ANA, 25_000),
                            share(PIERRE, 25_000),
                            share(BRUNO, 25_000),
                            share(CARLA, 25_000),
                        ),
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.days
                .single()
                .entries
                .filterIsInstance<ExpenseRow>()
                .single()

            // Then
            assertEquals(
                listOf(
                    ExpenseRowPayer(
                        name = "Pierre",
                        isMe = true,
                    ),
                    ExpenseRowPayer(
                        name = "Ana",
                        isMe = false,
                    ),
                ),
                row.payers,
            )
            assertEquals(15_000L, row.myBalanceMinor)
        }

    @Test
    fun `GIVEN Carla a former member and Bruno paid WHEN the screen opens THEN lists them in group order`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "boat",
                        spentAt = thisMorning,
                        amountMinor = 90_000,
                        payers = listOf(
                            ExpensePayer(CARLA, 30_000),
                            ExpensePayer(FORMER, 30_000),
                            ExpensePayer(BRUNO, 30_000),
                        ),
                        shares = listOf(
                            share(PIERRE, 45_000),
                            share(ANA, 45_000),
                        ),
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.days
                .single()
                .entries
                .filterIsInstance<ExpenseRow>()
                .single()

            // Then
            assertEquals(
                listOf(
                    ExpenseRowPayer(
                        name = "Bruno",
                        isMe = false,
                    ),
                    ExpenseRowPayer(
                        name = "Carla",
                        isMe = false,
                    ),
                    ExpenseRowPayer(
                        name = null,
                        isMe = false,
                    ),
                ),
                row.payers,
            )
        }

    @Test
    fun `GIVEN a bill this device user neither paid nor shares WHEN the screen opens THEN has no balance`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "taxi",
                        spentAt = thisMorning,
                        amountMinor = 4_000,
                        payers = listOf(ExpensePayer(ANA, 4_000)),
                        shares = listOf(share(ANA, 2_000), share(BRUNO, 2_000)),
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.days
                .single()
                .entries
                .filterIsInstance<ExpenseRow>()
                .single()

            // Then
            assertEquals(
                listOf(
                    ExpenseRowPayer(
                        name = "Ana",
                        isMe = false,
                    ),
                ),
                row.payers,
            )
            assertNull(row.myBalanceMinor)
        }

    @Test
    fun `GIVEN a bill paid by someone no longer in the group WHEN the screen opens THEN the row has no payer name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "drinks",
                        spentAt = thisMorning,
                        amountMinor = 6_000,
                        payers = listOf(ExpensePayer("member-gone", 6_000)),
                        shares = listOf(share(PIERRE, 3_000), share("member-gone", 3_000)),
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.days
                .single()
                .entries
                .filterIsInstance<ExpenseRow>()
                .single()

            // Then
            assertEquals(
                listOf(
                    ExpenseRowPayer(
                        name = null,
                        isMe = false,
                    ),
                ),
                row.payers,
            )
            assertEquals(-3_000L, row.myBalanceMinor)
        }

    @Test
    fun `GIVEN a group without this device user WHEN the screen opens THEN no row has a balance or is paid by me`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(members = listOf(ana, bruno)),
                storedExpenses = listOf(
                    expense(
                        id = "taxi",
                        spentAt = thisMorning,
                        amountMinor = 4_000,
                        payers = listOf(ExpensePayer(ANA, 4_000)),
                        shares = listOf(share(ANA, 2_000), share(BRUNO, 2_000)),
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.days
                .single()
                .entries
                .filterIsInstance<ExpenseRow>()
                .single()

            // Then
            assertFalse(row.payers.single().isMe)
            assertNull(row.myBalanceMinor)
        }

    @Test
    fun `GIVEN the screen open WHEN a bill is added THEN shows it on top and adds it to the total`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "breakfast",
                        spentAt = thisMorning,
                        amountMinor = 3_000,
                    ),
                ),
            )
            val lunch = expense(
                "lunch",
                spentAt = thisAfternoon,
                amountMinor = 5_000,
            )

            // When
            expenses.expenses.value = expenses.expenses.value + lunch

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf("lunch", "breakfast"), state.days.single().ids())
            assertEquals(8_000L, state.header.totalSpentMinor)
            assertTrue(state.showsSplitBillButton)
        }

    @Test
    fun `GIVEN an empty group open WHEN its first bill arrives THEN leaves the empty state`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = emptyList())
            val first = expense("first", spentAt = thisMorning)

            // When
            expenses.expenses.value = listOf(first)

            // Then
            assertFalse(viewModel.uiState.value.isEmpty)
            assertTrue(viewModel.uiState.value.showsSplitBillButton)
        }

    @Test
    fun `GIVEN the screen open WHEN a sync renames the group and adds someone THEN shows the change`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val synced = trip.copy(
                name = "Praia do Rosa",
                members = trip.members + diego,
            )

            // When
            groups.groups.value = listOf(synced)

            // Then
            val state = viewModel.uiState.value
            assertEquals("Praia do Rosa", state.header.groupName)
            assertEquals("PR", state.header.groupInitials)
            assertEquals(5, state.header.memberCount)
            assertEquals(3, state.header.notJoinedCount)
            assertEquals(1, state.header.extraMemberCount)
        }

    @Test
    fun `GIVEN the screen on top WHEN the group disappears THEN goes back`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val noGroups = emptyList<Group>()

        // When
        groups.groups.value = noGroups

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a native back already closed the screen WHEN the group disappears THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()
            navigator.navigate(Route.Profile)
            val noGroups = emptyList<Group>()

            // When
            groups.groups.value = noGroups

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Rachar conta opened on top WHEN the group disappears THEN leaves Rachar conta in place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupDetailUiEvent.OnSplitBillClicked)
            val noGroups = emptyList<Group>()

            // When
            groups.groups.value = noGroups

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Groups,
                    route,
                    Route.SplitBill(
                        GROUP_ID,
                        expenseId = null,
                        picksGroupFirst = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the group gone while Rachar conta was on top WHEN Rachar conta closes THEN goes back`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupDetailUiEvent.OnSplitBillClicked)
            groups.groups.value = emptyList()

            // When
            navigator.navigateBack()

            // Then
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the group gone and back again while covered WHEN the cover closes THEN stays on the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupDetailUiEvent.OnInviteClicked)
            groups.groups.value = emptyList()
            groups.groups.value = listOf(trip)

            // When
            navigator.navigateBack()

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the screen on top WHEN tapping back THEN returns to the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = GroupDetailUiEvent.OnBackClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen on top WHEN tapping back THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Invite(GROUP_ID, isNewGroup = false))

        // When
        viewModel.onEvent(GroupDetailUiEvent.OnBackClicked)

        // Then
        assertEquals(
            listOf(Route.Home, Route.Groups, route, Route.Invite(GROUP_ID, isNewGroup = false)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the screen on top WHEN tapping Rachar conta THEN opens it for the group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = GroupDetailUiEvent.OnSplitBillClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(
            listOf(
                Route.Home,
                Route.Groups,
                route,
                Route.SplitBill(
                    GROUP_ID,
                    expenseId = null,
                    picksGroupFirst = false,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Convidar already opened WHEN tapping Rachar conta THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupDetailUiEvent.OnInviteClicked)

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Groups, route, Route.Invite(GROUP_ID, isNewGroup = false)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the screen on top WHEN tapping Convidar THEN opens the invite of the group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = GroupDetailUiEvent.OnInviteClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(
            listOf(Route.Home, Route.Groups, route, Route.Invite(GROUP_ID, isNewGroup = false)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the screen on top WHEN tapping Estatisticas THEN opens the statistics of the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val event = GroupDetailUiEvent.OnStatsClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, route, Route.GroupStats(GROUP_ID)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Rachar conta already opened WHEN tapping Convidar THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupDetailUiEvent.OnSplitBillClicked)

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnInviteClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Groups,
                    route,
                    Route.SplitBill(
                        GROUP_ID,
                        expenseId = null,
                        picksGroupFirst = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the screen on top WHEN tapping Membros THEN the members tab takes its place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val event = GroupDetailUiEvent.OnMembersClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Groups, Route.Members(GROUP_ID)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Rachar conta already opened WHEN tapping Membros THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupDetailUiEvent.OnSplitBillClicked)

        // When
        viewModel.onEvent(GroupDetailUiEvent.OnMembersClicked)

        // Then
        assertEquals(
            listOf(
                Route.Home,
                Route.Groups,
                route,
                Route.SplitBill(
                    GROUP_ID,
                    expenseId = null,
                    picksGroupFirst = false,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the screen open WHEN tapping the balances tab THEN Saldos takes its place`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = GroupDetailUiEvent.OnBalancesTabClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups, Route.Balances(GROUP_ID)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a bill on the list WHEN tapping it THEN opens the bill`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(expense("dinner", spentAt = thisMorning)))
        val event = GroupDetailUiEvent.OnExpenseClicked("dinner")

        // When
        viewModel.onEvent(event)

        // Then
        assertTrue(actions.isEmpty())
        assertEquals(
            listOf(Route.Home, Route.Groups, route, Route.BillDetail(GROUP_ID, "dinner")),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN an open bill deleted elsewhere WHEN its screen closes THEN says it was deleted once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(expense("dinner", spentAt = thisMorning)))
            navigator.navigate(Route.BillDetail(GROUP_ID, "dinner"))
            deletedBills.postNotice(GROUP_ID)

            // When
            navigator.navigateBack()

            // Then
            assertTrue(viewModel.uiState.value.showsBillDeletedNotice)
            assertNull(deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN the deleted notice on screen WHEN it was shown THEN it hides`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        deletedBills.postNotice(GROUP_ID)

        // When
        viewModel.onEvent(GroupDetailUiEvent.OnBillDeletedNoticeShown)

        // Then
        assertFalse(viewModel.uiState.value.showsBillDeletedNotice)
    }

    @Test
    fun `GIVEN a bill of another group deleted elsewhere WHEN this group is on top THEN leaves the notice waiting`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            deletedBills.postNotice("group-2")

            // Then
            assertFalse(viewModel.uiState.value.showsBillDeletedNotice)
            assertEquals("group-2", deletedBills.observeNoticeGroupId().first())
        }

    @Test
    fun `GIVEN the screen on top WHEN tapping Editar grupo THEN opens the edit of the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val event = GroupDetailUiEvent.OnEditGroupClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, route, Route.EditGroup(GROUP_ID)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Rachar conta already opened WHEN tapping Editar grupo THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupDetailUiEvent.OnSplitBillClicked)

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnEditGroupClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.Groups,
                    route,
                    Route.SplitBill(
                        GROUP_ID,
                        expenseId = null,
                        picksGroupFirst = false,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN this person owes someone WHEN tapping Sair do grupo THEN says to settle first with Acertar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "bill-1",
                        spentAt = thisMorning,
                    ),
                ),
            )

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)

            // Then
            val dialog = viewModel.uiState.value.exit.dialog
            assertIs<GroupExitDialog.LeaveBlocked>(dialog)
            assertTrue(dialog.canSettle)
            assertEquals(listOf(DebtKind.YOU_OWE), dialog.debts.map { it.kind })
            assertEquals(listOf(6_000L), dialog.debts.map { it.amountMinor })
        }

    @Test
    fun `GIVEN this person is only owed WHEN tapping Sair do grupo THEN says to settle first with Ver saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "bill-1",
                        spentAt = thisMorning,
                        payers = listOf(ExpensePayer(PIERRE, 12_000)),
                    ),
                ),
            )

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)

            // Then
            val dialog = viewModel.uiState.value.exit.dialog
            assertIs<GroupExitDialog.LeaveBlocked>(dialog)
            assertFalse(dialog.canSettle)
            assertEquals(listOf(DebtKind.OWES_YOU), dialog.debts.map { it.kind })
        }

    @Test
    fun `GIVEN a settled balance WHEN tapping Sair do grupo THEN asks first counting the others`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)

            // Then
            assertEquals(
                GroupExitDialog.ConfirmLeave(
                    groupName = "Viagem de Floripa",
                    otherMemberCount = 3,
                    deletesGroup = false,
                ),
                viewModel.uiState.value.exit.dialog,
            )
        }

    @Test
    fun `GIVEN nobody else joined WHEN tapping Sair do grupo THEN says the group goes for good`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(members = listOf(pierre, bruno, carla)))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)

            // Then
            assertEquals(
                GroupExitDialog.ConfirmLeave(
                    groupName = "Viagem de Floripa",
                    otherMemberCount = 2,
                    deletesGroup = true,
                ),
                viewModel.uiState.value.exit.dialog,
            )
        }

    @Test
    fun `GIVEN the device offline WHEN tapping Sair do grupo THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isOnline = false)

        // When
        viewModel.onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)

        // Then
        assertFalse(viewModel.uiState.value.exit.isOnline)
        assertNull(viewModel.uiState.value.exit.dialog)
    }

    @Test
    fun `GIVEN Sair do grupo asked WHEN confirming THEN leaves and goes back to Grupos`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)

        // When
        viewModel.onEvent(GroupDetailUiEvent.OnGroupExitConfirmed)

        // Then
        assertEquals(listOf(GROUP_ID), groups.leftGroupIds)
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN the server unreachable WHEN confirming Sair THEN says so and stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario(exitError = IllegalStateException("offline"))
        viewModel.onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)

        // When
        viewModel.onEvent(GroupDetailUiEvent.OnGroupExitConfirmed)

        // Then
        assertEquals(listOf<GroupDetailUiAction>(GroupDetailUiAction.ShowLeaveError), actions)
        assertNull(viewModel.uiState.value.exit.dialog)
        assertFalse(viewModel.uiState.value.exit.isRunning)
        assertEquals(listOf(Route.Home, Route.Groups, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the server knows of a balance WHEN confirming Sair THEN says to settle first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(exitOutcome = GroupExitOutcome.HAS_BALANCE)
            viewModel.onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnGroupExitConfirmed)

            // Then
            assertIs<GroupExitDialog.LeaveBlocked>(viewModel.uiState.value.exit.dialog)
            assertFalse(viewModel.uiState.value.exit.isRunning)
            assertEquals(listOf(Route.Home, Route.Groups, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Acerte antes de sair open WHEN the sync brings the bill THEN lists it and offers Acertar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(exitOutcome = GroupExitOutcome.HAS_BALANCE)
            viewModel.onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)
            viewModel.onEvent(GroupDetailUiEvent.OnGroupExitConfirmed)

            // When
            expenses.expenses.value = listOf(
                expense(
                    id = "bill-1",
                    spentAt = nowMillis,
                ),
            )

            // Then
            val dialog = assertIs<GroupExitDialog.LeaveBlocked>(viewModel.uiState.value.exit.dialog)
            assertEquals(listOf(PIERRE to ANA), dialog.debts.map { it.fromMemberId to it.toMemberId })
            assertTrue(dialog.canSettle)
        }

    @Test
    fun `GIVEN the server still answering WHEN dismissing the dialog THEN it stays open`() = runTest(testDispatcher) {
        // Given
        prepareScenario(exitGate = CompletableDeferred())
        viewModel.onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)
        viewModel.onEvent(GroupDetailUiEvent.OnGroupExitConfirmed)

        // When
        viewModel.onEvent(GroupDetailUiEvent.OnGroupExitDismissed)

        // Then
        assertIs<GroupExitDialog.ConfirmLeave>(viewModel.uiState.value.exit.dialog)
        assertTrue(viewModel.uiState.value.exit.isRunning)
    }

    @Test
    fun `GIVEN Sair do grupo asked WHEN dismissing THEN the dialog closes and nobody leaves`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnGroupExitDismissed)

            // Then
            assertNull(viewModel.uiState.value.exit.dialog)
            assertTrue(groups.leftGroupIds.isEmpty())
        }

    @Test
    fun `GIVEN Acerte antes de sair open WHEN tapping Acertar THEN opens Registrar pagamento to the creditor`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "bill-1",
                        spentAt = thisMorning,
                    ),
                ),
            )
            viewModel.onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnGroupExitConfirmed)

            // Then
            assertNull(viewModel.uiState.value.exit.dialog)
            assertEquals(
                listOf(Route.Home, Route.Groups, route, Route.RegisterPayment(GROUP_ID, PIERRE, ANA)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN someone still owes WHEN tapping Apagar grupo THEN lists every pair still open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "bill-1",
                        spentAt = thisMorning,
                        shares = listOf(share(BRUNO, 6_000), share(CARLA, 6_000)),
                    ),
                ),
            )

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnDeleteGroupClicked)

            // Then
            val dialog = viewModel.uiState.value.exit.dialog
            assertIs<GroupExitDialog.DeleteBlocked>(dialog)
            assertEquals(listOf(DebtKind.BETWEEN_OTHERS, DebtKind.BETWEEN_OTHERS), dialog.debts.map { it.kind })
        }

    @Test
    fun `GIVEN the open balances dialog WHEN tapping Ver saldos THEN opens the Saldos tab in its place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "bill-1",
                        spentAt = thisMorning,
                    ),
                ),
            )
            viewModel.onEvent(GroupDetailUiEvent.OnDeleteGroupClicked)

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnGroupExitConfirmed)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, Route.Balances(GROUP_ID)), navigator.backStack.value)
            assertTrue(groups.deletedGroupIds.isEmpty())
        }

    @Test
    fun `GIVEN everyone settled WHEN tapping Apagar grupo THEN asks first counting bills and members`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "bill-1",
                        spentAt = thisMorning,
                        shares = listOf(share(ANA, 12_000)),
                    ),
                ),
            )

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnDeleteGroupClicked)

            // Then
            assertEquals(
                GroupExitDialog.ConfirmDelete(
                    groupName = "Viagem de Floripa",
                    billCount = 1,
                    memberCount = 4,
                ),
                viewModel.uiState.value.exit.dialog,
            )
        }

    @Test
    fun `GIVEN Apagar grupo asked WHEN confirming THEN deletes it and goes back to Grupos`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupDetailUiEvent.OnDeleteGroupClicked)

        // When
        viewModel.onEvent(GroupDetailUiEvent.OnGroupExitConfirmed)

        // Then
        assertEquals(listOf(GROUP_ID), groups.deletedGroupIds)
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the server unreachable WHEN confirming Apagar THEN says so`() = runTest(testDispatcher) {
        // Given
        prepareScenario(exitError = IllegalStateException("offline"))
        viewModel.onEvent(GroupDetailUiEvent.OnDeleteGroupClicked)

        // When
        viewModel.onEvent(GroupDetailUiEvent.OnGroupExitConfirmed)

        // Then
        assertEquals(listOf<GroupDetailUiAction>(GroupDetailUiAction.ShowDeleteError), actions)
        assertEquals(listOf(Route.Home, Route.Groups, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN someone else deleted the group WHEN a sync takes it off THEN says so and stays until asked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            deletions.post(
                GroupDeletion(
                    groupId = GROUP_ID,
                    groupName = "Viagem de Floripa",
                    deletedByName = "Ana",
                ),
            )

            // When
            groups.groups.value = emptyList()

            // Then
            assertEquals(
                GroupDeletedNotice(
                    groupName = "Viagem de Floripa",
                    deletedByName = "Ana",
                ),
                viewModel.uiState.value.exit.deletedNotice,
            )
            assertEquals(listOf(Route.Home, Route.Groups, route), navigator.backStack.value)
            assertTrue(deletions.observeDeletions().first().isEmpty())
        }

    @Test
    fun `GIVEN Grupo apagado shown WHEN tapping Voltar para Grupos THEN goes back to Grupos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            deletions.post(
                GroupDeletion(
                    groupId = GROUP_ID,
                    groupName = "Viagem de Floripa",
                    deletedByName = "Ana",
                ),
            )
            groups.groups.value = emptyList()

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnBackToGroupsClicked)

            // Then
            assertNull(viewModel.uiState.value.exit.deletedNotice)
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a bill open over a group someone else deleted WHEN the bill closes THEN Grupo apagado shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.BillDetail(GROUP_ID, "bill-1"))
            deletions.post(
                GroupDeletion(
                    groupId = GROUP_ID,
                    groupName = "Viagem de Floripa",
                    deletedByName = null,
                ),
            )
            groups.groups.value = emptyList()

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                GroupDeletedNotice(
                    groupName = "Viagem de Floripa",
                    deletedByName = null,
                ),
                viewModel.uiState.value.exit.deletedNotice,
            )
        }

    @Test
    fun `GIVEN a group nobody pinned WHEN the screen opens THEN the header is not pinned`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val header = viewModel.uiState.value.header

        // Then
        assertFalse(header.isPinned)
    }

    @Test
    fun `GIVEN the group pinned to Inicio WHEN the screen opens THEN the header is pinned`() = runTest(testDispatcher) {
        // Given
        prepareScenario(pinnedGroupIds = setOf(GROUP_ID))

        // When
        val header = viewModel.uiState.value.header

        // Then
        assertTrue(header.isPinned)
    }

    @Test
    fun `GIVEN only another group pinned WHEN the screen opens THEN the header is not pinned`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(pinnedGroupIds = setOf("group-2"))

            // When
            val header = viewModel.uiState.value.header

            // Then
            assertFalse(header.isPinned)
        }

    @Test
    fun `GIVEN the screen open WHEN a sync brings a pin made on another device THEN the header is pinned`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val pinnedElsewhere = setOf(GROUP_ID)

            // When
            pins.pinnedGroupIds.value = pinnedElsewhere

            // Then
            assertTrue(viewModel.uiState.value.header.isPinned)
        }

    @Test
    fun `GIVEN the group pinned WHEN a sync brings its unpin THEN the header is not pinned`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(pinnedGroupIds = setOf(GROUP_ID))
            val unpinnedElsewhere = emptySet<String>()

            // When
            pins.pinnedGroupIds.value = unpinnedElsewhere

            // Then
            assertFalse(viewModel.uiState.value.header.isPinned)
        }

    @Test
    fun `GIVEN a group not pinned WHEN tapping Fixar na Inicio THEN pins it now and the header shows it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val event = GroupDetailUiEvent.OnPinToggled

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Triple(GROUP_ID, true, nowMillis)), pins.pinUpdates)
            assertTrue(viewModel.uiState.value.header.isPinned)
        }

    @Test
    fun `GIVEN the group pinned WHEN tapping Desafixar da Inicio THEN unpins it now and the header shows it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(pinnedGroupIds = setOf(GROUP_ID, "group-2"))
            val event = GroupDetailUiEvent.OnPinToggled

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Triple(GROUP_ID, false, nowMillis)), pins.pinUpdates)
            assertEquals(setOf("group-2"), pins.pinnedGroupIds.value)
            assertFalse(viewModel.uiState.value.header.isPinned)
        }

    @Test
    fun `GIVEN the group just pinned WHEN tapping the item again THEN unpins it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(GroupDetailUiEvent.OnPinToggled)

        // When
        viewModel.onEvent(GroupDetailUiEvent.OnPinToggled)

        // Then
        assertEquals(listOf(true, false), pins.pinUpdates.map { it.second })
        assertFalse(viewModel.uiState.value.header.isPinned)
    }

    @Test
    fun `GIVEN the screen on top WHEN tapping Fixar na Inicio THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = GroupDetailUiEvent.OnPinToggled

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups, route), navigator.backStack.value)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN Room not read yet WHEN tapping Fixar na Inicio THEN nothing is pinned`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)
        val event = GroupDetailUiEvent.OnPinToggled

        // When
        viewModel.onEvent(event)

        // Then
        assertTrue(pins.pinUpdates.isEmpty())
        assertTrue(pins.pinnedGroupIds.value.isEmpty())
    }

    @Test
    fun `GIVEN the group gone while covered WHEN tapping Fixar na Inicio THEN nothing is pinned`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(GroupDetailUiEvent.OnInviteClicked)
            groups.groups.value = emptyList()

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnPinToggled)

            // Then
            assertTrue(pins.pinUpdates.isEmpty())
            assertTrue(pins.pinnedGroupIds.value.isEmpty())
        }

    @Test
    fun `GIVEN the device fails to save the pin WHEN tapping Fixar na Inicio THEN the header stays unpinned`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            pins.updatePinnedError = IllegalStateException("disk full")

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnPinToggled)

            // Then
            assertEquals(listOf(Triple(GROUP_ID, true, nowMillis)), pins.pinUpdates)
            assertFalse(viewModel.uiState.value.header.isPinned)
        }

    @Test
    fun `GIVEN the group opened from Inicio WHEN confirming Sair THEN goes back to Grupos above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(routesBelow = emptyList())
            viewModel.onEvent(GroupDetailUiEvent.OnLeaveGroupClicked)

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnGroupExitConfirmed)

            // Then
            assertEquals(listOf(GROUP_ID), groups.leftGroupIds)
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the group opened from Inicio WHEN confirming Apagar THEN goes back to Grupos above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(routesBelow = emptyList())
            viewModel.onEvent(GroupDetailUiEvent.OnDeleteGroupClicked)

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnGroupExitConfirmed)

            // Then
            assertEquals(listOf(GROUP_ID), groups.deletedGroupIds)
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Grupo apagado on a group opened from Inicio WHEN tapping Voltar para Grupos THEN opens Grupos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(routesBelow = emptyList())
            deletions.post(
                GroupDeletion(
                    groupId = GROUP_ID,
                    groupName = "Viagem de Floripa",
                    deletedByName = "Ana",
                ),
            )
            groups.groups.value = emptyList()

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnBackToGroupsClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
        }

    private fun ExpenseDay.ids(): List<String> = entries.map { it.id }

    private var route = Route.GroupDetail(GROUP_ID, joinedAs = null)

    private val nowMillis = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()
    private val thisMorning = Instant.parse("2026-10-30T09:00:00Z").toEpochMilliseconds()
    private val thisAfternoon = Instant.parse("2026-10-30T13:00:00Z").toEpochMilliseconds()
    private val yesterdayEvening = Instant.parse("2026-10-29T20:00:00Z").toEpochMilliseconds()
    private val weekAgo = Instant.parse("2026-10-23T15:00:00Z").toEpochMilliseconds()

    // Why: São Paulo, with no daylight saving since 2019, as a fixed offset: the JS target has no time-zone database.
    private val saoPaulo = UtcOffset(hours = -3).asTimeZone()

    // Why: 22:00 of the 29th in São Paulo, while UTC is already on the 30th.
    private val lateEveningInSaoPaulo = Instant.parse("2026-10-30T01:00:00Z").toEpochMilliseconds()

    // Why: 09:00 of the 30th in São Paulo, the same UTC day as the late dinner.
    private val thisMorningInSaoPaulo = Instant.parse("2026-10-30T12:00:00Z").toEpochMilliseconds()

    private val ana = member(ANA, "Ana", AvatarColor.PEACH, hasJoined = true)
    private val pierre = member(
        PIERRE,
        "Pierre",
        AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
    )
    private val bruno = member(BRUNO, "Bruno", AvatarColor.GREEN)
    private val carla = member(CARLA, "Carla", AvatarColor.LILAC)
    private val diego = member(DIEGO, "Diego", AvatarColor.SAND)
    private val elisa = member(ELISA, "Elisa", AvatarColor.PEACH)

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem de Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.LILAC,
        currency = GroupCurrency.USD,
        members = listOf(ana, pierre, bruno, carla),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    private fun member(
        id: String,
        name: String,
        color: AvatarColor,
        isMe: Boolean = false,
        hasJoined: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = color,
        isMe = isMe,
        hasJoined = hasJoined,
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

    private fun expense(
        id: String,
        spentAt: Long,
        amountMinor: Long = 12_000,
        description: String = "Conta $id",
        groupId: String = GROUP_ID,
        payers: List<ExpensePayer> = listOf(ExpensePayer(ANA, amountMinor)),
        shares: List<ExpenseShare> = listOf(share(ANA, amountMinor / 2), share(PIERRE, amountMinor / 2)),
        isPending: Boolean = false,
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = description,
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

    @Test
    fun `GIVEN the group was just joined as Bruno WHEN the screen opens THEN says Bruno joined`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinedAs = "Bruno")

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("Bruno", state.joinedAs)
        }

    @Test
    fun `GIVEN the screen opened from the groups list WHEN it opens THEN there is no joined notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinedAs = null)

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.joinedAs)
        }

    @Test
    fun `GIVEN just joined with a local only account WHEN the screen opens THEN asks to protect the access`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                joinedAs = "Bruno",
                accountStatus = AccountStatus.LocalOnly,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isAccountUnprotected)
            assertTrue(state.showsProtectAccess)
        }

    @Test
    fun `GIVEN just joined with an anonymous account WHEN the screen opens THEN asks to protect the access`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                joinedAs = "Bruno",
                accountStatus = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsProtectAccess)
        }

    @Test
    fun `GIVEN just joined with Apple WHEN the screen opens THEN does not ask to protect the access`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                joinedAs = "Bruno",
                accountStatus = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.APPLE),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isAccountUnprotected)
            assertFalse(state.showsProtectAccess)
        }

    @Test
    fun `GIVEN asked to protect the access WHEN Google gets connected THEN stops asking`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            joinedAs = "Bruno",
            accountStatus = AccountStatus.Anonymous(FakeAccountRepository.ANONYMOUS_USER_ID),
        )

        // When
        account.status = AccountStatus.Connected(FakeAccountRepository.CONNECTED_USER_ID, AccountProvider.GOOGLE)

        // Then
        assertFalse(viewModel.uiState.value.showsProtectAccess)
        assertEquals("Bruno", viewModel.uiState.value.joinedAs)
    }

    @Test
    fun `GIVEN opened from the groups list with Google WHEN Google is removed THEN does not ask to protect`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                joinedAs = null,
                accountStatus = AccountStatus.Connected(
                    FakeAccountRepository.CONNECTED_USER_ID,
                    AccountProvider.GOOGLE,
                ),
            )

            // When
            account.status = AccountStatus.Anonymous(FakeAccountRepository.CONNECTED_USER_ID)

            // Then
            assertFalse(viewModel.uiState.value.isAccountUnprotected)
            assertFalse(viewModel.uiState.value.showsProtectAccess)
        }

    @Test
    fun `GIVEN asked to protect the access WHEN tapping it THEN opens the profile over the expenses`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinedAs = "Bruno")

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnProtectAccessClicked)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.GroupDetail(GROUP_ID, joinedAs = "Bruno"), Route.Profile),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN another screen on top WHEN tapping protect the access THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(joinedAs = "Bruno")
            navigator.navigate(Route.Invite(GROUP_ID, isNewGroup = false))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnProtectAccessClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.GroupDetail(GROUP_ID, joinedAs = "Bruno"),
                    Route.Invite(GROUP_ID, isNewGroup = false),
                ),
                navigator.backStack.value,
            )
        }

    private fun TestScope.prepareScenario(
        storedGroup: Group = trip,
        storedExpenses: List<Expense> = emptyList(),
        isRoomRead: Boolean = true,
        timeZone: TimeZone = TimeZone.UTC,
        isOnline: Boolean = true,
        exitOutcome: GroupExitOutcome = GroupExitOutcome.DONE,
        exitError: Throwable? = null,
        exitGate: CompletableDeferred<Unit>? = null,
        pinnedGroupIds: Set<String> = emptySet(),
        routesBelow: List<Route> = listOf(Route.Groups),
        joinedAs: String? = null,
        accountStatus: AccountStatus = AccountStatus.LocalOnly,
    ) {
        route = Route.GroupDetail(GROUP_ID, joinedAs = joinedAs)
        account = FakeAccountRepository(initialStatus = accountStatus)
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup)).also {
            it.exitOutcome = exitOutcome
            it.exitError = exitError
            it.exitGate = exitGate
        }
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository()
        pins = FakeGroupPinRepository(initialPinnedGroupIds = pinnedGroupIds)
        navigator = BackStackNavigator(startRoute = Route.Home).also {
            routesBelow.forEach(it::navigate)
            it.navigate(route)
        }
        val groupNeverAnswers = MutableSharedFlow<Group?>()
        val expensesNeverAnswer = MutableSharedFlow<List<Expense>>()
        viewModel = GroupDetailViewModel(
            route = route,
            currentTimeMillis = { nowMillis },
            currentTimeZone = { timeZone },
            consumeDeletedBillNotice = ConsumeDeletedBillNoticeUseCase(deletedBills),
            observeDeletedBillNotice = ObserveDeletedBillNoticeUseCase(deletedBills),
            observeAccount = ObserveAccountUseCase(account),
            useCases = GroupScreenUseCases(
                navigator = navigator,
                observeGroup = if (isRoomRead) ObserveGroupUseCase(groups) else ObserveGroup { groupNeverAnswers },
                observeExpenses = if (isRoomRead) {
                    ObserveExpensesUseCase(expenses)
                } else {
                    ObserveExpenses { expensesNeverAnswer }
                },
                observeSettlements = ObserveSettlementsUseCase(settlements),
                observeRecordedPayment = ObserveRecordedPaymentUseCase(recordedPayments),
                consumeRecordedPayment = ConsumeRecordedPaymentUseCase(recordedPayments),
                deleteSettlement = DeleteSettlementUseCase(settlements, recordedPayments) { nowMillis },
                observeGroupDeletions = ObserveGroupDeletionsUseCase(deletions),
                consumeGroupDeletion = ConsumeGroupDeletionUseCase(deletions),
                connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(isOnline) },
                leaveGroup = LeaveGroup { groups.requestLeaveGroup(it) },
                deleteGroup = DeleteGroup { groups.deleteGroup(it) },
                observePinnedGroupIds = ObservePinnedGroupIdsUseCase(pins),
                setGroupPinned = SetGroupPinnedUseCase(pins) { nowMillis },
            ),
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
        const val CARLA = "member-4"
        const val DIEGO = "member-5"
        const val ELISA = "member-6"
        const val FORMER = "member-left"
    }
}
