package com.quare.blitzsplit.feature.groupdetail

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteSettlementUseCase
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
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup
import com.quare.blitzsplit.core.sync.domain.usecase.LeaveGroup
import com.quare.blitzsplit.feature.groupdetail.presentation.model.BalancesUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.BalancesUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.DebtKind
import com.quare.blitzsplit.feature.groupdetail.presentation.model.DebtRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupFace
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.BalancesViewModel
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupScreenUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class BalancesViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var recordSettlement: RecordSettlementUseCase
    private lateinit var viewModel: BalancesViewModel
    private val actions = mutableListOf<BalancesUiAction>()
    private val recordedPayments = InMemoryRecordedPaymentRepository()
    private val isRoomRead = MutableStateFlow(true)
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
    fun `GIVEN Room not read yet WHEN Saldos opens THEN nothing is drawn`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomReadAtStart = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
        assertFalse(state.isEmpty)
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
        assertTrue(state.isEmpty)
        assertFalse(state.hasBills)
        assertTrue(state.myDebts.isEmpty())
        assertTrue(state.otherDebts.isEmpty())
        assertFalse(state.showsDebtNote)
        assertTrue(state.showsSplitBillButton)
        assertNull(state.header.myBalance)
        assertFalse(state.header.isEveryoneSettled)
    }

    @Test
    fun `GIVEN a group with a bill WHEN Saldos opens THEN shows the same header as Despesas`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(members = trip.members + diego),
                storedExpenses = listOf(
                    bill(
                        "dinner",
                        paidBy = ANA,
                        amountMinor = 12_000,
                        PIERRE,
                    ),
                ),
                storedSettlements = listOf(
                    settlement(
                        id = "pay-1",
                        from = PIERRE,
                        to = ANA,
                        amountMinor = 1_000,
                    ),
                ),
            )

            // When
            val header = viewModel.uiState.value.header

            // Then
            assertEquals("Viagem de Floripa", header.groupName)
            assertEquals("VF", header.groupInitials)
            assertEquals(AvatarColor.LILAC, header.groupColor)
            assertEquals(GroupCurrency.USD, header.currency)
            assertEquals(listOf("A", "P", "B", "C"), header.faces.map { it.initials })
            assertEquals(5, header.memberCount)
            assertEquals(1, header.extraMemberCount)
            assertEquals(3, header.notJoinedCount)
            assertEquals(12_000L, header.totalSpentMinor)
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 5_000,
                ),
                header.myBalance,
            )
            assertFalse(header.isEveryoneSettled)
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
            assertFalse(state.isEmpty)
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
            assertFalse(state.isEmpty)
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
            assertFalse(state.isEmpty)
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
        val event = BalancesUiEvent.OnDebtClicked(
            fromMemberId = CARLA,
            toMemberId = ANA,
        )

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(
            listOf(Route.Groups, route, Route.RegisterPayment(GROUP_ID, CARLA, ANA)),
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
            BalancesUiEvent.OnDebtClicked(
                fromMemberId = CARLA,
                toMemberId = ANA,
            ),
        )

        // Then
        assertEquals(listOf(Route.Groups, route, Route.Invite(GROUP_ID, isNewGroup = false)), navigator.backStack.value)
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
            val event = BalancesUiEvent.OnSettleClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Groups, route, Route.RegisterPayment(GROUP_ID, PIERRE, CARLA)),
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
            val event = BalancesUiEvent.OnSettleClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Route.Groups, route), navigator.backStack.value)
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
            val event = BalancesUiEvent.OnSettleClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Route.Groups, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Saldos on top WHEN tapping the expenses tab THEN Despesas takes its place`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = BalancesUiEvent.OnExpensesTabClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(GROUP_ID)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Saldos on top WHEN tapping Membros THEN the members tab takes its place`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = BalancesUiEvent.OnMembersClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf(Route.Groups, Route.Members(GROUP_ID, joinedAs = null)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen on top WHEN tapping the expenses tab THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.EditGroup(GROUP_ID))

            // When
            viewModel.onEvent(BalancesUiEvent.OnExpensesTabClicked)

            // Then
            assertEquals(listOf(Route.Groups, route, Route.EditGroup(GROUP_ID)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Saldos on top WHEN tapping Convidar THEN opens the invite over it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = BalancesUiEvent.OnInviteClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf(Route.Groups, route, Route.Invite(GROUP_ID, isNewGroup = false)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Saldos on top WHEN tapping Editar grupo THEN opens the edit over it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = BalancesUiEvent.OnEditGroupClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf(Route.Groups, route, Route.EditGroup(GROUP_ID)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Saldos on top WHEN tapping Rachar conta THEN opens it for the group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = BalancesUiEvent.OnSplitBillClicked

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
            viewModel.onEvent(BalancesUiEvent.OnInviteClicked)

            // When
            viewModel.onEvent(BalancesUiEvent.OnSplitBillClicked)

            // Then
            assertEquals(
                listOf(Route.Groups, route, Route.Invite(GROUP_ID, isNewGroup = false)),
                navigator.backStack.value,
            )
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
            viewModel.onEvent(BalancesUiEvent.OnDeleteGroupClicked)

            // When
            viewModel.onEvent(BalancesUiEvent.OnGroupExitConfirmed)

            // Then
            assertNull(viewModel.uiState.value.exit.dialog)
            assertEquals(listOf(Route.Groups, route), navigator.backStack.value)
            assertTrue(groups.deletedGroupIds.isEmpty())
        }

    @Test
    fun `GIVEN everyone settled on Saldos WHEN confirming Apagar grupo THEN deletes it and goes back to Grupos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(BalancesUiEvent.OnDeleteGroupClicked)

            // When
            viewModel.onEvent(BalancesUiEvent.OnGroupExitConfirmed)

            // Then
            assertEquals(listOf(GROUP_ID), groups.deletedGroupIds)
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the server unreachable WHEN confirming Sair on Saldos THEN says so`() = runTest(testDispatcher) {
        // Given
        prepareScenario(exitError = IllegalStateException("offline"))
        viewModel.onEvent(BalancesUiEvent.OnLeaveGroupClicked)

        // When
        viewModel.onEvent(BalancesUiEvent.OnGroupExitConfirmed)

        // Then
        assertEquals(listOf<BalancesUiAction>(BalancesUiAction.ShowLeaveError), actions)
        assertEquals(listOf(Route.Groups, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Saldos on top WHEN tapping back THEN returns to the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = BalancesUiEvent.OnBackClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(listOf<Route>(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen on top WHEN tapping back THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Invite(GROUP_ID, isNewGroup = false))

        // When
        viewModel.onEvent(BalancesUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Groups, route, Route.Invite(GROUP_ID, isNewGroup = false)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Saldos on top WHEN the group disappears THEN goes back`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val noGroups = emptyList<Group>()

        // When
        groups.groups.value = noGroups

        // Then
        assertEquals(listOf<Route>(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Registrar pagamento on top WHEN the group disappears THEN leaves it in place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.RegisterPayment(GROUP_ID, PIERRE, ANA))
            val noGroups = emptyList<Group>()

            // When
            groups.groups.value = noGroups

            // Then
            assertEquals(
                listOf(Route.Groups, route, Route.RegisterPayment(GROUP_ID, PIERRE, ANA)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the group gone while covered WHEN the cover closes THEN goes back`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.RegisterPayment(GROUP_ID, PIERRE, ANA))
        groups.groups.value = emptyList()

        // When
        navigator.navigateBack()

        // Then
        assertEquals(listOf<Route>(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Registrar pagamento over Saldos WHEN it closes after recording THEN shows the notice once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.RegisterPayment(GROUP_ID, ANA, PIERRE))
            val settlementId = record(
                from = ANA,
                to = PIERRE,
            )

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                PaymentNotice(
                    settlementId = settlementId,
                    fromName = "Ana",
                    fromIsMe = false,
                    toName = "Pierre",
                ),
                viewModel.uiState.value.recordedPayment,
            )
            assertNull(recordedPayments.observeRecorded().first())
        }

    @Test
    fun `GIVEN this device user paid Bruno WHEN Registrar pagamento closes THEN the notice is from me`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.RegisterPayment(GROUP_ID, PIERRE, BRUNO))
            record(
                from = PIERRE,
                to = BRUNO,
            )

            // When
            navigator.navigateBack()

            // Then
            val notice = viewModel.uiState.value.recordedPayment
            assertEquals("Pierre", notice?.fromName)
            assertEquals(true, notice?.fromIsMe)
            assertEquals("Bruno", notice?.toName)
        }

    @Test
    fun `GIVEN Registrar pagamento still on top WHEN a payment is recorded THEN the notice waits`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.RegisterPayment(GROUP_ID, ANA, PIERRE))

            // When
            val settlementId = record(
                from = ANA,
                to = PIERRE,
            )

            // Then
            assertNull(viewModel.uiState.value.recordedPayment)
            assertEquals(settlementId, recordedPayments.observeRecorded().first()?.id)
        }

    @Test
    fun `GIVEN a payment recorded before Room was read WHEN the group loads THEN shows the notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isRoomReadAtStart = false)
            val settlementId = record(
                from = ANA,
                to = PIERRE,
            )

            // When
            isRoomRead.value = true

            // Then
            assertEquals(
                settlementId,
                viewModel.uiState.value.recordedPayment
                    ?.settlementId,
            )
            assertEquals(
                "Ana",
                viewModel.uiState.value.recordedPayment
                    ?.fromName,
            )
            assertNull(recordedPayments.observeRecorded().first())
        }

    @Test
    fun `GIVEN Saldos on top WHEN another group records a payment THEN shows no notice and leaves it waiting`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val settlementId = record(
                from = ANA,
                to = PIERRE,
                groupId = "group-2",
            )

            // Then
            assertNull(viewModel.uiState.value.recordedPayment)
            assertEquals(settlementId, recordedPayments.observeRecorded().first()?.id)
        }

    @Test
    fun `GIVEN the payment notice on Saldos WHEN it was shown THEN it hides without deleting the payment`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            record(
                from = ANA,
                to = PIERRE,
            )

            // When
            viewModel.onEvent(BalancesUiEvent.OnRecordedPaymentNoticeShown)

            // Then
            assertNull(viewModel.uiState.value.recordedPayment)
            assertTrue(settlements.deletedIds.isEmpty())
            assertEquals(1, settlements.settlements.value.size)
        }

    @Test
    fun `GIVEN the device fails to delete WHEN tapping Desfazer on Saldos THEN says so and the payment still counts`() =
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
            record(
                from = PIERRE,
                to = BRUNO,
                amountMinor = 3_000,
            )
            settlements.deleteError = IllegalStateException("disk full")

            // When
            viewModel.onEvent(BalancesUiEvent.OnUndoPaymentClicked)

            // Then
            assertEquals(listOf<BalancesUiAction>(BalancesUiAction.ShowChangeError), actions)
            assertTrue(
                viewModel.uiState.value.myDebts
                    .isEmpty(),
            )
        }

    @Test
    fun `GIVEN the payment notice on Saldos WHEN tapping Desfazer THEN deletes that payment and the debt is back`() =
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
            val settlementId = record(
                from = PIERRE,
                to = BRUNO,
                amountMinor = 3_000,
            )

            // When
            viewModel.onEvent(BalancesUiEvent.OnUndoPaymentClicked)

            // Then
            assertEquals(listOf(settlementId), settlements.deletedIds)
            assertEquals(listOf(nowMillis), settlements.deletedAts)
            assertNull(viewModel.uiState.value.recordedPayment)
            assertEquals(
                listOf(PIERRE to BRUNO),
                viewModel.uiState.value.myDebts
                    .map { it.fromMemberId to it.toMemberId },
            )
        }

    @Test
    fun `GIVEN no payment notice on Saldos WHEN tapping Desfazer THEN deletes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedSettlements = listOf(
                settlement(
                    id = "pay-1",
                    from = PIERRE,
                    to = ANA,
                ),
            ),
        )
        val event = BalancesUiEvent.OnUndoPaymentClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertTrue(settlements.deletedIds.isEmpty())
        assertEquals(listOf("pay-1"), settlements.settlements.value.map { it.id })
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

    private val route = Route.Balances(GROUP_ID)

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
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup)).also { it.exitError = exitError }
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        navigator = BackStackNavigator(startRoute = Route.Groups).also { it.navigate(route) }
        isRoomRead.value = isRoomReadAtStart
        recordSettlement = RecordSettlementUseCase(settlements, recordedPayments) { nowMillis }
        val observeStoredGroup = ObserveGroupUseCase(groups)
        viewModel = BalancesViewModel(
            route = route,
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
