package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairOutcome
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CreateExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettlementsUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.UpdateExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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

// Why: 7.4b, "Editar conta" on a bill a payment settled afterwards; EditBillViewModelTest covers the rest of it.
class EditAfterPaymentViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var viewModel: SplitBillViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Bruno settled with Ana after the bill WHEN Editar conta opens untouched THEN warns of nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.settledPair)
            assertTrue(state.showsAnyoneCanEditHint)
        }

    @Test
    fun `GIVEN Bruno settled with Ana after the bill WHEN the total doubles THEN warns he owes her again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("624,00"))

            // Then
            assertEquals(
                SettledPairChange(
                    payerName = "Bruno",
                    payerIsMe = false,
                    receiverName = "Ana",
                    receiverIsMe = false,
                    outcome = SettledPairOutcome.OWES_AGAIN,
                    debtorIsPayer = true,
                    amountMinor = 10_400L,
                ),
                viewModel.uiState.value.settledPair,
            )
            assertFalse(viewModel.uiState.value.showsAnyoneCanEditHint)
        }

    @Test
    fun `GIVEN Bruno settled with Ana after the bill WHEN he leaves the split THEN warns Ana comes to owe him`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(BRUNO))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // Then
            val settledPair = viewModel.uiState.value.settledPair
            assertEquals(SettledPairOutcome.OWES, settledPair?.outcome)
            assertEquals("Ana", settledPair?.debtorName)
            assertEquals("Bruno", settledPair?.creditorName)
            assertEquals(10_400L, settledPair?.amountMinor)
        }

    @Test
    fun `GIVEN a change that moves the pair WHEN the total is cleared THEN the warning goes until it can be saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("624,00"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged(""))

            // Then
            assertNull(viewModel.uiState.value.settledPair)
        }

    @Test
    fun `GIVEN the bill was changed WHEN a payment after it syncs THEN the warning shows at once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedSettlements = emptyList())
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("624,00"))

            // When
            settlements.settlements.value = listOf(brunoPaidAna)

            // Then
            assertEquals(
                "Bruno",
                viewModel.uiState.value.settledPair
                    ?.payerName,
            )
        }

    @Test
    fun `GIVEN the total doubled WHEN Quando moves past the payment THEN the warning goes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("624,00"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(afterThePaymentMillis))

        // Then
        assertNull(viewModel.uiState.value.settledPair)
    }

    @Test
    fun `GIVEN a payment dated before the bill WHEN Quando moves before it THEN the warning shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedSettlements = listOf(brunoPaidAna.copy(paidAt = beforeTheBillMillis)))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("624,00"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(beforeThePaymentMillis))

            // Then
            assertEquals(
                "Bruno",
                viewModel.uiState.value.settledPair
                    ?.payerName,
            )
        }

    @Test
    fun `GIVEN no payment in the group WHEN the total doubles THEN warns of nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedSettlements = emptyList())

        // When
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("624,00"))

        // Then
        assertNull(viewModel.uiState.value.settledPair)
        assertTrue(viewModel.uiState.value.showsAnyoneCanEditHint)
    }

    @Test
    fun `GIVEN Bruno settled with Ana after the bill WHEN tapping Apagar conta THEN the question says Ana owes him`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)

            // Then
            val settledPair = viewModel.uiState.value.deletion
                ?.settledPair
            assertEquals(SettledPairOutcome.OWES, settledPair?.outcome)
            assertEquals(false, settledPair?.debtorIsPayer)
            assertEquals(10_400L, settledPair?.amountMinor)
        }

    @Test
    fun `GIVEN Apagar conta open WHEN the payment is undone elsewhere THEN the question stops saying it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)

            // When
            settlements.settlements.value = emptyList()

            // Then
            assertNull(
                viewModel.uiState.value.deletion
                    ?.settledPair,
            )
        }

    private val route = Route.SplitBill(GROUP_ID, EXPENSE_ID, picksGroupFirst = false)

    private val yesterdayMillis = Instant.parse("2026-10-29T20:00:00Z").toEpochMilliseconds()

    // Why: Quando moved past Bruno's payment this morning, still before now.
    private val afterThePaymentMillis = Instant.parse("2026-10-30T12:00:00Z").toEpochMilliseconds()

    // Why: a payment two hours before the bill, and a Quando moved before it.
    private val beforeTheBillMillis = Instant.parse("2026-10-29T18:00:00Z").toEpochMilliseconds()
    private val beforeThePaymentMillis = Instant.parse("2026-10-29T10:00:00Z").toEpochMilliseconds()

    // Why: "Mercado", R$ 312,00 Ana paid yesterday, split equally among Ana, Pierre and Bruno.
    private val savedBill = Expense(
        id = EXPENSE_ID,
        groupId = GROUP_ID,
        description = "Mercado",
        amountMinor = 31_200L,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(ANA, 31_200L)),
        shares = listOf(
            ExpenseShare(ANA, 10_400L, weight = 1L),
            ExpenseShare(PIERRE, 10_400L, weight = 1L),
            ExpenseShare(BRUNO, 10_400L, weight = 1L),
        ),
        items = null,
        categoryId = null,
        spentAt = yesterdayMillis,
        isPending = false,
        updatedAt = SAVED_VERSION,
        updatedByMemberId = ANA,
    )

    // Why: Bruno paid Ana his part this morning, which settled them.
    private val brunoPaidAna = Settlement(
        id = "settlement-1",
        groupId = GROUP_ID,
        fromMemberId = BRUNO,
        toMemberId = ANA,
        amountMinor = 10_400L,
        method = SettlementMethod.PIX,
        paidAt = Instant.parse("2026-10-30T09:00:00Z").toEpochMilliseconds(),
        isPending = false,
        updatedAt = SAVED_VERSION,
    )

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        currency = GroupCurrency.BRL,
        members = listOf(
            member(ANA, "Ana", isMe = false),
            member(PIERRE, "Pierre", isMe = true),
            member(BRUNO, "Bruno", isMe = false),
        ),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    private fun member(
        id: String,
        name: String,
        isMe: Boolean,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.PEACH,
        isMe = isMe,
        hasJoined = true,
        userId = null,
        inviteCode = null,
    )

    private fun prepareScenario(storedSettlements: List<Settlement> = listOf(brunoPaidAna)) {
        groups = FakeGroupRepository(initialGroups = listOf(trip))
        expenses = FakeExpenseRepository(initialExpenses = listOf(savedBill))
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        val deletedBills = InMemoryDeletedBillRepository()
        val navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.GroupDetail(GROUP_ID))
            it.navigate(Route.BillDetail(GROUP_ID, EXPENSE_ID))
            it.navigate(route)
        }
        viewModel = SplitBillViewModel(
            route = route,
            navigator = navigator,
            createExpense = CreateExpenseUseCase(expenses) { NOW_MILLIS },
            updateExpense = UpdateExpenseUseCase(expenses) { NOW_MILLIS },
            deleteExpense = DeleteExpenseUseCase(expenses, deletedBills) { NOW_MILLIS },
            noticeBillGone = NoticeBillGoneUseCase(deletedBills),
            currentTimeMillis = { NOW_MILLIS },
            currentTimeZone = { TimeZone.UTC },
            decimalSeparator = { ',' },
            observeGroup = ObserveGroupUseCase(groups),
            observeExpense = ObserveExpenseUseCase(expenses),
            observeGroupsByActivity = { groups.groups },
            observeExpenses = ObserveExpensesUseCase(expenses),
            observeSettlements = ObserveSettlementsUseCase(settlements),
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val EXPENSE_ID = "expense-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"

        // Why: 2026-10-30T15:00:00Z.
        const val NOW_MILLIS = 1_793_372_400_000L
        const val SAVED_VERSION = 1_793_300_000_000L
    }
}
