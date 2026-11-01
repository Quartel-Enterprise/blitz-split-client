package com.quare.blitzsplit.feature.groupdetail

import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
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
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup
import com.quare.blitzsplit.core.sync.domain.usecase.LeaveGroup
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiAction
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.PaymentRow
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupDetailViewModel
import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupScreenUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

// Why: the payments, balance card and payment notice of Despesas; the bills are in GroupDetailViewModelTest.
class GroupDetailPaymentsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var recordSettlement: RecordSettlementUseCase
    private lateinit var viewModel: GroupDetailViewModel
    private val recordedPayments = InMemoryRecordedPaymentRepository()
    private val isRoomRead = MutableStateFlow(true)
    private val groupDeletions = InMemoryGroupDeletionRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a payment this device user made WHEN the screen opens THEN shows it as a payment row`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedSettlements = listOf(
                    settlement(
                        id = "pix-1",
                        from = PIERRE,
                        to = ANA,
                        amountMinor = 5_000,
                        paidAt = thisAfternoon,
                        method = SettlementMethod.PIX,
                        isPending = true,
                    ),
                ),
            )

            // When
            val entry = viewModel.uiState.value.days
                .single()
                .entries
                .single()

            // Then
            assertEquals(
                PaymentRow(
                    id = "pix-1",
                    paidAt = thisAfternoon,
                    fromName = "Pierre",
                    fromIsMe = true,
                    toName = "Ana",
                    toIsMe = false,
                    amountMinor = 5_000,
                    method = SettlementMethod.PIX,
                    isPending = true,
                ),
                entry,
            )
        }

    @Test
    fun `GIVEN a payment Ana made to this device user WHEN the screen opens THEN the row is received by me`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedSettlements = listOf(
                    settlement(
                        id = "cash-1",
                        from = ANA,
                        to = PIERRE,
                        method = SettlementMethod.CASH,
                    ),
                ),
            )

            // When
            val row = paymentRows().single()

            // Then
            assertEquals("Ana", row.fromName)
            assertFalse(row.fromIsMe)
            assertEquals("Pierre", row.toName)
            assertTrue(row.toIsMe)
            assertEquals(SettlementMethod.CASH, row.method)
            assertFalse(row.isPending)
        }

    @Test
    fun `GIVEN a payment between two other members WHEN the screen opens THEN the row is neither from nor to me`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedSettlements = listOf(
                    settlement(
                        id = "pay-1",
                        from = BRUNO,
                        to = ANA,
                    ),
                ),
            )

            // When
            val row = paymentRows().single()

            // Then
            assertEquals("Bruno", row.fromName)
            assertEquals("Ana", row.toName)
            assertFalse(row.fromIsMe)
            assertFalse(row.toIsMe)
        }

    @Test
    fun `GIVEN a payment from someone no longer in the group WHEN the screen opens THEN the row has no payer name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedSettlements = listOf(
                    settlement(
                        id = "pay-1",
                        from = "member-gone",
                        to = PIERRE,
                    ),
                ),
            )

            // When
            val row = paymentRows().single()

            // Then
            assertNull(row.fromName)
            assertFalse(row.fromIsMe)
            assertEquals("Pierre", row.toName)
        }

    @Test
    fun `GIVEN a payment to someone no longer in the group WHEN the screen opens THEN the row has no receiver name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedSettlements = listOf(
                    settlement(
                        id = "pay-1",
                        from = PIERRE,
                        to = "member-gone",
                    ),
                ),
            )

            // When
            val row = paymentRows().single()

            // Then
            assertNull(row.toName)
            assertFalse(row.toIsMe)
            assertTrue(row.fromIsMe)
        }

    @Test
    fun `GIVEN a group without this device user WHEN the screen opens THEN no payment is from or to me`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(members = listOf(ana, bruno)),
                storedSettlements = listOf(
                    settlement(
                        id = "pay-1",
                        from = BRUNO,
                        to = ANA,
                    ),
                ),
            )

            // When
            val row = paymentRows().single()

            // Then
            assertFalse(row.fromIsMe)
            assertFalse(row.toIsMe)
        }

    @Test
    fun `GIVEN bills and payments over two days WHEN the screen opens THEN mixes them by time most recent first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense("breakfast", spentAt = thisMorning),
                    expense("dinner", spentAt = yesterdayEvening),
                ),
                storedSettlements = listOf(
                    settlement(
                        id = "pay-today",
                        paidAt = thisAfternoon,
                    ),
                    settlement(
                        id = "pay-yesterday",
                        paidAt = yesterdayNoon,
                    ),
                ),
            )

            // When
            val days = viewModel.uiState.value.days

            // Then
            assertEquals(listOf(RelativeDay.TODAY, RelativeDay.YESTERDAY), days.map { it.relativeDay })
            assertEquals(
                listOf(listOf("pay-today", "breakfast"), listOf("dinner", "pay-yesterday")),
                days.map { day -> day.entries.map { it.id } },
            )
        }

    @Test
    fun `GIVEN only payments in the group WHEN the screen opens THEN lists them without counting them as spent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedSettlements = listOf(
                    settlement(
                        id = "pay-1",
                        amountMinor = 7_000,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf("pay-1"), state.days.flatMap { day -> day.entries.map { it.id } })
            assertEquals(0L, state.header.totalSpentMinor)
        }

    @Test
    fun `GIVEN a bill and a payment WHEN the screen opens THEN the total adds the bill only`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "dinner",
                        spentAt = thisMorning,
                        amountMinor = 12_000,
                    ),
                ),
                storedSettlements = listOf(
                    settlement(
                        id = "pay-1",
                        amountMinor = 6_000,
                    ),
                ),
            )

            // When
            val total = viewModel.uiState.value.header.totalSpentMinor

            // Then
            assertEquals(12_000L, total)
        }

    @Test
    fun `GIVEN a payment of another group WHEN the screen opens THEN it is not on the list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(expense("dinner", spentAt = thisMorning)),
                storedSettlements = listOf(
                    settlement(
                        id = "elsewhere",
                        groupId = "group-2",
                    ),
                ),
            )

            // When
            val ids = viewModel.uiState.value.days
                .flatMap { day -> day.entries.map { it.id } }

            // Then
            assertEquals(listOf("dinner"), ids)
        }

    @Test
    fun `GIVEN a group without bills WHEN the screen opens THEN there is no balance card and nobody settled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val header = viewModel.uiState.value.header

            // Then
            assertNull(header.myBalance)
            assertFalse(header.isEveryoneSettled)
        }

    @Test
    fun `GIVEN a bill Ana paid that this device user shares WHEN the screen opens THEN the balance card says I owe`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "dinner",
                        spentAt = thisMorning,
                        amountMinor = 12_000,
                    ),
                ),
            )

            // When
            val header = viewModel.uiState.value.header

            // Then
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 6_000,
                ),
                header.myBalance,
            )
            assertFalse(header.isEveryoneSettled)
        }

    @Test
    fun `GIVEN this device user owes one and is owed by another WHEN the screen opens THEN the card holds both`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "dinner",
                        spentAt = thisMorning,
                        amountMinor = 12_000,
                    ),
                    expense(
                        id = "taxi",
                        spentAt = thisMorning,
                        amountMinor = 8_000,
                        payers = listOf(ExpensePayer(PIERRE, 8_000)),
                        shares = listOf(share(PIERRE, 4_000), share(BRUNO, 4_000)),
                    ),
                ),
            )

            // When
            val balance = viewModel.uiState.value.header.myBalance

            // Then
            assertEquals(
                MemberBalance(
                    receivesMinor = 4_000,
                    owesMinor = 6_000,
                ),
                balance,
            )
        }

    @Test
    fun `GIVEN a bill and a payment that settles it WHEN the screen opens THEN everyone is settled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "dinner",
                        spentAt = thisMorning,
                        amountMinor = 12_000,
                    ),
                ),
                storedSettlements = listOf(
                    settlement(
                        id = "pay-1",
                        from = PIERRE,
                        to = ANA,
                        amountMinor = 6_000,
                    ),
                ),
            )

            // When
            val header = viewModel.uiState.value.header

            // Then
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 0,
                ),
                header.myBalance,
            )
            assertTrue(header.isEveryoneSettled)
        }

    @Test
    fun `GIVEN a payment of part of the debt WHEN the screen opens THEN the card says what is left`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "dinner",
                        spentAt = thisMorning,
                        amountMinor = 12_000,
                    ),
                ),
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
            val header = viewModel.uiState.value.header

            // Then
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 4_000,
                ),
                header.myBalance,
            )
            assertFalse(header.isEveryoneSettled)
        }

    @Test
    fun `GIVEN a payment with no bill WHEN the screen opens THEN the card shows what comes back and nobody settled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedSettlements = listOf(
                    settlement(
                        id = "pay-1",
                        from = PIERRE,
                        to = ANA,
                        amountMinor = 5_000,
                    ),
                ),
            )

            // When
            val header = viewModel.uiState.value.header

            // Then
            assertEquals(
                MemberBalance(
                    receivesMinor = 5_000,
                    owesMinor = 0,
                ),
                header.myBalance,
            )
            assertFalse(header.isEveryoneSettled)
        }

    @Test
    fun `GIVEN a group without this device user WHEN a bill is in it THEN there is no balance card`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(members = listOf(ana, bruno)),
                storedExpenses = listOf(
                    expense(
                        id = "taxi",
                        spentAt = thisMorning,
                        amountMinor = 4_000,
                        shares = listOf(share(ANA, 2_000), share(BRUNO, 2_000)),
                    ),
                ),
            )

            // When
            val header = viewModel.uiState.value.header

            // Then
            assertNull(header.myBalance)
            assertFalse(header.isEveryoneSettled)
        }

    @Test
    fun `GIVEN the screen open WHEN a payment syncs in THEN the balance card and the list follow`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        "dinner",
                        spentAt = thisMorning,
                        amountMinor = 12_000,
                    ),
                ),
            )
            val synced = settlement(
                id = "pay-1",
                from = PIERRE,
                to = ANA,
                amountMinor = 6_000,
            )

            // When
            settlements.settlements.value = listOf(synced)

            // Then
            val state = viewModel.uiState.value
            assertEquals(
                MemberBalance(
                    receivesMinor = 0,
                    owesMinor = 0,
                ),
                state.header.myBalance,
            )
            assertTrue(state.header.isEveryoneSettled)
            assertEquals(
                listOf("pay-1", "dinner"),
                state.days
                    .single()
                    .entries
                    .map { it.id },
            )
        }

    @Test
    fun `GIVEN a payment on the list WHEN tapping it THEN asks to delete that payment`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedSettlements = listOf(settlement(id = "pay-1")))
        val event = GroupDetailUiEvent.OnPaymentClicked("pay-1")

        // When
        viewModel.onEvent(event)

        // Then
        assertEquals(paymentRows().single(), viewModel.uiState.value.paymentToDelete)
        assertEquals(listOf(Route.Groups, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN no such payment on the list WHEN tapping it THEN asks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedSettlements = listOf(settlement(id = "pay-1")))
        val event = GroupDetailUiEvent.OnPaymentClicked("pay-unknown")

        // When
        viewModel.onEvent(event)

        // Then
        assertNull(viewModel.uiState.value.paymentToDelete)
    }

    @Test
    fun `GIVEN a bill id WHEN tapped as a payment THEN asks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(expense("dinner", spentAt = thisMorning)))
        val event = GroupDetailUiEvent.OnPaymentClicked("dinner")

        // When
        viewModel.onEvent(event)

        // Then
        assertNull(viewModel.uiState.value.paymentToDelete)
    }

    @Test
    fun `GIVEN the delete question open WHEN confirming THEN deletes the payment as a tombstone and closes it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedSettlements = listOf(settlement(id = "pay-1")))
            viewModel.onEvent(GroupDetailUiEvent.OnPaymentClicked("pay-1"))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnPaymentDeleteConfirmed)

            // Then
            assertEquals(listOf("pay-1"), settlements.deletedIds)
            assertEquals(listOf(nowMillis), settlements.deletedAts)
            assertNull(viewModel.uiState.value.paymentToDelete)
            assertTrue(
                viewModel.uiState.value.days
                    .isEmpty(),
            )
        }

    @Test
    fun `GIVEN the delete question open WHEN dismissing it THEN closes it and keeps the payment`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedSettlements = listOf(settlement(id = "pay-1")))
            viewModel.onEvent(GroupDetailUiEvent.OnPaymentClicked("pay-1"))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnPaymentDeleteDismissed)

            // Then
            assertNull(viewModel.uiState.value.paymentToDelete)
            assertTrue(settlements.deletedIds.isEmpty())
            assertEquals(listOf("pay-1"), paymentRows().map { it.id })
        }

    @Test
    fun `GIVEN no delete question open WHEN confirming THEN deletes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedSettlements = listOf(settlement(id = "pay-1")))
        val event = GroupDetailUiEvent.OnPaymentDeleteConfirmed

        // When
        viewModel.onEvent(event)

        // Then
        assertTrue(settlements.deletedIds.isEmpty())
    }

    @Test
    fun `GIVEN the delete question open WHEN someone else undoes the payment THEN the question closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedSettlements = listOf(settlement(id = "pay-1")))
            viewModel.onEvent(GroupDetailUiEvent.OnPaymentClicked("pay-1"))
            val noPayments = emptyList<Settlement>()

            // When
            settlements.settlements.value = noPayments

            // Then
            assertNull(viewModel.uiState.value.paymentToDelete)
            assertTrue(settlements.deletedIds.isEmpty())
        }

    @Test
    fun `GIVEN the delete question open WHEN a bill syncs in THEN the question stays open`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedSettlements = listOf(settlement(id = "pay-1")))
        viewModel.onEvent(GroupDetailUiEvent.OnPaymentClicked("pay-1"))
        val bill = expense("dinner", spentAt = thisMorning)

        // When
        expenses.expenses.value = listOf(bill)

        // Then
        assertEquals(
            "pay-1",
            viewModel.uiState.value.paymentToDelete
                ?.id,
        )
    }

    @Test
    fun `GIVEN this device user owes Ana and more to Bruno WHEN tapping Acertar THEN opens the payment to Bruno`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = owingAnaAndMoreToBruno)
            val event = GroupDetailUiEvent.OnSettleClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Groups, route, Route.RegisterPayment(GROUP_ID, PIERRE, BRUNO)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the debt to Bruno paid since WHEN tapping Acertar THEN opens the payment to Ana`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = owingAnaAndMoreToBruno,
                storedSettlements = listOf(
                    settlement(
                        id = "pay-1",
                        from = PIERRE,
                        to = BRUNO,
                        amountMinor = 9_000,
                    ),
                ),
            )
            val event = GroupDetailUiEvent.OnSettleClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(
                listOf(Route.Groups, route, Route.RegisterPayment(GROUP_ID, PIERRE, ANA)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN this device user only owed money WHEN tapping Acertar THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    expense(
                        id = "market",
                        spentAt = thisMorning,
                        amountMinor = 10_000,
                        payers = listOf(ExpensePayer(PIERRE, 10_000)),
                        shares = listOf(share(PIERRE, 5_000), share(ANA, 5_000)),
                    ),
                ),
            )
            val event = GroupDetailUiEvent.OnSettleClicked

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
                    expense(
                        id = "taxi",
                        spentAt = thisMorning,
                        shares = listOf(share(ANA, 6_000), share(BRUNO, 6_000)),
                    ),
                ),
            )
            val event = GroupDetailUiEvent.OnSettleClicked

            // When
            viewModel.onEvent(event)

            // Then
            assertEquals(listOf(Route.Groups, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN another screen on top WHEN tapping Acertar THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = owingAnaAndMoreToBruno)
        navigator.navigate(Route.Invite(GROUP_ID, isNewGroup = false))

        // When
        viewModel.onEvent(GroupDetailUiEvent.OnSettleClicked)

        // Then
        assertEquals(listOf(Route.Groups, route, Route.Invite(GROUP_ID, isNewGroup = false)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen on top WHEN tapping the balances tab THEN the back stack stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Invite(GROUP_ID, isNewGroup = false))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnBalancesTabClicked)

            // Then
            assertEquals(
                listOf(Route.Groups, route, Route.Invite(GROUP_ID, isNewGroup = false)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Registrar pagamento over the screen WHEN it closes after recording THEN shows the notice once`() =
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
                    settlementIds = listOf(settlementId),
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
            val settlementId = record(
                from = PIERRE,
                to = BRUNO,
            )

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                PaymentNotice(
                    settlementIds = listOf(settlementId),
                    fromName = "Pierre",
                    fromIsMe = true,
                    toName = "Bruno",
                ),
                viewModel.uiState.value.recordedPayment,
            )
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
            assertEquals(
                settlementId,
                recordedPayments
                    .observeRecorded()
                    .first()
                    ?.settlementIds
                    ?.single(),
            )
        }

    @Test
    fun `GIVEN Room not read yet WHEN a payment is recorded THEN the notice waits for the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isRoomReadAtStart = false)

            // When
            val settlementId = record(
                from = ANA,
                to = PIERRE,
            )

            // Then
            assertNull(viewModel.uiState.value.recordedPayment)
            assertEquals(
                settlementId,
                recordedPayments
                    .observeRecorded()
                    .first()
                    ?.settlementIds
                    ?.single(),
            )
        }

    @Test
    fun `GIVEN a payment recorded before Room was read WHEN the group loads THEN shows the notice with names`() =
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
                PaymentNotice(
                    settlementIds = listOf(settlementId),
                    fromName = "Ana",
                    fromIsMe = false,
                    toName = "Pierre",
                ),
                viewModel.uiState.value.recordedPayment,
            )
            assertNull(recordedPayments.observeRecorded().first())
        }

    @Test
    fun `GIVEN the screen on top WHEN another group records a payment THEN shows no notice and leaves it waiting`() =
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
            assertEquals(
                settlementId,
                recordedPayments
                    .observeRecorded()
                    .first()
                    ?.settlementIds
                    ?.single(),
            )
        }

    @Test
    fun `GIVEN the payment notice on screen WHEN it was shown THEN it hides without deleting the payment`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val settlementId = record(
                from = ANA,
                to = PIERRE,
            )

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnRecordedPaymentNoticeShown)

            // Then
            assertNull(viewModel.uiState.value.recordedPayment)
            assertTrue(settlements.deletedIds.isEmpty())
            assertEquals(listOf(settlementId), paymentRows().map { it.id })
        }

    @Test
    fun `GIVEN the payment notice on screen WHEN tapping Desfazer THEN deletes that payment and hides the notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val settlementId = record(
                from = ANA,
                to = PIERRE,
            )

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnUndoPaymentClicked)

            // Then
            assertEquals(listOf(settlementId), settlements.deletedIds)
            assertEquals(listOf(nowMillis), settlements.deletedAts)
            assertNull(viewModel.uiState.value.recordedPayment)
            assertTrue(paymentRows().isEmpty())
        }

    @Test
    fun `GIVEN the device fails to delete WHEN tapping Desfazer THEN says so and the payment stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val actions = mutableListOf<GroupDetailUiAction>()
            backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
            val settlementId = record(
                from = ANA,
                to = PIERRE,
            )
            settlements.deleteError = IllegalStateException("disk full")

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnUndoPaymentClicked)

            // Then
            assertEquals(listOf<GroupDetailUiAction>(GroupDetailUiAction.ShowChangeError), actions)
            assertEquals(listOf(settlementId), paymentRows().map { it.id })
        }

    @Test
    fun `GIVEN the device fails to delete WHEN confirming the delete question THEN says so and the payment stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedSettlements = listOf(settlement(id = "pay-1")))
            val actions = mutableListOf<GroupDetailUiAction>()
            backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
            settlements.deleteError = IllegalStateException("disk full")
            viewModel.onEvent(GroupDetailUiEvent.OnPaymentClicked("pay-1"))

            // When
            viewModel.onEvent(GroupDetailUiEvent.OnPaymentDeleteConfirmed)

            // Then
            assertEquals(listOf<GroupDetailUiAction>(GroupDetailUiAction.ShowChangeError), actions)
            assertEquals(listOf("pay-1"), paymentRows().map { it.id })
        }

    @Test
    fun `GIVEN no payment notice WHEN tapping Desfazer THEN deletes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedSettlements = listOf(settlement(id = "pay-1")))
        val event = GroupDetailUiEvent.OnUndoPaymentClicked

        // When
        viewModel.onEvent(event)

        // Then
        assertTrue(settlements.deletedIds.isEmpty())
        assertEquals(listOf("pay-1"), paymentRows().map { it.id })
    }

    @Test
    fun `GIVEN the screen on top WHEN a payment is recorded THEN shows the notice at once and lists the payment`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val settlementId = record(
                from = ANA,
                to = PIERRE,
            )

            // Then
            assertNotNull(viewModel.uiState.value.recordedPayment)
            assertEquals(listOf(settlementId), paymentRows().map { it.id })
        }

    private fun paymentRows(): List<PaymentRow> = viewModel.uiState.value.days
        .flatMap { it.entries }
        .filterIsInstance<PaymentRow>()

    private suspend fun record(
        from: String,
        to: String,
        groupId: String = GROUP_ID,
    ): String = recordSettlement(
        groupId = groupId,
        fromMemberId = from,
        toMemberId = to,
        amountMinor = 5_000,
        method = SettlementMethod.PIX,
        paidAt = thisAfternoon,
    ).getOrThrow()

    private val route = Route.GroupDetail(GROUP_ID)

    private val nowMillis = Instant.parse("2026-10-30T15:00:00Z").toEpochMilliseconds()
    private val thisMorning = Instant.parse("2026-10-30T09:00:00Z").toEpochMilliseconds()
    private val thisAfternoon = Instant.parse("2026-10-30T13:00:00Z").toEpochMilliseconds()
    private val yesterdayNoon = Instant.parse("2026-10-29T12:00:00Z").toEpochMilliseconds()
    private val yesterdayEvening = Instant.parse("2026-10-29T20:00:00Z").toEpochMilliseconds()

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

    // Why: this device's user owes Ana 6.000 and Bruno 9.000.
    private val owingAnaAndMoreToBruno
        get() = listOf(
            expense(
                "dinner",
                spentAt = thisMorning,
                amountMinor = 12_000,
            ),
            expense(
                id = "hotel",
                spentAt = thisMorning,
                amountMinor = 18_000,
                payers = listOf(ExpensePayer(BRUNO, 18_000)),
                shares = listOf(share(PIERRE, 9_000), share(BRUNO, 9_000)),
            ),
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

    // Why: Ana paid it and splits it with this device's user by default.
    private fun expense(
        id: String,
        spentAt: Long,
        amountMinor: Long = 12_000,
        payers: List<ExpensePayer> = listOf(ExpensePayer(ANA, amountMinor)),
        shares: List<ExpenseShare> = listOf(share(ANA, amountMinor / 2), share(PIERRE, amountMinor / 2)),
    ): Expense = Expense(
        id = id,
        groupId = GROUP_ID,
        description = "Conta $id",
        amountMinor = amountMinor,
        splitMode = SplitMode.EQUAL,
        payers = payers,
        shares = shares,
        items = null,
        categoryId = null,
        spentAt = spentAt,
        isPending = false,
        updatedAt = spentAt,
        updatedByMemberId = null,
    )

    private fun settlement(
        id: String,
        from: String = PIERRE,
        to: String = ANA,
        amountMinor: Long = 5_000,
        paidAt: Long = thisAfternoon,
        method: SettlementMethod = SettlementMethod.PIX,
        isPending: Boolean = false,
        groupId: String = GROUP_ID,
    ): Settlement = Settlement(
        id = id,
        groupId = groupId,
        fromMemberId = from,
        toMemberId = to,
        amountMinor = amountMinor,
        method = method,
        paidAt = paidAt,
        isPending = isPending,
        updatedAt = 0L,
    )

    private fun prepareScenario(
        storedGroup: Group = trip,
        storedExpenses: List<Expense> = emptyList(),
        storedSettlements: List<Settlement> = emptyList(),
        isRoomReadAtStart: Boolean = true,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup))
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        navigator = BackStackNavigator(startRoute = Route.Groups).also { it.navigate(route) }
        isRoomRead.value = isRoomReadAtStart
        recordSettlement = RecordSettlementUseCase(settlements, recordedPayments) { nowMillis }
        val deletedBills = InMemoryDeletedBillRepository()
        val observeStoredGroup = ObserveGroupUseCase(groups)
        viewModel = GroupDetailViewModel(
            route = route,
            currentTimeMillis = { nowMillis },
            currentTimeZone = { TimeZone.UTC },
            consumeDeletedBillNotice = ConsumeDeletedBillNoticeUseCase(deletedBills),
            observeDeletedBillNotice = ObserveDeletedBillNoticeUseCase(deletedBills),
            observeCategoryTree = ObserveCategoryTreeUseCase(FakeCategoryRepository()),
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
                observeGroupDeletions = ObserveGroupDeletionsUseCase(groupDeletions),
                consumeGroupDeletion = ConsumeGroupDeletionUseCase(groupDeletions),
                connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
                leaveGroup = LeaveGroup { groups.requestLeaveGroup(it) },
                deleteGroup = DeleteGroup { groups.deleteGroup(it) },
            ),
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
        const val CARLA = "member-4"
    }
}
