package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CreateExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.UpdateExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillGroup
import com.quare.blitzsplit.feature.splitbill.presentation.model.GroupSelection
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

// Why: "Rachar em qual grupo?" (7.1i), first from the home's "Rachar conta", then from the group's chip of a new bill.
class PickGroupViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var navigator: BackStackNavigator
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
    fun `GIVEN several groups WHEN Rachar conta opens from the home THEN asks which group with the recent marked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(screen = fromHome)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GroupSelection(groupId = TRIP_ID), state.groupSelection)
            assertEquals(
                BillGroup(TRIP_ID, "Viagem Floripa", "VF", AvatarColor.GREEN, memberCount = 3),
                state.groups.first(),
            )
            assertEquals(listOf(TRIP_ID, HOME_ID), state.groups.map { it.id })
            assertEquals(listOf(3, 2), state.groups.map { it.memberCount })
            assertEquals(TRIP_ID, state.groupId)
            assertEquals("Viagem Floripa", state.groupName)
            assertTrue(state.canChangeGroup)
        }

    @Test
    fun `GIVEN a single group WHEN Rachar conta opens from the home THEN goes straight to the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = fromHome,
                storedGroups = listOf(trip),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.groupSelection)
            assertFalse(state.canChangeGroup)
            assertEquals("Viagem Floripa", state.groupName)
        }

    @Test
    fun `GIVEN a single group from the home WHEN a sync brings another group THEN does not ask which group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = fromHome,
                storedGroups = listOf(trip),
            )

            // When
            groups.groups.value = listOf(trip, home)

            // Then
            assertNull(viewModel.uiState.value.groupSelection)
            assertTrue(viewModel.uiState.value.canChangeGroup)
        }

    @Test
    fun `GIVEN Rachar conta from the group WHEN it opens THEN does not ask which group`() = runTest(testDispatcher) {
        // Given
        prepareScenario(screen = fromGroup)

        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.groupSelection)
        assertTrue(state.canChangeGroup)
    }

    @Test
    fun `GIVEN a new bill WHEN tapping its group THEN asks which group with its own marked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(screen = fromGroup)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)

            // Then
            assertEquals(GroupSelection(groupId = TRIP_ID), viewModel.uiState.value.groupSelection)
        }

    @Test
    fun `GIVEN the question open WHEN picking another group THEN marks it and the bill stays in its group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(screen = fromHome)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(HOME_ID))

            // Then
            assertEquals(GroupSelection(groupId = HOME_ID), viewModel.uiState.value.groupSelection)
            assertEquals(TRIP_ID, viewModel.uiState.value.groupId)
        }

    @Test
    fun `GIVEN the question open WHEN picking a group not on the device THEN keeps the mark`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(screen = fromHome)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked("gone"))

            // Then
            assertEquals(GroupSelection(groupId = TRIP_ID), viewModel.uiState.value.groupSelection)
        }

    @Test
    fun `GIVEN another group marked WHEN tapping Continuar THEN the bill goes to that group with its people`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(screen = fromHome)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(HOME_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.groupSelection)
            assertEquals(HOME_ID, state.groupId)
            assertEquals("Apê 302", state.groupName)
            assertEquals(GroupCurrency.USD, state.currency)
            assertEquals(listOf(PIERRE_HOME, CARLA), state.people.map { it.id })
            assertEquals(PIERRE_HOME, state.payerId)
            assertEquals(listOf(PIERRE_HOME, CARLA), state.split.included.map { it.id })
        }

    @Test
    fun `GIVEN a bill filled in WHEN moving it to another group THEN keeps what it was and how much`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(screen = fromGroup)
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("90"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado"))
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(HOME_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            val state = viewModel.uiState.value
            assertEquals("90", state.amountText)
            assertEquals("Mercado", state.description)
            assertEquals(9_000L, state.split.totalMinor)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN someone else paying split by parts WHEN moving the bill to another group THEN starts over equally`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(screen = fromGroup)
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("90"))
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(ANA))
            viewModel.onEvent(SplitBillUiEvent.OnPayerDone)
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(HOME_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            val state = viewModel.uiState.value
            assertEquals(PIERRE_HOME, state.payerId)
            assertEquals(SplitMode.EQUAL, state.split.mode)
            assertEquals(listOf(4_500L, 4_500L), state.split.shares?.map { it.amountMinor })
        }

    @Test
    fun `GIVEN another group marked WHEN closing the question THEN the bill stays in its group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(screen = fromHome)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(HOME_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDismissed)

            // Then
            assertNull(viewModel.uiState.value.groupSelection)
            assertEquals(TRIP_ID, viewModel.uiState.value.groupId)
            assertEquals("Viagem Floripa", viewModel.uiState.value.groupName)
        }

    @Test
    fun `GIVEN another group marked WHEN a sync takes it off the device THEN the mark goes back to the bills group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(screen = fromHome)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(HOME_ID))

            // When
            groups.groups.value = listOf(trip, soccer)

            // Then
            assertEquals(GroupSelection(groupId = TRIP_ID), viewModel.uiState.value.groupSelection)
        }

    @Test
    fun `GIVEN a bill moved to another group WHEN a sync deletes that group THEN goes back to its own group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = fromGroup,
                storedGroups = listOf(trip, home, soccer),
            )
            fillIn()
            viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(YESTERDAY))
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(HOME_ID))
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(CARLA))
            viewModel.onEvent(SplitBillUiEvent.OnPayerDone)

            // When
            groups.groups.value = listOf(trip, soccer)

            // Then
            val state = viewModel.uiState.value
            assertEquals(fromGroup, navigator.backStack.value.last())
            assertEquals(TRIP_ID, state.groupId)
            assertEquals("Viagem Floripa", state.groupName)
            assertEquals("90", state.amountText)
            assertEquals("Mercado", state.description)
            assertEquals(YESTERDAY, state.spentAt)
            assertEquals(PIERRE_TRIP, state.payerId)
            assertEquals(listOf(ANA, PIERRE_TRIP, BRUNO), state.split.included.map { it.id })
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a bill moved to another group WHEN a sync deletes both groups THEN closes`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            screen = fromGroup,
            storedGroups = listOf(trip, home, soccer),
        )
        viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
        viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(HOME_ID))
        viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

        // When
        groups.groups.value = listOf(soccer)

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail(TRIP_ID)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a bill being edited WHEN tapping its group THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(screen = Route.SplitBill(TRIP_ID, "bill-1", picksGroupFirst = false))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)

        // Then
        assertNull(viewModel.uiState.value.groupSelection)
        assertTrue(
            viewModel.uiState.value.groups
                .isEmpty(),
        )
        assertFalse(viewModel.uiState.value.canChangeGroup)
    }

    @Test
    fun `GIVEN the bill from the home WHEN saving THEN opens Despesas of its group in its place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(screen = fromHome)
            viewModel.onEvent(SplitBillUiEvent.OnGroupDismissed)
            fillIn()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(TRIP_ID, expenses.createdExpenses.single().groupId)
            assertEquals(listOf(Route.Groups, Route.GroupDetail(TRIP_ID)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the bill moved to another group from the home WHEN saving THEN saves it there and opens that group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(screen = fromHome)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(HOME_ID))
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)
            fillIn()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val saved = expenses.createdExpenses.single()
            assertEquals(HOME_ID, saved.groupId)
            assertEquals(listOf(PIERRE_HOME), saved.payers.map { it.memberId })
            assertEquals(listOf(Route.Groups, Route.GroupDetail(HOME_ID)), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the bill moved to another group from a group WHEN saving THEN opens the other group over it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(screen = fromGroup)
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(HOME_ID))
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)
            fillIn()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                listOf(Route.Groups, Route.GroupDetail(TRIP_ID), Route.GroupDetail(HOME_ID)),
                navigator.backStack.value,
            )
        }

    private fun fillIn() {
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("90"))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado"))
    }

    private val fromHome = Route.SplitBill(
        TRIP_ID,
        expenseId = null,
        picksGroupFirst = true,
    )

    private val fromGroup = Route.SplitBill(
        TRIP_ID,
        expenseId = null,
        picksGroupFirst = false,
    )

    private val trip = group(
        id = TRIP_ID,
        name = "Viagem Floripa",
        color = AvatarColor.GREEN,
        currency = GroupCurrency.BRL,
        members = listOf(
            member(ANA, "Ana"),
            member(PIERRE_TRIP, "Pierre", isMe = true),
            member(BRUNO, "Bruno"),
        ),
    )

    private val home = group(
        id = HOME_ID,
        name = "Apê 302",
        color = AvatarColor.BLUE,
        currency = GroupCurrency.USD,
        members = listOf(
            member(PIERRE_HOME, "Pierre", isMe = true),
            member(CARLA, "Carla"),
        ),
    )

    private val soccer = group(
        id = "soccer",
        name = "Futebol de quinta",
        color = AvatarColor.SAND,
        currency = GroupCurrency.BRL,
        members = listOf(member("soccer-me", "Pierre", isMe = true)),
    )

    private fun group(
        id: String,
        name: String,
        color: AvatarColor,
        currency: GroupCurrency,
        members: List<GroupMember>,
    ): Group = Group(
        id = id,
        name = name,
        type = GroupType.TRIP,
        color = color,
        currency = currency,
        members = members,
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        updatedAt = 0L,
    )

    private fun member(
        id: String,
        name: String,
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.PEACH,
        isMe = isMe,
        hasJoined = true,
        inviteCode = null,
    )

    private fun prepareScenario(
        screen: Route.SplitBill,
        storedGroups: List<Group> = listOf(trip, home),
    ) {
        groups = FakeGroupRepository(initialGroups = storedGroups)
        expenses = FakeExpenseRepository()
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            if (!screen.picksGroupFirst) it.navigate(Route.GroupDetail(screen.groupId))
            it.navigate(screen)
        }
        val deletedBills = InMemoryDeletedBillRepository()
        viewModel = SplitBillViewModel(
            route = screen,
            navigator = navigator,
            createExpense = CreateExpenseUseCase(expenses) { NOW },
            updateExpense = UpdateExpenseUseCase(expenses) { NOW },
            deleteExpense = DeleteExpenseUseCase(expenses, deletedBills) { NOW },
            noticeBillGone = NoticeBillGoneUseCase(deletedBills),
            currentTimeMillis = { NOW },
            currentTimeZone = { TimeZone.UTC },
            decimalSeparator = { ',' },
            observeGroup = ObserveGroupUseCase(groups),
            observeExpense = ObserveExpenseUseCase(expenses),
            observeGroupsByActivity = { groups.groups },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
        )
    }

    private companion object {
        const val NOW = 1_761_836_400_000L
        const val YESTERDAY = NOW - 86_400_000L
        const val TRIP_ID = "trip"
        const val HOME_ID = "home"
        const val ANA = "ana"
        const val PIERRE_TRIP = "pierre-trip"
        const val BRUNO = "bruno"
        const val PIERRE_HOME = "pierre-home"
        const val CARLA = "carla"
    }
}
