package com.quare.blitzsplit.feature.groupdetail

import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityUnreadCount
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.DebtKind
import com.quare.blitzsplit.core.expenses.domain.model.DebtRow
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupFace
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeDeletedBillNoticeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveDeletedBillNoticeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettlementsUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RecordSettlementUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.data.repository.InMemoryGroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeGroupDeletionUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupDeletionsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObservePinnedGroupIdsUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.SetGroupPinnedUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupPinRepository
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup
import com.quare.blitzsplit.core.sync.domain.usecase.LeaveGroup
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupDetailViewModel
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupScreenUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class GroupDetailBalancesViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val activityUnread = MutableStateFlow(0)
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var pins: FakeGroupPinRepository
    private lateinit var recordSettlement: RecordSettlementUseCase
    private lateinit var viewModel: GroupDetailViewModel
    private val actions = mutableListOf<GroupDetailUiAction>()
    private val recordedPayments = InMemoryRecordedPaymentRepository()
    private val isRoomRead = MutableStateFlow(true)
    private val deletions = InMemoryGroupDeletionRepository()
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
    fun `GIVEN Room not read yet WHEN Saldos opens THEN nothing is drawn`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomReadAtStart = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
        assertFalse(state.isBalancesEmpty)
        assertFalse(state.showsSplitBillButton)
        assertFalse(state.showsDebtNote)
    }

    @Test
    fun `GIVEN a group without bills WHEN Saldos opens THEN says nobody owes anything yet`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoaded)
        assertTrue(state.isBalancesEmpty)
        assertFalse(state.hasBills)
        assertTrue(state.myDebts.isEmpty())
        assertTrue(state.otherDebts.isEmpty())
        assertFalse(state.showsDebtNote)
        assertTrue(state.showsSplitBillButton)
        assertNull(state.header.myBalance)
        assertFalse(state.header.isEveryoneSettled)
    }

    @Test
    fun `GIVEN this device user owes Bruno WHEN Saldos opens THEN shows the pair with you owe`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill(
                        "hotel",
                        paidBy = BRUNO,
                        amountMinor = 6_000,
                        PIERRE,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                listOf(
                    DebtRow(
                        fromMemberId = PIERRE,
                        toMemberId = BRUNO,
                        fromName = "Pierre",
                        fromFace = GroupFace("P", AvatarColor.BLUE),
                        toName = "Bruno",
                        toFace = GroupFace("B", AvatarColor.GREEN),
                        amountMinor = 3_000,
                        kind = DebtKind.YOU_OWE,
                    ),
                ),
                state.myDebts,
            )
            assertTrue(state.otherDebts.isEmpty())
            assertTrue(state.showsDebtNote)
            assertFalse(state.isBalancesEmpty)
        }

    @Test
    fun `GIVEN Ana owes this device user WHEN Saldos opens THEN shows the pair with owes you`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill(
                        "market",
                        paidBy = PIERRE,
                        amountMinor = 10_000,
                        ANA,
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.myDebts
                .single()

            // Then
            assertEquals(ANA, row.fromMemberId)
            assertEquals(PIERRE, row.toMemberId)
            assertEquals("Ana", row.fromName)
            assertEquals("Pierre", row.toName)
            assertEquals(5_000L, row.amountMinor)
            assertEquals(DebtKind.OWES_YOU, row.kind)
        }

    @Test
    fun `GIVEN debts to and from this device user WHEN Saldos opens THEN what I owe comes before what I am owed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill(
                        "hotel",
                        paidBy = BRUNO,
                        amountMinor = 6_000,
                        PIERRE,
                    ),
                    bill(
                        "flight",
                        paidBy = CARLA,
                        amountMinor = 18_000,
                        PIERRE,
                    ),
                    bill(
                        "market",
                        paidBy = PIERRE,
                        amountMinor = 10_000,
                        ANA,
                    ),
                ),
            )

            // When
            val myDebts = viewModel.uiState.value.myDebts

            // Then
            assertEquals(
                listOf(
                    Triple(PIERRE, CARLA, DebtKind.YOU_OWE),
                    Triple(PIERRE, BRUNO, DebtKind.YOU_OWE),
                    Triple(ANA, PIERRE, DebtKind.OWES_YOU),
                ),
                myDebts.map { Triple(it.fromMemberId, it.toMemberId, it.kind) },
            )
            assertEquals(listOf(9_000L, 3_000L, 5_000L), myDebts.map { it.amountMinor })
        }

    @Test
    fun `GIVEN two people owe this device user WHEN Saldos opens THEN the largest comes first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill(
                        "market",
                        paidBy = PIERRE,
                        amountMinor = 4_000,
                        ANA,
                    ),
                    bill(
                        "hotel",
                        paidBy = PIERRE,
                        amountMinor = 20_000,
                        BRUNO,
                    ),
                ),
            )

            // When
            val myDebts = viewModel.uiState.value.myDebts

            // Then
            assertEquals(listOf(BRUNO, ANA), myDebts.map { it.fromMemberId })
            assertEquals(listOf(10_000L, 2_000L), myDebts.map { it.amountMinor })
        }

    @Test
    fun `GIVEN debts between other members WHEN Saldos opens THEN lists them between the others largest first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill(
                        "taxi",
                        paidBy = ANA,
                        amountMinor = 8_000,
                        BRUNO,
                    ),
                    bill(
                        "hotel",
                        paidBy = CARLA,
                        amountMinor = 20_000,
                        ANA,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.myDebts.isEmpty())
            assertEquals(
                listOf(
                    DebtRow(
                        fromMemberId = ANA,
                        toMemberId = CARLA,
                        fromName = "Ana",
                        fromFace = GroupFace("A", AvatarColor.PEACH),
                        toName = "Carla",
                        toFace = GroupFace("C", AvatarColor.LILAC),
                        amountMinor = 10_000,
                        kind = DebtKind.BETWEEN_OTHERS,
                    ),
                    DebtRow(
                        fromMemberId = BRUNO,
                        toMemberId = ANA,
                        fromName = "Bruno",
                        fromFace = GroupFace("B", AvatarColor.GREEN),
                        toName = "Ana",
                        toFace = GroupFace("A", AvatarColor.PEACH),
                        amountMinor = 4_000,
                        kind = DebtKind.BETWEEN_OTHERS,
                    ),
                ),
                state.otherDebts,
            )
            assertTrue(state.showsDebtNote)
        }

    @Test
    fun `GIVEN debts with and without this device user WHEN Saldos opens THEN splits them into the two lists`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill(
                        "hotel",
                        paidBy = BRUNO,
                        amountMinor = 6_000,
                        PIERRE,
                    ),
                    bill(
                        "taxi",
                        paidBy = ANA,
                        amountMinor = 8_000,
                        CARLA,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(PIERRE to BRUNO), state.myDebts.map { it.fromMemberId to it.toMemberId })
            assertEquals(listOf(CARLA to ANA), state.otherDebts.map { it.fromMemberId to it.toMemberId })
        }

    @Test
    fun `GIVEN a debt to someone no longer in the group WHEN Saldos opens THEN the row has no name or face`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill(
                        "drinks",
                        paidBy = "member-gone",
                        amountMinor = 6_000,
                        ANA,
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.otherDebts
                .single()

            // Then
            assertEquals(ANA, row.fromMemberId)
            assertEquals("member-gone", row.toMemberId)
            assertEquals("Ana", row.fromName)
            assertEquals(GroupFace("A", AvatarColor.PEACH), row.fromFace)
            assertNull(row.toName)
            assertNull(row.toFace)
        }

    @Test
    fun `GIVEN a debt from someone no longer in the group to me WHEN Saldos opens THEN it owes me without a name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill(
                        "drinks",
                        paidBy = PIERRE,
                        amountMinor = 6_000,
                        "member-gone",
                    ),
                ),
            )

            // When
            val row = viewModel.uiState.value.myDebts
                .single()

            // Then
            assertNull(row.fromName)
            assertNull(row.fromFace)
            assertEquals(DebtKind.OWES_YOU, row.kind)
        }

    @Test
    fun `GIVEN a group without this device user WHEN Saldos opens THEN every debt is between the others`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(members = listOf(ana, bruno, carla)),
                storedExpenses = listOf(
                    bill(
                        "taxi",
                        paidBy = ANA,
                        amountMinor = 8_000,
                        BRUNO,
                    ),
                    bill(
                        "hotel",
                        paidBy = CARLA,
                        amountMinor = 4_000,
                        ANA,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.myDebts.isEmpty())
            assertEquals(2, state.otherDebts.size)
            assertTrue(state.otherDebts.all { it.kind == DebtKind.BETWEEN_OTHERS })
            assertNull(state.header.myBalance)
        }

    @Test
    fun `GIVEN every bill already paid back WHEN Saldos opens THEN everyone settled and no empty state`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill(
                        "hotel",
                        paidBy = BRUNO,
                        amountMinor = 6_000,
                        PIERRE,
                    ),
                ),
                storedSettlements = listOf(
                    settlement(
                        id = "pay-1",
                        from = PIERRE,
                        to = BRUNO,
                        amountMinor = 3_000,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.hasBills)
            assertFalse(state.isBalancesEmpty)
            assertTrue(state.myDebts.isEmpty())
            assertTrue(state.otherDebts.isEmpty())
            assertFalse(state.showsDebtNote)
            assertTrue(state.header.isEveryoneSettled)
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 0,
                ),
                state.header.myBalance,
            )
        }

    @Test
    fun `GIVEN only a payment and no bill WHEN Saldos opens THEN shows the debt it makes and not the empty state`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedSettlements = listOf(
                    settlement(
                        id = "pay-1",
                        from = PIERRE,
                        to = ANA,
                        amountMinor = 2_000,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.hasBills)
            assertFalse(state.isBalancesEmpty)
            assertEquals(listOf(ANA to PIERRE), state.myDebts.map { it.fromMemberId to it.toMemberId })
            assertEquals(
                MemberBalance(
                    receivesMinor = 2_000,
                    owesMinor = 0,
                ),
                state.header.myBalance,
            )
            assertFalse(state.header.isEveryoneSettled)
        }

    @Test
    fun `GIVEN Saldos open WHEN a bill syncs in THEN the new pair shows`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedExpenses = listOf(
                bill(
                    "hotel",
                    paidBy = BRUNO,
                    amountMinor = 6_000,
                    PIERRE,
                ),
            ),
        )
        val taxi = bill(
            "taxi",
            paidBy = ANA,
            amountMinor = 8_000,
            CARLA,
        )

        // When
        expenses.expenses.value = expenses.expenses.value + taxi

        // Then
        val state = viewModel.uiState.value
        assertEquals(listOf(CARLA to ANA), state.otherDebts.map { it.fromMemberId to it.toMemberId })
        assertEquals(14_000L, state.header.totalSpentMinor)
    }

    @Test
    fun `GIVEN Saldos open WHEN a payment syncs in THEN the pair it settles goes away`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedExpenses = listOf(
                bill(
                    "hotel",
                    paidBy = BRUNO,
                    amountMinor = 6_000,
                    PIERRE,
                ),
                bill(
                    "flight",
                    paidBy = CARLA,
                    amountMinor = 18_000,
                    PIERRE,
                ),
            ),
        )
        val payment = settlement(
            id = "pay-1",
            from = PIERRE,
            to = CARLA,
            amountMinor = 9_000,
        )

        // When
        settlements.settlements.value = listOf(payment)

        // Then
        val state = viewModel.uiState.value
        assertEquals(listOf(PIERRE to BRUNO), state.myDebts.map { it.fromMemberId to it.toMemberId })
        assertEquals(
            MemberBalance(
                receivesMinor = 0,
                owesMinor = 3_000,
            ),
            state.header.myBalance,
        )
    }

    @Test
    fun `GIVEN Saldos on top WHEN tapping a pair THEN opens Registrar pagamento for it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedExpenses = listOf(
                bill(
                    "taxi",
                    paidBy = ANA,
                    amountMinor = 8_000,
                    CARLA,
                ),
            ),
        )
        val event = GroupDetailUiEvent.OnDebtClicked(
            fromMemberId = CARLA,
            toMemberId = ANA,
        )

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(
            listOf(Route.Home, Route.Groups, route, Route.RegisterPayment(GROUP_ID, CARLA, ANA)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN another screen on top WHEN tapping a pair THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedExpenses = listOf(
                bill(
                    "taxi",
                    paidBy = ANA,
                    amountMinor = 8_000,
                    CARLA,
                ),
            ),
        )
        navigator.navigate(Route.Invite(GROUP_ID, isNewGroup = false))

        // When
        viewModel.onEvent(
            GroupDetailUiEvent.OnDebtClicked(
                fromMemberId = CARLA,
                toMemberId = ANA,
            ),
        )

        // Then
        assertEquals(
            listOf(Route.Home, Route.Groups, route, Route.Invite(GROUP_ID, isNewGroup = false)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN this device user owes Bruno and more to Carla WHEN tapping Acertar THEN opens the payment to Carla`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill(
                        "hotel",
                        paidBy = BRUNO,
                        amountMinor = 6_000,
                        PIERRE,
                    ),
                    bill(
                        "flight",
                        paidBy = CARLA,
                        amountMinor = 18_000,
                        PIERRE,
                    ),
                ),
            )
            val event = GroupDetailUiEvent.OnSettleClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Groups, route, Route.RegisterPayment(GROUP_ID, PIERRE, CARLA)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN this device user owes nobody WHEN tapping Acertar THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill(
                        "market",
                        paidBy = PIERRE,
                        amountMinor = 10_000,
                        ANA,
                    ),
                ),
            )
            val event = GroupDetailUiEvent.OnSettleClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a group without this device user WHEN tapping Acertar THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(members = listOf(ana, bruno)),
                storedExpenses = listOf(
                    bill(
                        "taxi",
                        paidBy = ANA,
                        amountMinor = 8_000,
                        BRUNO,
                    ),
                ),
            )
            val event = GroupDetailUiEvent.OnSettleClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Route.Home, Route.Groups, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN an open balance dialog on Saldos WHEN tapping Ver saldos THEN the dialog closes over Saldos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill(
                        id = "bill-1",
                        paidBy = ANA,
                        amountMinor = 10_000,
                        sharedWith = BRUNO,
                    ),
                ),
            )
            viewModel.onEvent(GroupDetailUiEvent.OnDeleteGroupClicked)

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnGroupExitConfirmed)

            // Then
            assertNull(viewModel.uiState.value.exit.dialog)
            assertEquals(listOf(Route.Home, Route.Groups, route), navigator.backStack.value)
            assertTrue(groups.deletedGroupIds.isEmpty())
        }

    private suspend fun record(
        from: String,
        to: String,
        groupId: String = GROUP_ID,
        amountMinor: Long = 5_000,
    ): String = recordSettlement(
        groupId = groupId,
        fromMemberId = from,
        toMemberId = to,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = paidAt,
    ).getOrThrow()

    private val route = Route.GroupDetail(GROUP_ID, joinedAs = null, GroupTab.BALANCES)

    private val nowMillis = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()
    private val paidAt = Instant.parse("2026-10-30T13:00:00Z").toEpochMilliseconds()

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

    // Why: one payer, split in equal halves with the other member, so the other owes the payer half the bill.
    private fun bill(
        id: String,
        paidBy: String,
        amountMinor: Long,
        sharedWith: String,
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = "Conta $id",
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(paidBy, amountMinor)),
        shares = listOf(
            ExpenseShare(
                memberId = paidBy,
                amountMinor = amountMinor / 2,
                weight = 1,
            ),
            ExpenseShare(
                memberId = sharedWith,
                amountMinor = amountMinor / 2,
                weight = 1,
            ),
        ),
        items = null,
        spentAt = paidAt,
        isPending = false,
        updatedAt = paidAt,
        updatedByMemberId = null,
    )

    private fun settlement(
        id: String,
        from: String,
        to: String,
        amountMinor: Long = 5_000,
    ): Settlement = Settlement(
        id = id,
        groupId = GROUP_ID,
        fromMemberId = from,
        toMemberId = to,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = paidAt,
        isPending = false,
        updatedAt = 0L,
    )

    private fun TestScope.prepareScenario(
        storedGroup: Group = trip,
        storedExpenses: List<Expense> = emptyList(),
        storedSettlements: List<Settlement> = emptyList(),
        isRoomReadAtStart: Boolean = true,
        exitError: Throwable? = null,
        pinnedGroupIds: Set<String> = emptySet(),
        routesBelow: List<Route> = listOf(Route.Groups),
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup)).also { it.exitError = exitError }
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        pins = FakeGroupPinRepository(initialPinnedGroupIds = pinnedGroupIds)
        navigator = BackStackNavigator(startRoute = Route.Home).also {
            routesBelow.forEach(it::navigate)
            it.navigate(route)
        }
        isRoomRead.value = isRoomReadAtStart
        recordSettlement = RecordSettlementUseCase(settlements, recordedPayments) { nowMillis }
        val observeStoredGroup = ObserveGroupUseCase(groups)
        viewModel = GroupDetailViewModel(
            route = route,
            currentTimeMillis = { nowMillis },
            currentTimeZone = { TimeZone.UTC },
            consumeDeletedBillNotice = ConsumeDeletedBillNoticeUseCase(deletedBills),
            observeDeletedBillNotice = ObserveDeletedBillNoticeUseCase(deletedBills),
            observeAccount = ObserveAccountUseCase(FakeAccountRepository(initialStatus = AccountStatus.LocalOnly)),
            useCases = GroupScreenUseCases(
                navigator = navigator,
                observeGroup = ObserveGroup { groupId ->
                    isRoomRead.flatMapLatest { isRead -> if (isRead) observeStoredGroup(groupId) else emptyFlow() }
                },
                observeExpenses = ObserveExpensesUseCase(expenses),
                observeSettlements = ObserveSettlementsUseCase(settlements),
                observeRecordedPayment = ObserveRecordedPaymentUseCase(recordedPayments),
                consumeRecordedPayment = ConsumeRecordedPaymentUseCase(recordedPayments),
                deleteSettlement = DeleteSettlementUseCase(settlements, recordedPayments) { nowMillis },
                observeGroupDeletions = ObserveGroupDeletionsUseCase(deletions),
                consumeGroupDeletion = ConsumeGroupDeletionUseCase(deletions),
                connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
                leaveGroup = LeaveGroup { groups.requestLeaveGroup(it) },
                deleteGroup = DeleteGroup { groups.deleteGroup(it) },
                observePinnedGroupIds = ObservePinnedGroupIdsUseCase(pins),
                setGroupPinned = SetGroupPinnedUseCase(pins) { nowMillis },
                observeActivityUnreadCount = ObserveActivityUnreadCount { activityUnread },
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
    }
}
