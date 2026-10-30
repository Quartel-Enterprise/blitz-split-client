package com.quare.blitzsplit.feature.groupdetail

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeDeletedBillNoticeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveDeletedBillNoticeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupFace
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class GroupDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: GroupDetailViewModel
    private val actions = mutableListOf<GroupDetailUiAction>()
    private val deletedBills = InMemoryDeletedBillRepository()

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
            assertEquals("Viagem de Floripa", state.groupName)
            assertEquals("VF", state.groupInitials)
            assertEquals(AvatarColor.LILAC, state.groupColor)
            assertEquals(GroupCurrency.USD, state.currency)
        }

    @Test
    fun `GIVEN a group with four members and two joined WHEN the screen opens THEN counts them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(4, state.memberCount)
            assertEquals(2, state.notJoinedCount)
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
                state.faces,
            )
            assertEquals(0, state.extraMemberCount)
        }

    @Test
    fun `GIVEN a group with six members WHEN the screen opens THEN shows the first four faces and two more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(members = trip.members + diego + elisa))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("A", "P", "B", "C"), state.faces.map { it.initials })
            assertEquals(2, state.extraMemberCount)
            assertEquals(6, state.memberCount)
            assertEquals(4, state.notJoinedCount)
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
            assertEquals(listOf(GroupFace("E", AvatarColor.SAND)), state.faces)
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
            assertEquals(0L, state.totalSpentMinor)
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
            assertEquals(76_545L, state.totalSpentMinor)
            assertEquals(listOf("expense-1", "expense-2"), state.days.flatMap { day -> day.expenses.map { it.id } })
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
                .expenses
                .single()

            // Then
            assertEquals(
                ExpenseRow(
                    id = "dinner",
                    description = "Jantar no Ostradamus",
                    spentAt = thisMorning,
                    payerName = "Bruno",
                    payerIsMe = false,
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
                .expenses
                .single()

            // Then
            assertEquals("Pierre", row.payerName)
            assertTrue(row.payerIsMe)
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
                .expenses
                .single()

            // Then
            assertTrue(row.payerIsMe)
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
                .expenses
                .single()

            // Then
            assertEquals(0L, row.myBalanceMinor)
        }

    @Test
    fun `GIVEN a bill two people paid with this device user second WHEN the screen opens THEN names the first payer`() =
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
                .expenses
                .single()

            // Then
            assertEquals("Ana", row.payerName)
            assertFalse(row.payerIsMe)
            assertEquals(15_000L, row.myBalanceMinor)
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
                .expenses
                .single()

            // Then
            assertEquals("Ana", row.payerName)
            assertFalse(row.payerIsMe)
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
                .expenses
                .single()

            // Then
            assertNull(row.payerName)
            assertFalse(row.payerIsMe)
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
                .expenses
                .single()

            // Then
            assertFalse(row.payerIsMe)
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
            assertEquals(8_000L, state.totalSpentMinor)
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
            assertEquals("Praia do Rosa", state.groupName)
            assertEquals("PR", state.groupInitials)
            assertEquals(5, state.memberCount)
            assertEquals(3, state.notJoinedCount)
            assertEquals(1, state.extraMemberCount)
        }

    @Test
    fun `GIVEN the screen on top WHEN the group disappears THEN goes back`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val noGroups = emptyList<Group>()

        // When
        groups.groups.value = noGroups

        // Then
        assertEquals(listOf<Route>(Route.Groups), navigator.backStack.value)
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
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
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
                listOf(Route.Groups, route, Route.SplitBill(GROUP_ID, expenseId = null)),
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
            assertEquals(listOf<Route>(Route.Groups), navigator.backStack.value)
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
            assertEquals(listOf(Route.Groups, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the screen on top WHEN tapping back THEN returns to the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = GroupDetailUiEvent.OnBackClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf<Route>(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen on top WHEN tapping back THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Invite(GROUP_ID))

        // When
        viewModel.onEvent(GroupDetailUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Groups, route, Route.Invite(GROUP_ID)), navigator.backStack.value)
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
            listOf(Route.Groups, route, Route.SplitBill(GROUP_ID, expenseId = null)),
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
            assertEquals(listOf(Route.Groups, route, Route.Invite(GROUP_ID)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the screen on top WHEN tapping Convidar THEN opens the invite of the group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = GroupDetailUiEvent.OnInviteClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf(Route.Groups, route, Route.Invite(GROUP_ID)), navigator.backStack.value)
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
                listOf(Route.Groups, route, Route.SplitBill(GROUP_ID, expenseId = null)),
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
                listOf(Route.Groups, Route.Members(GROUP_ID, joinedAs = null)),
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
            listOf(Route.Groups, route, Route.SplitBill(GROUP_ID, expenseId = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the screen open WHEN tapping the balances tab THEN says coming soon`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = GroupDetailUiEvent.OnBalancesTabClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf<GroupDetailUiAction>(GroupDetailUiAction.ShowComingSoon), actions)
        assertEquals(listOf(Route.Groups, route), navigator.backStack.value)
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
        assertEquals(listOf(Route.Groups, route, Route.BillDetail(GROUP_ID, "dinner")), navigator.backStack.value)
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
            assertEquals(listOf(Route.Groups, route, Route.EditGroup(GROUP_ID)), navigator.backStack.value)
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
                listOf(Route.Groups, route, Route.SplitBill(GROUP_ID, expenseId = null)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the screen open WHEN tapping Sair do grupo THEN says coming soon`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = GroupDetailUiEvent.OnLeaveGroupClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf<GroupDetailUiAction>(GroupDetailUiAction.ShowComingSoon), actions)
        assertEquals(listOf(Route.Groups, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the screen open WHEN tapping Apagar grupo THEN says coming soon`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = GroupDetailUiEvent.OnDeleteGroupClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf<GroupDetailUiAction>(GroupDetailUiAction.ShowComingSoon), actions)
        assertEquals(listOf(Route.Groups, route), navigator.backStack.value)
    }

    private fun ExpenseDay.ids(): List<String> = expenses.map { it.id }

    private val route = Route.GroupDetail(GROUP_ID)

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
        spentAt = spentAt,
        isPending = isPending,
        updatedAt = spentAt,
        updatedByMemberId = null,
    )

    private fun TestScope.prepareScenario(
        storedGroup: Group = trip,
        storedExpenses: List<Expense> = emptyList(),
        isRoomRead: Boolean = true,
        timeZone: TimeZone = TimeZone.UTC,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup))
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        navigator = BackStackNavigator(startRoute = Route.Groups).also { it.navigate(route) }
        val groupNeverAnswers = MutableSharedFlow<Group?>()
        val expensesNeverAnswer = MutableSharedFlow<List<Expense>>()
        viewModel = GroupDetailViewModel(
            route = route,
            navigator = navigator,
            currentTimeMillis = { nowMillis },
            currentTimeZone = { timeZone },
            consumeDeletedBillNotice = ConsumeDeletedBillNoticeUseCase(deletedBills),
            observeGroup = if (isRoomRead) ObserveGroupUseCase(groups) else ObserveGroup { groupNeverAnswers },
            observeExpenses = if (isRoomRead) {
                ObserveExpensesUseCase(expenses)
            } else {
                ObserveExpenses { expensesNeverAnswer }
            },
            observeDeletedBillNotice = ObserveDeletedBillNoticeUseCase(deletedBills),
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
    }
}
