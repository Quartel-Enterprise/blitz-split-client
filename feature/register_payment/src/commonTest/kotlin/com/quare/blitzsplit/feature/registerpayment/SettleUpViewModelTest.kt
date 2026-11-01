package com.quare.blitzsplit.feature.registerpayment

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.RecordedPayment
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupBalancesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObservePeopleBalancesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RecordSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SettleUpUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentKind
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiAction
import com.quare.blitzsplit.feature.registerpayment.presentation.model.RegisterPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.RegisterPaymentViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
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

class SettleUpViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var recordedPayments: InMemoryRecordedPaymentRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: RegisterPaymentViewModel
    private val actions = mutableListOf<RegisterPaymentUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Carla across three groups WHEN settling up opens THEN she pays the net shown by group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals(PaymentKind.RECEIVED, state.kind)
            assertEquals("Carla", state.from?.name)
            assertTrue(state.to?.isMe == true)
            assertEquals(3_290L, state.debtMinor)
            assertEquals("32,90", state.amountText)
            assertEquals(listOf(9_840L, -9_800L, 3_250L), state.groupParts.map { it.amountMinor })
            assertTrue(state.isAmountLocked)
            assertTrue(state.canConfirm)
        }

    @Test
    fun `GIVEN someone you owe across groups WHEN settling up opens THEN you pay them`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            bills = listOf(
                owes(FLORIPA, ME_FLORIPA, CARLA_FLORIPA, 5_000L),
                owes(APE, CARLA_APE, ME_APE, 1_000L),
            ),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(PaymentKind.PAID, state.kind)
        assertEquals("Carla", state.to?.name)
        assertEquals(4_000L, state.debtMinor)
    }

    @Test
    fun `GIVEN a settling up across groups WHEN typing another amount THEN keeps the whole net`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged("10,00"))

            // Then
            assertEquals("32,90", viewModel.uiState.value.amountText)
            assertTrue(viewModel.uiState.value.canConfirm)
        }

    @Test
    fun `GIVEN a settling up across groups WHEN confirming THEN records a payment in each group and goes back`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RegisterPaymentUiEvent.OnMethodPicked(SettlementMethod.CASH))

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            val recorded = settlements.recordedSettlements
            assertEquals(listOf(FLORIPA, APE, CHURRASCO), recorded.map { it.groupId })
            assertEquals(listOf(CARLA_FLORIPA, ME_APE, CARLA_CHURRASCO), recorded.map { it.fromMemberId })
            assertEquals(listOf(ME_FLORIPA, CARLA_APE, ME_CHURRASCO), recorded.map { it.toMemberId })
            assertEquals(listOf(9_840L, 9_800L, 3_250L), recorded.map { it.amountMinor })
            assertTrue(recorded.all { it.method == SettlementMethod.CASH })
            assertEquals(
                listOf(Route.Groups, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
                navigator.backStack.value,
            )
            assertEquals(
                RecordedPayment.SettledUp(
                    recorded.map { it.id },
                    personName = "Carla",
                    isPaidByMe = false,
                ),
                recordedPayments.observeRecorded().first(),
            )
        }

    @Test
    fun `GIVEN Grupos tapped while settling up WHEN the save finishes THEN a later Saldos owes no notice`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(recordGate = gate)
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)
            navigator.popTo(1)
            navigator.navigate(Route.GlobalBalances(BalancesFilter.ALL, charge = null))

            // When
            gate.complete(Unit)

            // Then
            assertEquals(3, settlements.recordedSettlements.size)
            assertNull(recordedPayments.observeRecorded().first())
        }

    @Test
    fun `GIVEN a settling up in one group WHEN paying part of it THEN records that part in its group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(bills = listOf(owes(FLORIPA, CARLA_FLORIPA, ME_FLORIPA, 9_840L)))
            viewModel.onEvent(RegisterPaymentUiEvent.OnAmountChanged("40,00"))

            // When
            viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

            // Then
            assertFalse(viewModel.uiState.value.isAmountLocked)
            assertEquals(listOf(4_000L), settlements.recordedSettlements.map { it.amountMinor })
            assertEquals(listOf(FLORIPA), settlements.recordedSettlements.map { it.groupId })
        }

    @Test
    fun `GIVEN the settling up fails WHEN confirming THEN stays open and says so`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        settlements.recordError = IllegalStateException("disk full")

        // When
        viewModel.onEvent(RegisterPaymentUiEvent.OnConfirmClicked)

        // Then
        assertEquals(listOf<RegisterPaymentUiAction>(RegisterPaymentUiAction.ShowSaveError), actions)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(route, navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN settling up open WHEN a sync settles the person THEN closes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        expenses.expenses.update { emptyList() }

        // Then
        assertEquals(
            listOf(Route.Groups, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
            navigator.backStack.value,
        )
    }

    private val route = Route.SettleUp(
        personKey = "user:carla",
        currency = GroupCurrency.BRL,
    )

    private val defaultBills = listOf(
        owes(FLORIPA, CARLA_FLORIPA, ME_FLORIPA, 9_840L),
        owes(APE, ME_APE, CARLA_APE, 9_800L),
        owes(CHURRASCO, CARLA_CHURRASCO, ME_CHURRASCO, 3_250L),
    )

    private fun owes(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
    ): Expense = Expense(
        id = "bill-$groupId",
        groupId = groupId,
        description = "Bill",
        amountMinor = amountMinor,
        splitMode = SplitMode.AMOUNT,
        payers = listOf(ExpensePayer(toMemberId, amountMinor)),
        shares = listOf(ExpenseShare(fromMemberId, amountMinor, amountMinor)),
        spentAt = 0L,
        isPending = false,
        updatedAt = 0L,
        updatedByMemberId = null,
    )

    private fun group(
        id: String,
        meId: String,
        carlaId: String,
    ): Group = Group(
        id = id,
        name = id,
        type = GroupType.OTHER,
        color = AvatarColor.BLUE,
        currency = GroupCurrency.BRL,
        members = listOf(
            GroupMember(
                meId,
                "Pierre",
                AvatarColor.GREEN,
                isMe = true,
                hasJoined = true,
                userId = "pierre",
                inviteCode = null,
            ),
            GroupMember(
                carlaId,
                "Carla",
                AvatarColor.LILAC,
                isMe = false,
                hasJoined = true,
                userId = "carla",
                inviteCode = null,
            ),
        ),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    private fun TestScope.prepareScenario(
        bills: List<Expense> = defaultBills,
        recordGate: CompletableDeferred<Unit>? = null,
    ) {
        groups = FakeGroupRepository(
            initialGroups = listOf(
                group(FLORIPA, ME_FLORIPA, CARLA_FLORIPA),
                group(APE, ME_APE, CARLA_APE),
                group(CHURRASCO, ME_CHURRASCO, CARLA_CHURRASCO),
            ),
        )
        expenses = FakeExpenseRepository(initialExpenses = bills)
        settlements = FakeSettlementRepository().also { it.recordGate = recordGate }
        recordedPayments = InMemoryRecordedPaymentRepository()
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.GlobalBalances(BalancesFilter.ALL, charge = null))
            it.navigate(route)
        }
        val money = ObserveGroupsMoneyUseCase(expenses, settlements)
        viewModel = RegisterPaymentViewModel(
            route = route,
            navigator = navigator,
            recordSettlement = RecordSettlementUseCase(settlements, recordedPayments) { NOW },
            settleUp = SettleUpUseCase(settlements, recordedPayments) { NOW },
            consumeRecordedPayment = ConsumeRecordedPaymentUseCase(recordedPayments),
            currentTimeMillis = { NOW },
            currentTimeZone = { TimeZone.UTC },
            decimalSeparator = { ',' },
            observeGroup = ObserveGroupUseCase(groups),
            observeGroupBalances = ObserveGroupBalancesUseCase(expenses, settlements),
            observePeopleBalances = ObservePeopleBalancesUseCase(
                observeGroups = ObserveGroupsUseCase(groups),
                observeGroupsMoney = money,
            ),
            connectivityObserver = NetworkConnectivityObserver { flowOf(true) },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val NOW = 1_000L
        const val FLORIPA = "floripa"
        const val APE = "ape"
        const val CHURRASCO = "churrasco"
        const val ME_FLORIPA = "me-floripa"
        const val ME_APE = "me-ape"
        const val ME_CHURRASCO = "me-churrasco"
        const val CARLA_FLORIPA = "carla-floripa"
        const val CARLA_APE = "carla-ape"
        const val CARLA_CHURRASCO = "carla-churrasco"
    }
}
