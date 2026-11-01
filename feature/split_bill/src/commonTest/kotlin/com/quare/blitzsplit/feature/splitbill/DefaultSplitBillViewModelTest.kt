package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CreateExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.UpdateExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitFact
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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

// Why: a new bill opens with the group's "Divisão padrão" (6.7, 7.1j, 7.2f), which Dividir may change for it (7.2h).
class DefaultSplitBillViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
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
    fun `GIVEN a group splitting 3 to 1 WHEN a new bill opens THEN it splits by parts and says it is the group's default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("190,00"))

            // Then
            val state = viewModel.uiState.value
            assertEquals(SplitMode.SHARES, state.split.mode)
            assertEquals(listOf(14_250L, 4_750L), state.split.shares?.map { it.amountMinor })
            assertTrue(state.splitFollowsDefault)
            assertEquals(SplitFact.GroupDefault, state.splitSummary.first())
            assertEquals(listOf(3L, 1L), state.defaultSplit?.values)
        }

    @Test
    fun `GIVEN a group without a default WHEN a new bill opens THEN it splits equally with no word of a default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SplitMode.EQUAL, state.split.mode)
            assertEquals(2, state.split.includedCount)
            assertNull(state.defaultSplit)
            assertFalse(state.splitSummary.contains(SplitFact.GroupDefault))
        }

    @Test
    fun `GIVEN Dividir open on the default WHEN changing the mode THEN it says the bill differs and Voltar ao padrao undoes it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("190,00"))
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            assertTrue(viewModel.uiState.value.showsDefaultSplitNotice)
            assertFalse(viewModel.uiState.value.isSplitEditorOffDefault)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.EQUAL))
            assertTrue(viewModel.uiState.value.isSplitEditorOffDefault)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitResetToDefault)

            // Then
            val editor = assertNotNull(viewModel.uiState.value.splitEditor)
            assertEquals(SplitMode.SHARES, editor.mode)
            assertEquals(19_000L, editor.totalMinor)
            assertFalse(viewModel.uiState.value.isSplitEditorOffDefault)
        }

    @Test
    fun `GIVEN Dividir changed the bill WHEN the default changes under it THEN the bill keeps its own split`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("190,00"))
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.EQUAL))
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // When
            groups.groups.value = listOf(home.copy(defaultSplit = threeToOne.copy(weights = mapOf(PIERRE to 1L, ANA to 2L))))

            // Then
            assertEquals(SplitMode.EQUAL, viewModel.uiState.value.split.mode)
            assertFalse(viewModel.uiState.value.splitFollowsDefault)
        }

    @Test
    fun `GIVEN a new bill on the default WHEN a sync changes the default THEN the bill follows the new one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home)

            // When
            groups.groups.value = listOf(home.copy(defaultSplit = threeToOne))

            // Then
            assertEquals(SplitMode.SHARES, viewModel.uiState.value.split.mode)
            assertTrue(viewModel.uiState.value.splitFollowsDefault)
        }

    @Test
    fun `GIVEN someone who joined after the default WHEN a new bill opens THEN they have 1 part`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne, members = home.members + carla))

        // When
        val rows = viewModel.uiState.value.split.rows

        // Then
        assertEquals(listOf(3L, 1L, 1L), rows.map { it.parts })
    }

    @Test
    fun `GIVEN Pela renda WHEN a new bill opens THEN it splits by the percentages`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = home.copy(defaultSplit = GroupDefaultSplit(DefaultSplitMode.INCOME, mapOf(PIERRE to 75L, ANA to 25L))))

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(SplitMode.PERCENT, state.split.mode)
        assertEquals(listOf("75", "25"), state.split.rows.map { it.valueText })
        assertEquals(DefaultSplitMode.INCOME, state.defaultSplit?.mode)
    }

    @Test
    fun `GIVEN percentages that lost someone who left WHEN a new bill opens THEN it splits equally among everyone`() =
        runTest(testDispatcher) {
            // Given
            val percent = GroupDefaultSplit(DefaultSplitMode.PERCENT, mapOf(PIERRE to 50L, ANA to 25L, CARLA to 25L))
            prepareScenario(storedGroup = home.copy(defaultSplit = percent))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SplitMode.EQUAL, state.split.mode)
            assertNull(state.defaultSplit)
        }

    @Test
    fun `GIVEN Igual without Ana by default WHEN a new bill opens THEN Ana is out of it and named as left out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = GroupDefaultSplit(DefaultSplitMode.EQUAL, mapOf(PIERRE to 1L, ANA to 0L))))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(true, false), state.split.rows.map { it.isIncluded })
            assertEquals(listOf(ANA), state.defaultSplit?.excluded?.map { it.id })
        }

    @Test
    fun `GIVEN a saved bill WHEN editing it in a group with a default THEN it keeps its split with no notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne), expenseId = BILL_ID)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals(SplitMode.EQUAL, state.split.mode)
            assertNull(state.defaultSplit)
            assertFalse(state.showsDefaultSplitNotice)
        }

    private val pierre = GroupMember(
        id = PIERRE,
        name = "Pierre",
        avatarColor = AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
        userId = null,
        inviteCode = null,
    )
    private val ana = pierre.copy(
        id = ANA,
        name = "Ana",
        avatarColor = AvatarColor.GREEN,
        isMe = false,
    )
    private val carla = ana.copy(
        id = CARLA,
        name = "Carla",
        hasJoined = false,
    )

    private val threeToOne = GroupDefaultSplit(
        mode = DefaultSplitMode.SHARES,
        weights = mapOf(PIERRE to 3L, ANA to 1L),
    )

    private val home = Group(
        id = GROUP_ID,
        name = "Apê 302",
        type = GroupType.HOME,
        color = AvatarColor.BLUE,
        currency = GroupCurrency.BRL,
        members = listOf(pierre, ana),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    private val bill = Expense(
        id = BILL_ID,
        groupId = GROUP_ID,
        description = "Luz",
        amountMinor = 10_000,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(PIERRE, 10_000)),
        shares = listOf(
            ExpenseShare(memberId = PIERRE, amountMinor = 5_000, weight = 1),
            ExpenseShare(memberId = ANA, amountMinor = 5_000, weight = 1),
        ),
        spentAt = 1_000L,
        isPending = false,
        updatedAt = 1_000L,
        updatedByMemberId = null,
    )

    private fun prepareScenario(
        storedGroup: Group,
        expenseId: String? = null,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup))
        expenses = FakeExpenseRepository(initialExpenses = listOf(bill))
        val route = Route.SplitBill(
            GROUP_ID,
            expenseId = expenseId,
            picksGroupFirst = false,
        )
        val navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.GroupDetail(GROUP_ID))
            it.navigate(route)
        }
        val deletedBills = InMemoryDeletedBillRepository()
        viewModel = SplitBillViewModel(
            route = route,
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
            observeExpenses = ObserveExpensesUseCase(expenses),
            observeSettlements = { flowOf(emptyList()) },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val BILL_ID = "bill-1"
        const val PIERRE = "member-1"
        const val ANA = "member-2"
        const val CARLA = "member-3"
        const val NOW = 10_000L
    }
}
