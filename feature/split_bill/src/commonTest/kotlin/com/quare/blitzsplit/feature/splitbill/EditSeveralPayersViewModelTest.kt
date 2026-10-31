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
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerAmounts
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerSelection
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

// Why: "Editar conta" of a bill several people paid, or whose one payer left the group.
class EditSeveralPayersViewModelTest {
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
    fun `GIVEN a bill Ana and Pierre paid WHEN Editar conta opens THEN each has their amount and others are blank`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            val several = assertNotNull(state.severalPayers)
            assertEquals("", state.payerId)
            assertEquals(
                mapOf(ANA to "200,00", PIERRE to "112,00", BRUNO to "", CARLA to ""),
                several.textByMember(),
            )
            assertTrue(several.rows.all { it.person != null })
            assertTrue(several.addsUp)
            assertEquals(listOf(ANA, PIERRE), state.payerIds)
            assertEquals(listOf(ANA, PIERRE), state.split.payerIds)
            assertNull(state.payerAmounts)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a device that writes a decimal point WHEN Editar conta opens THEN the amounts paid have a point`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(decimalSeparator = '.')

            // When
            val several = viewModel.uiState.value.severalPayers

            // Then
            assertEquals("200.00", several?.textByMember()?.get(ANA))
            assertEquals("112.00", several?.textByMember()?.get(PIERRE))
        }

    @Test
    fun `GIVEN a bill several people paid WHEN tapping Quem pagou THEN Varias pessoas is marked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)

            // Then
            assertEquals(PayerSelection(memberId = null), viewModel.uiState.value.payerSelection)
        }

    @Test
    fun `GIVEN a bill several people paid WHEN tapping Varias pessoas THEN opens with what each one paid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // Then
            val amounts = assertNotNull(viewModel.uiState.value.payerAmounts)
            assertEquals(
                mapOf(ANA to "200,00", PIERRE to "112,00", BRUNO to "", CARLA to ""),
                amounts.textByMember(),
            )
            assertNull(viewModel.uiState.value.payerSelection)
        }

    @Test
    fun `GIVEN someone joined after the bill WHEN Editar conta opens THEN they have a blank row`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(members = trip.members + diego))

            // When
            val several = viewModel.uiState.value.severalPayers

            // Then
            assertEquals("", several?.textByMember()?.get(DIEGO))
            assertEquals(
                DIEGO,
                several
                    ?.rows
                    ?.last()
                    ?.person
                    ?.id,
            )
            assertTrue(several?.addsUp == true)
        }

    @Test
    fun `GIVEN a bill partly paid by someone who left WHEN Editar conta opens THEN they stay as a former member`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = billPaidWithFormer)

            // When
            val state = viewModel.uiState.value

            // Then
            val several = assertNotNull(state.severalPayers)
            val formerRow = several.rows.single { it.memberId == FORMER }
            assertNull(formerRow.person)
            assertEquals("112,00", formerRow.amountText)
            assertEquals(listOf(ANA, PIERRE, BRUNO, CARLA, FORMER), several.rows.map { it.memberId })
            assertTrue(several.addsUp)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a bill partly paid by someone who left WHEN saving another change THEN they still paid their part`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = billPaidWithFormer)
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val saved = expenses.updatedExpenses.single()
            assertEquals("Feira", saved.description)
            assertEquals(listOf(ExpensePayer(ANA, 20_000L), ExpensePayer(FORMER, 11_200L)), saved.payers)
        }

    @Test
    fun `GIVEN a bill several people paid WHEN one of them leaves the group THEN they stay as a former member`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            groups.groups.value = listOf(trip.copy(members = listOf(pierre, bruno, carla)))

            // Then
            val state = viewModel.uiState.value
            val several = assertNotNull(state.severalPayers)
            assertNull(several.rows.single { it.memberId == ANA }.person)
            assertEquals(listOf(PIERRE, BRUNO, CARLA, ANA), several.rows.map { it.memberId })
            assertEquals(listOf(PIERRE, ANA), state.payerIds)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN Varias pessoas open while editing WHEN someone who paid leaves the group THEN their row stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // When
            groups.groups.value = listOf(trip.copy(members = listOf(pierre, bruno, carla)))

            // Then
            val amounts = assertNotNull(viewModel.uiState.value.payerAmounts)
            val anaRow = amounts.rows.single { it.memberId == ANA }
            assertNull(anaRow.person)
            assertEquals("200,00", anaRow.amountText)
            assertTrue(amounts.addsUp)
        }

    @Test
    fun `GIVEN Varias pessoas open while editing WHEN a member joins the group THEN they come in blank`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // When
            groups.groups.value = listOf(trip.copy(members = trip.members + diego))

            // Then
            val amounts = assertNotNull(viewModel.uiState.value.payerAmounts)
            assertEquals("", amounts.textByMember()[DIEGO])
            assertTrue(amounts.addsUp)
        }

    @Test
    fun `GIVEN a bill several people paid WHEN changing the amounts and saving THEN updates who paid what`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPayerAmountChanged(ANA, "100,00"))
            viewModel.onEvent(SplitBillUiEvent.OnPayerAmountChanged(BRUNO, "100,00"))
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                listOf(ExpensePayer(ANA, 10_000L), ExpensePayer(PIERRE, 11_200L), ExpensePayer(BRUNO, 10_000L)),
                expenses.updatedExpenses.single().payers,
            )
            assertTrue(expenses.createdExpenses.isEmpty())
        }

    @Test
    fun `GIVEN a bill several people paid WHEN picking one member and saving THEN they paid it all`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(BRUNO))
            viewModel.onEvent(SplitBillUiEvent.OnPayerDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(listOf(ExpensePayer(BRUNO, 31_200L)), expenses.updatedExpenses.single().payers)
        }

    @Test
    fun `GIVEN a bill whose one payer left WHEN tapping Varias pessoas THEN a former member row holds the total`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = billPaidByFormer)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // Then
            val amounts = assertNotNull(viewModel.uiState.value.payerAmounts)
            assertEquals(
                mapOf(ANA to "", PIERRE to "", BRUNO to "", CARLA to "", FORMER to "312,00"),
                amounts.textByMember(),
            )
            assertNull(amounts.rows.last().person)
            assertTrue(amounts.addsUp)
        }

    @Test
    fun `GIVEN the former member still holding the total WHEN tapping Pronto THEN they remain the only payer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = billPaidByFormer)
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersDone)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.payerAmounts)
            assertNull(state.severalPayers)
            assertEquals(FORMER, state.payerId)
            assertTrue(state.isPayerFormerMember)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a bill whose one payer left WHEN sharing it with Ana and saving THEN both paid their part`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = billPaidByFormer)
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPayerAmountChanged(ANA, "112,00"))
            viewModel.onEvent(SplitBillUiEvent.OnPayerAmountChanged(FORMER, "200,00"))
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                listOf(ExpensePayer(ANA, 11_200L), ExpensePayer(FORMER, 20_000L)),
                expenses.updatedExpenses.single().payers,
            )
        }

    private fun PayerAmounts.textByMember(): Map<String, String> = rows.associate { it.memberId to it.amountText }

    private val route = Route.SplitBill(GROUP_ID, EXPENSE_ID)

    private val yesterdayMillis = NOW_MILLIS - 20.hours.inWholeMilliseconds

    // Why: "Mercado", R$ 312,00 split equally among Ana, Pierre and Bruno, synced, saved by Ana.
    private fun expense(payers: List<ExpensePayer>): Expense = Expense(
        id = EXPENSE_ID,
        groupId = GROUP_ID,
        description = "Mercado",
        amountMinor = 31_200L,
        splitMode = SplitMode.EQUAL,
        payers = payers,
        shares = listOf(
            ExpenseShare(ANA, 10_400L, weight = 1L),
            ExpenseShare(PIERRE, 10_400L, weight = 1L),
            ExpenseShare(BRUNO, 10_400L, weight = 1L),
        ),
        spentAt = yesterdayMillis,
        isPending = false,
        updatedAt = SAVED_VERSION,
        updatedByMemberId = ANA,
    )

    // Why: Ana paid R$ 200,00 and Pierre R$ 112,00.
    private val billPaidTogether = expense(listOf(ExpensePayer(ANA, 20_000L), ExpensePayer(PIERRE, 11_200L)))

    private val billPaidWithFormer = expense(listOf(ExpensePayer(ANA, 20_000L), ExpensePayer(FORMER, 11_200L)))

    private val billPaidByFormer = expense(listOf(ExpensePayer(FORMER, 31_200L)))

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
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        currency = GroupCurrency.BRL,
        members = listOf(ana, pierre, bruno, carla),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
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

    private fun prepareScenario(
        storedExpense: Expense = billPaidTogether,
        storedGroup: Group = trip,
        decimalSeparator: Char = ',',
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup))
        expenses = FakeExpenseRepository(initialExpenses = listOf(storedExpense))
        val navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.GroupDetail(GROUP_ID))
            it.navigate(Route.BillDetail(GROUP_ID, EXPENSE_ID))
            it.navigate(route)
        }
        val deletedBills = InMemoryDeletedBillRepository()
        viewModel = SplitBillViewModel(
            route = route,
            navigator = navigator,
            createExpense = CreateExpenseUseCase(expenses) { NOW_MILLIS },
            updateExpense = UpdateExpenseUseCase(expenses) { NOW_MILLIS },
            deleteExpense = DeleteExpenseUseCase(expenses, deletedBills) { NOW_MILLIS },
            noticeBillGone = NoticeBillGoneUseCase(deletedBills),
            currentTimeMillis = { NOW_MILLIS },
            currentTimeZone = { TimeZone.UTC },
            decimalSeparator = { decimalSeparator },
            observeGroup = ObserveGroupUseCase(groups),
            observeExpense = ObserveExpenseUseCase(expenses),
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val EXPENSE_ID = "expense-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
        const val CARLA = "member-4"
        const val DIEGO = "member-5"
        const val FORMER = "member-9"

        // Why: 2026-10-30T15:00:00Z.
        const val NOW_MILLIS = 1_793_372_400_000L
        const val SAVED_VERSION = 1_793_300_000_000L
    }
}
