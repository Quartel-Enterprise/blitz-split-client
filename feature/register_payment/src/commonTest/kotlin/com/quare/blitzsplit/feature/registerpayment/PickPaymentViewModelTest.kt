package com.quare.blitzsplit.feature.registerpayment

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsByActivity
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsByActivityUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PickPaymentGroup
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PickPaymentUiEvent
import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.PickPaymentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PickPaymentViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: PickPaymentViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the device data not read yet WHEN the picker opens THEN shows neither the pairs nor the empty state`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isRoomRead = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertTrue(state.groups.isEmpty())
            assertFalse(state.showsEmpty)
        }

    @Test
    fun `GIVEN a group with open pairs WHEN the picker opens THEN lists the group under its name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertFalse(state.showsEmpty)
            val group = state.groups.single()
            assertEquals(TRIP, group.groupId)
            assertEquals("Viagem Floripa", group.name)
            assertEquals("VF", group.initials)
            assertEquals(AvatarColor.GREEN, group.color)
            assertEquals(GroupCurrency.BRL, group.currency)
        }

    @Test
    fun `GIVEN pairs with and without you WHEN the picker opens THEN yours come first and the largest first in each`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val pairs = viewModel.uiState.value.groups
                .single()
                .pairs

            // Then
            assertEquals(
                listOf(
                    Triple(PIERRE, ANA, 5_000L),
                    Triple(BRUNO, PIERRE, 3_000L),
                    Triple(DIEGO, BRUNO, 9_000L),
                    Triple(ANA, DIEGO, 1_000L),
                ),
                pairs.map { Triple(it.fromMemberId, it.toMemberId, it.amountMinor) },
            )
        }

    @Test
    fun `GIVEN a pair with you WHEN the picker opens THEN names both people from the group members`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val pair = viewModel.uiState.value.groups
                .single()
                .pairs
                .first()

            // Then
            assertEquals(
                PaymentPerson(
                    id = PIERRE,
                    name = "Pierre Vieira",
                    initials = "P",
                    avatarColor = AvatarColor.BLUE,
                    isMe = true,
                ),
                pair.from,
            )
            assertEquals(
                PaymentPerson(
                    id = ANA,
                    name = "Ana Paula",
                    initials = "AP",
                    avatarColor = AvatarColor.PEACH,
                    isMe = false,
                ),
                pair.to,
            )
        }

    @Test
    fun `GIVEN a debt with someone no longer in the group WHEN the picker opens THEN that person is an ex-member`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill(
                        TRIP,
                        "gone-bill",
                        payer = GONE,
                        debtor = ANA,
                        2_000L,
                    ),
                ),
            )

            // When
            val pair = viewModel.uiState.value.groups
                .single()
                .pairs
                .single()

            // Then
            assertEquals(ANA, pair.from?.id)
            assertNull(pair.to)
            assertEquals(GONE, pair.toMemberId)
            assertEquals(2_000L, pair.amountMinor)
        }

    @Test
    fun `GIVEN two people owing each other in a group WHEN the picker opens THEN shows one pair already netted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill(
                        TRIP,
                        "dinner",
                        payer = ANA,
                        debtor = PIERRE,
                        10_000L,
                    ),
                    bill(
                        TRIP,
                        "taxi",
                        payer = PIERRE,
                        debtor = ANA,
                        3_000L,
                    ),
                ),
            )

            // When
            val pairs = viewModel.uiState.value.groups
                .single()
                .pairs

            // Then
            assertEquals(
                listOf(Triple(PIERRE, ANA, 7_000L)),
                pairs.map { Triple(it.fromMemberId, it.toMemberId, it.amountMinor) },
            )
        }

    @Test
    fun `GIVEN groups used at different times WHEN the picker opens THEN lists the most recently used first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(APE, "Apê 302", updatedAt = 30L),
                    group(TRIP, "Viagem Floripa", updatedAt = 10L),
                ),
                storedExpenses = listOf(
                    bill(
                        APE,
                        "rent",
                        payer = ANA,
                        debtor = PIERRE,
                        1_000L,
                        updatedAt = 20L,
                    ),
                    bill(
                        TRIP,
                        "dinner",
                        payer = ANA,
                        debtor = PIERRE,
                        1_000L,
                        updatedAt = 50L,
                    ),
                ),
            )

            // When
            val groupIds = viewModel.uiState.value.groups
                .map(PickPaymentGroup::groupId)

            // Then
            assertEquals(listOf(TRIP, APE), groupIds)
        }

    @Test
    fun `GIVEN a group without bills and a settled group WHEN the picker opens THEN leaves both out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(
                    group(TRIP, "Viagem Floripa"),
                    group(APE, "Apê 302"),
                    group(BBQ, "Churrasco"),
                ),
                storedExpenses = listOf(
                    bill(
                        TRIP,
                        "dinner",
                        payer = ANA,
                        debtor = PIERRE,
                        1_000L,
                    ),
                    bill(
                        APE,
                        "rent",
                        payer = ANA,
                        debtor = PIERRE,
                        4_000L,
                    ),
                ),
                storedSettlements = listOf(
                    settlement(
                        APE,
                        from = PIERRE,
                        to = ANA,
                        4_000L,
                    ),
                ),
            )

            // When
            val groupIds = viewModel.uiState.value.groups
                .map(PickPaymentGroup::groupId)

            // Then
            assertEquals(listOf(TRIP), groupIds)
        }

    @Test
    fun `GIVEN nobody owes anybody on the device WHEN the picker opens THEN shows the empty state`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpenses = listOf(
                    bill(
                        TRIP,
                        "dinner",
                        payer = ANA,
                        debtor = PIERRE,
                        1_000L,
                    ),
                ),
                storedSettlements = listOf(
                    settlement(
                        TRIP,
                        from = PIERRE,
                        to = ANA,
                        1_000L,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertTrue(state.groups.isEmpty())
            assertTrue(state.showsEmpty)
        }

    @Test
    fun `GIVEN the picker open WHEN a sync brings a payment that settles a pair THEN that pair leaves the list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val payment = settlement(
                TRIP,
                from = PIERRE,
                to = ANA,
                5_000L,
            )

            // When
            settlements.settlements.update { it + payment }

            // Then
            val pairs = viewModel.uiState.value.groups
                .single()
                .pairs
            assertEquals(
                listOf(Triple(BRUNO, PIERRE, 3_000L), Triple(DIEGO, BRUNO, 9_000L), Triple(ANA, DIEGO, 1_000L)),
                pairs.map { Triple(it.fromMemberId, it.toMemberId, it.amountMinor) },
            )
        }

    @Test
    fun `GIVEN the picker open WHEN a sync deletes a group THEN that group leaves the list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(group(TRIP, "Viagem Floripa"), group(APE, "Apê 302")),
                storedExpenses = listOf(
                    bill(
                        TRIP,
                        "dinner",
                        payer = ANA,
                        debtor = PIERRE,
                        1_000L,
                    ),
                    bill(
                        APE,
                        "rent",
                        payer = ANA,
                        debtor = PIERRE,
                        4_000L,
                    ),
                ),
            )

            // When
            groups.groups.update { list -> list.filterNot { it.id == TRIP } }

            // Then
            assertEquals(
                listOf(APE),
                viewModel.uiState.value.groups
                    .map(PickPaymentGroup::groupId),
            )
        }

    @Test
    fun `GIVEN the picker over Inicio WHEN tapping a pair THEN opens Registrar pagamento in its place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val tap = PickPaymentUiEvent.OnPairClicked(
                TRIP,
                fromMemberId = BRUNO,
                toMemberId = PIERRE,
            )

            // When
            viewModel.onEvent(tap)

            // Then
            assertEquals(
                listOf(
                    Route.Home,
                    Route.RegisterPayment(
                        TRIP,
                        fromMemberId = BRUNO,
                        toMemberId = PIERRE,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a sync settled a pair WHEN a late tap on it arrives THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        settlements.settlements.update {
            it + settlement(
                TRIP,
                from = PIERRE,
                to = ANA,
                5_000L,
            )
        }
        val staleTap = PickPaymentUiEvent.OnPairClicked(
            TRIP,
            fromMemberId = PIERRE,
            toMemberId = ANA,
        )

        // When
        viewModel.onEvent(staleTap)

        // Then
        assertEquals(listOf(Route.Home, Route.PickPayment), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a pair the other way round WHEN tapping it THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val reversedTap = PickPaymentUiEvent.OnPairClicked(
            TRIP,
            fromMemberId = ANA,
            toMemberId = PIERRE,
        )

        // When
        viewModel.onEvent(reversedTap)

        // Then
        assertEquals(listOf(Route.Home, Route.PickPayment), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group no longer listed WHEN tapping one of its pairs THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        groups.groups.update { emptyList() }
        val staleTap = PickPaymentUiEvent.OnPairClicked(
            TRIP,
            fromMemberId = PIERRE,
            toMemberId = ANA,
        )

        // When
        viewModel.onEvent(staleTap)

        // Then
        assertEquals(listOf(Route.Home, Route.PickPayment), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen over the picker WHEN tapping a pair THEN opens nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Profile)
        val tap = PickPaymentUiEvent.OnPairClicked(
            TRIP,
            fromMemberId = PIERRE,
            toMemberId = ANA,
        )

        // When
        viewModel.onEvent(tap)

        // Then
        assertEquals(listOf(Route.Home, Route.PickPayment, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the picker over Inicio WHEN dismissing it THEN goes back to Inicio`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val dismiss = PickPaymentUiEvent.OnDismissed

        // When
        viewModel.onEvent(dismiss)

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen over the picker WHEN dismissing it THEN stays where it is`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Profile)
        val dismiss = PickPaymentUiEvent.OnDismissed

        // When
        viewModel.onEvent(dismiss)

        // Then
        assertEquals(listOf(Route.Home, Route.PickPayment, Route.Profile), navigator.backStack.value)
    }

    private val pierre = member(PIERRE, "Pierre Vieira", AvatarColor.BLUE, isMe = true)
    private val ana = member(ANA, "Ana Paula", AvatarColor.PEACH)
    private val bruno = member(BRUNO, "Bruno", AvatarColor.LILAC)
    private val diego = member(DIEGO, "Diego", AvatarColor.SAND)

    /*
     * Why: Pierre owes Ana 50,00 and Bruno owes Pierre 30,00 (with you), Diego owes Bruno 90,00 and Ana owes Diego
     * 10,00 (between the others), so the largest pair is not one of yours.
     */
    private val tripBills = listOf(
        bill(
            TRIP,
            "hotel",
            payer = BRUNO,
            debtor = DIEGO,
            9_000L,
        ),
        bill(
            TRIP,
            "dinner",
            payer = ANA,
            debtor = PIERRE,
            5_000L,
        ),
        bill(
            TRIP,
            "taxi",
            payer = PIERRE,
            debtor = BRUNO,
            3_000L,
        ),
        bill(
            TRIP,
            "ice cream",
            payer = DIEGO,
            debtor = ANA,
            1_000L,
        ),
    )

    private fun member(
        id: String,
        name: String,
        color: AvatarColor,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = color,
        isMe = isMe,
        hasJoined = true,
        userId = null,
        inviteCode = null,
    )

    private fun group(
        id: String,
        name: String,
        updatedAt: Long = 0L,
    ): Group = Group(
        id = id,
        name = name,
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        currency = GroupCurrency.BRL,
        members = listOf(ana, pierre, bruno, diego),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = updatedAt,
    )

    // Why: a bill the payer paid whole for the debtor alone, so the debtor owes the payer all of it.
    private fun bill(
        groupId: String,
        id: String,
        payer: String,
        debtor: String,
        amountMinor: Long,
        updatedAt: Long = 0L,
    ): Expense = Expense(
        id = "$groupId-$id",
        groupId = groupId,
        description = id,
        amountMinor = amountMinor,
        splitMode = SplitMode.AMOUNT,
        payers = listOf(ExpensePayer(payer, amountMinor)),
        shares = listOf(ExpenseShare(debtor, amountMinor, weight = amountMinor)),
        spentAt = 0L,
        isPending = false,
        updatedAt = updatedAt,
        updatedByMemberId = null,
        items = null,
    )

    private fun settlement(
        groupId: String,
        from: String,
        to: String,
        amountMinor: Long,
    ): Settlement = Settlement(
        id = "settlement-$groupId-$from-$to",
        groupId = groupId,
        fromMemberId = from,
        toMemberId = to,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = 0L,
        isPending = false,
        updatedAt = 0L,
    )

    private fun prepareScenario(
        storedGroups: List<Group> = listOf(group(TRIP, "Viagem Floripa")),
        storedExpenses: List<Expense> = tripBills,
        storedSettlements: List<Settlement> = emptyList(),
        isRoomRead: Boolean = true,
    ) {
        groups = FakeGroupRepository(initialGroups = storedGroups)
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        navigator = BackStackNavigator(startRoute = Route.Home).also { it.navigate(Route.PickPayment) }
        val money = ObserveGroupsMoneyUseCase(expenses, settlements)
        val roomNeverAnswers = MutableSharedFlow<List<Group>>()
        viewModel = PickPaymentViewModel(
            navigator = navigator,
            observeGroupsByActivity = if (isRoomRead) {
                ObserveGroupsByActivityUseCase(ObserveGroupsUseCase(groups), money)
            } else {
                ObserveGroupsByActivity { roomNeverAnswers }
            },
            observeGroupsMoney = money,
        )
    }

    private companion object {
        const val TRIP = "trip"
        const val APE = "ape"
        const val BBQ = "bbq"
        const val PIERRE = "member-pierre"
        const val ANA = "member-ana"
        const val BRUNO = "member-bruno"
        const val DIEGO = "member-diego"
        const val GONE = "member-gone"
    }
}
