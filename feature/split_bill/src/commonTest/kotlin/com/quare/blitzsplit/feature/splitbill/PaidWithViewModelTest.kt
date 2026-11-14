package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.CardColor
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReadFailure
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptScan
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CreateExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryMapUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.UpdateExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakePayingMethodRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiAction
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.BillCategoryUseCases
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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

// Why: "Paid with" on "Rachar conta", on a duplicate and on "Editar conta": this device's user's own payment method.
class PaidWithViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var paying: FakePayingMethodRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: SplitBillViewModel
    private val actions = mutableListOf<SplitBillUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a new bill this device user pays and methods saved WHEN the form opens THEN Paid with shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsPaidWith)
            assertEquals(listOf(nubank, cash), state.paidWith.methods)
        }

    @Test
    fun `GIVEN no method saved WHEN the form opens THEN Paid with is hidden`() = runTest(testDispatcher) {
        // Given
        prepareScenario(methods = emptyList())

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.showsPaidWith)
    }

    @Test
    fun `GIVEN a new bill WHEN someone else is picked as the payer THEN Paid with is hidden`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            pickPayer(ANA)

            // Then
            assertFalse(viewModel.uiState.value.showsPaidWith)
        }

    @Test
    fun `GIVEN a new bill WHEN this device user is one of several payers THEN Paid with shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")

            // When
            payTogether(ANA to "200,00", PIERRE to "112,00")

            // Then
            assertEquals(listOf(ANA, PIERRE), viewModel.uiState.value.payerIds)
            assertTrue(viewModel.uiState.value.showsPaidWith)
        }

    @Test
    fun `GIVEN a new bill WHEN several others pay it without this device user THEN Paid with is hidden`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")

            // When
            payTogether(ANA to "200,00", BRUNO to "112,00")

            // Then
            assertFalse(viewModel.uiState.value.showsPaidWith)
        }

    @Test
    fun `GIVEN a default method WHEN a new bill opens THEN it is picked and is no unsaved input`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(NUBANK, state.paidWith.methodId)
            assertEquals(nubank, state.paidWith.method)
            assertFalse(state.hasUnsavedInput)
        }

    @Test
    fun `GIVEN no default method WHEN a new bill opens THEN none is picked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(methods = listOf(nubank.copy(isDefault = false), cash))

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsPaidWith)
        assertNull(state.paidWith.methodId)
        assertNull(state.paidWith.method)
    }

    @Test
    fun `GIVEN the original bill marked with cash WHEN its duplicate opens THEN cash is picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = duplicate,
                marks = mapOf(EXPENSE_ID to CASH),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(CASH, state.paidWith.methodId)
            assertFalse(state.hasUnsavedInput)
        }

    @Test
    fun `GIVEN the original bill not marked WHEN its duplicate opens THEN the default method is picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(screen = duplicate)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(NUBANK, state.paidWith.methodId)
            assertFalse(state.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a saved bill marked with cash WHEN Editar conta opens THEN cash is picked and is no unsaved input`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = edit,
                marks = mapOf(EXPENSE_ID to CASH),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsPaidWith)
            assertEquals(CASH, state.paidWith.methodId)
            assertFalse(state.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a saved bill not marked WHEN Editar conta opens THEN none is picked instead of the default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(screen = edit)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsPaidWith)
            assertNull(state.paidWith.methodId)
            assertFalse(state.hasUnsavedInput)
        }

    @Test
    fun `GIVEN cash picked WHEN a method is added meanwhile THEN the list shows it and cash stays picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnPaidWithPicked(CASH))

            // When
            paying.methods.value = listOf(nubank, cash, pix)

            // Then
            assertEquals(listOf(nubank, cash, pix), viewModel.uiState.value.paidWith.methods)
            assertEquals(CASH, viewModel.uiState.value.paidWith.methodId)
        }

    @Test
    fun `GIVEN the default picked WHEN another method is deleted meanwhile THEN the default stays picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            paying.methods.value = listOf(nubank)

            // Then
            assertEquals(listOf(nubank), viewModel.uiState.value.paidWith.methods)
            assertEquals(nubank, viewModel.uiState.value.paidWith.method)
        }

    @Test
    fun `GIVEN cash picked WHEN cash is deleted meanwhile THEN Paid with reads as none`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnPaidWithPicked(CASH))

        // When
        paying.methods.value = listOf(nubank)

        // Then
        assertNull(viewModel.uiState.value.paidWith.method)
    }

    @Test
    fun `GIVEN the default picked WHEN picking cash THEN cash is picked and the pick is tracked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPaidWithPicked(CASH))

            // Then
            assertEquals(cash, viewModel.uiState.value.paidWith.method)
            assertEquals(
                listOf("split_bill_paid_with_picked" to mapOf<String, Any>("has_method" to true)),
                paidWithEvents(),
            )
        }

    @Test
    fun `GIVEN the default picked WHEN picking none THEN none is picked and the pick is tracked without a method`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPaidWithPicked(null))

            // Then
            assertNull(viewModel.uiState.value.paidWith.methodId)
            assertEquals(
                listOf("split_bill_paid_with_picked" to mapOf<String, Any>("has_method" to false)),
                paidWithEvents(),
            )
        }

    @Test
    fun `GIVEN Paid with hidden as someone else pays WHEN picking cash THEN nothing changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            pickPayer(ANA)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPaidWithPicked(CASH))

            // Then
            assertEquals(NUBANK, viewModel.uiState.value.paidWith.methodId)
            assertTrue(paidWithEvents().isEmpty())
        }

    @Test
    fun `GIVEN the default picked WHEN picking it again THEN nothing is tracked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPaidWithPicked(NUBANK))

        // Then
        assertEquals(NUBANK, viewModel.uiState.value.paidWith.methodId)
        assertTrue(paidWithEvents().isEmpty())
    }

    @Test
    fun `GIVEN the default picked WHEN picking a method that is not saved THEN nothing changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPaidWithPicked("method-unknown"))

            // Then
            assertEquals(NUBANK, viewModel.uiState.value.paidWith.methodId)
            assertTrue(paidWithEvents().isEmpty())
        }

    @Test
    fun `GIVEN a saved bill opened WHEN picking another method THEN closing asks to discard changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = edit,
                marks = mapOf(EXPENSE_ID to NUBANK),
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPaidWithPicked(CASH))

            // Then
            assertTrue(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN cash picked on a new bill WHEN tapping Salvar THEN the new bill is marked with cash`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            fillIn()
            viewModel.onEvent(SplitBillUiEvent.OnPaidWithPicked(CASH))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val created = expenses.createdExpenses.single()
            assertEquals(mapOf(created.id to CASH), paying.marks.value)
        }

    @Test
    fun `GIVEN the default left picked on a new bill WHEN tapping Salvar THEN the new bill is marked with it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            fillIn()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val created = expenses.createdExpenses.single()
            assertEquals(mapOf(created.id to NUBANK), paying.marks.value)
        }

    @Test
    fun `GIVEN a new bill someone else pays WHEN tapping Salvar THEN no mark is saved`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        fillIn()
        pickPayer(ANA)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        assertEquals(1, expenses.createdExpenses.size)
        assertTrue(paying.marks.value.isEmpty())
    }

    @Test
    fun `GIVEN a new bill and no method saved WHEN tapping Salvar THEN no mark is saved`() = runTest(testDispatcher) {
        // Given
        prepareScenario(methods = emptyList())
        fillIn()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        assertEquals(1, expenses.createdExpenses.size)
        assertTrue(paying.marks.value.isEmpty())
    }

    @Test
    fun `GIVEN a saved bill edited and cash picked WHEN tapping Salvar THEN saves the bill and marks it with cash`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = edit,
                marks = mapOf(EXPENSE_ID to NUBANK),
            )
            describe("Feira")
            viewModel.onEvent(SplitBillUiEvent.OnPaidWithPicked(CASH))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals("Feira", expenses.updatedExpenses.single().description)
            assertEquals(mapOf(EXPENSE_ID to CASH), paying.marks.value)
        }

    @Test
    fun `GIVEN only Paid with changed on a saved bill WHEN tapping Salvar THEN marks it and leaves the bill alone`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = edit,
                marks = mapOf(EXPENSE_ID to NUBANK),
            )
            viewModel.onEvent(SplitBillUiEvent.OnPaidWithPicked(CASH))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(mapOf(EXPENSE_ID to CASH), paying.marks.value)
            assertTrue(expenses.updatedExpenses.isEmpty())
            assertEquals(pierreBill, expenses.expenses.value.single())
        }

    @Test
    fun `GIVEN only Paid with changed on a saved bill WHEN tapping Salvar THEN goes back to Conta`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = edit,
                marks = mapOf(EXPENSE_ID to NUBANK),
            )
            viewModel.onEvent(SplitBillUiEvent.OnPaidWithPicked(CASH))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(withBill, navigator.backStack.value)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN a saved bill marked and left as it was WHEN tapping Salvar THEN saves the bill and keeps the mark`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = edit,
                marks = mapOf(EXPENSE_ID to CASH),
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(pierreBill.payers, expenses.updatedExpenses.single().payers)
            assertEquals(mapOf(EXPENSE_ID to CASH), paying.marks.value)
        }

    @Test
    fun `GIVEN a saved bill marked and someone else made the payer WHEN tapping Salvar THEN the mark is cleared`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = edit,
                marks = mapOf(EXPENSE_ID to CASH),
            )
            pickPayer(ANA)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(listOf(ExpensePayer(ANA, 31_200L)), expenses.updatedExpenses.single().payers)
            assertTrue(paying.marks.value.isEmpty())
        }

    @Test
    fun `GIVEN a saved bill someone else pays and never marked WHEN tapping Salvar THEN no mark is saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = edit,
                storedExpense = anaBill,
            )
            describe("Feira")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(1, expenses.updatedExpenses.size)
            assertTrue(paying.marks.value.isEmpty())
        }

    @Test
    fun `GIVEN the device fails to save the edit WHEN tapping Salvar THEN the mark stays as it was`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = edit,
                marks = mapOf(EXPENSE_ID to NUBANK),
                updateError = IllegalStateException("disk full"),
            )
            describe("Feira")
            viewModel.onEvent(SplitBillUiEvent.OnPaidWithPicked(CASH))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(mapOf(EXPENSE_ID to NUBANK), paying.marks.value)
            assertEquals(listOf<SplitBillUiAction>(SplitBillUiAction.ShowSaveError), actions)
        }

    private fun paidWithEvents(): List<Pair<String, Map<String, Any>>> =
        trackedEvents.filter { it.first == "split_bill_paid_with_picked" }

    private fun typeAmount(text: String) {
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged(""))
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged(text))
    }

    private fun describe(text: String) {
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged(text))
    }

    private fun fillIn() {
        typeAmount("312,00")
        describe("Mercado")
    }

    private fun pickPayer(memberId: String) {
        viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
        viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(memberId))
        viewModel.onEvent(SplitBillUiEvent.OnPayerDone)
    }

    // Why: "Várias pessoas pagaram" with what each one paid, then "Pronto".
    private fun payTogether(vararg paid: Pair<String, String>) {
        viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)
        val typed = paid.toMap()
        viewModel.uiState.value.payerAmounts
            ?.rows
            .orEmpty()
            .forEach { row ->
                viewModel.onEvent(SplitBillUiEvent.OnPayerAmountChanged(row.memberId, typed[row.memberId].orEmpty()))
            }
        viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersDone)
    }

    private val newBill = Route.SplitBill(
        GROUP_ID,
        expenseId = null,
        picksGroupFirst = false,
        scansReceipt = false,
        duplicatesExpenseId = null,
    )

    private val duplicate = newBill.copy(duplicatesExpenseId = EXPENSE_ID)

    private val edit = newBill.copy(expenseId = EXPENSE_ID)

    private val groupStack = listOf(
        Route.Groups,
        Route.GroupDetail(
            GROUP_ID,
            joinedAs = null,
            tab = GroupTab.EXPENSES,
            isOverBill = false,
        ),
    )

    private val withBill = groupStack + Route.BillDetail(GROUP_ID, EXPENSE_ID, focusCommentId = null)

    private val nubank = PayingMethod(
        id = NUBANK,
        type = PayingMethodType.CREDIT_CARD,
        name = "Nubank",
        cardColor = CardColor.PURPLE,
        isDefault = true,
        addedAt = 1L,
    )

    private val cash = PayingMethod(
        id = CASH,
        type = PayingMethodType.CASH,
        name = "",
        cardColor = null,
        isDefault = false,
        addedAt = 2L,
    )

    private val pix = PayingMethod(
        id = PIX,
        type = PayingMethodType.PIX,
        name = "",
        cardColor = null,
        isDefault = false,
        addedAt = 3L,
    )

    // Why: "Mercado", R$ 312,00 paid by one person, split equally among Ana, Pierre and Bruno, synced.
    private fun expense(payerId: String): Expense = Expense(
        id = EXPENSE_ID,
        groupId = GROUP_ID,
        description = "Mercado",
        currency = GroupCurrency.BRL,
        amountMinor = 31_200L,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(payerId, 31_200L)),
        shares = listOf(
            ExpenseShare(ANA, 10_400L, weight = 1L),
            ExpenseShare(PIERRE, 10_400L, weight = 1L),
            ExpenseShare(BRUNO, 10_400L, weight = 1L),
        ),
        items = null,
        categoryId = null,
        spentAt = SPENT_AT,
        isPending = false,
        updatedAt = SAVED_VERSION,
        updatedByMemberId = ANA,
    )

    private val pierreBill = expense(PIERRE)

    private val anaBill = expense(ANA)

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
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
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    private fun member(
        id: String,
        name: String,
        isMe: Boolean,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = true,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun TestScope.prepareScenario(
        screen: Route.SplitBill = newBill,
        storedExpense: Expense = pierreBill,
        methods: List<PayingMethod> = listOf(nubank, cash),
        marks: Map<String, String> = emptyMap(),
        updateError: Throwable? = null,
    ) {
        val groups = FakeGroupRepository(initialGroups = listOf(trip))
        expenses = FakeExpenseRepository(initialExpenses = listOf(storedExpense)).also {
            it.updateError = updateError
        }
        paying = FakePayingMethodRepository(
            initialMethods = methods,
            initialMarks = marks,
        )
        val underneath = if (screen.expenseId != null) withBill else groupStack
        navigator = BackStackNavigator(startRoute = underneath.first()).also { navigator ->
            underneath.drop(1).forEach(navigator::navigate)
            navigator.navigate(screen)
        }
        val deletedBills = InMemoryDeletedBillRepository()
        viewModel = SplitBillViewModel(
            paidWithUseCases = paidWithUseCasesOf(paying),
            trackEvent = { name, params -> trackedEvents += name to params },
            route = screen,
            navigator = navigator,
            createExpense = CreateExpenseUseCase(expenses) { NOW_MILLIS },
            updateExpense = UpdateExpenseUseCase(expenses) { NOW_MILLIS },
            deleteExpense = DeleteExpenseUseCase(expenses, deletedBills) { NOW_MILLIS },
            noticeBillGone = NoticeBillGoneUseCase(deletedBills),
            currentTimeMillis = { NOW_MILLIS },
            currentTimeZone = { TimeZone.UTC },
            decimalSeparator = { ',' },
            observeGroup = ObserveGroupUseCase(groups),
            observeGroupIncomes = { flowOf(emptyMap()) },
            observeExpense = ObserveExpenseUseCase(expenses),
            observeGroupsByActivity = { groups.groups },
            observeExpenses = ObserveExpensesUseCase(expenses),
            observeSettlements = { flowOf(emptyList()) },
            categoryUseCases = BillCategoryUseCases(
                observeCategoryTree = ObserveCategoryTreeUseCase(FakeCategoryRepository()),
                observeCategoryMap = ObserveCategoryMapUseCase(FakeCategoryTermRepository()),
                saveGroupCategory = { _, _, _, _, _, _, _ -> Result.success("") },
                deleteGroupCategory = { Result.success(Unit) },
            ),
            saveSplitAsDefault = { _, _, _, _, _ -> Result.success(Unit) },
            offerCategoryTeach = {},
            takePickedReceipt = { null },
            readReceipt = { _, _, _ ->
                ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE)
            },
            attachBillImage = { _, _, _, _, _ -> Result.success(BillAttachmentAddition.ADDED) },
            observeBitcoinInSats = { flowOf(false) },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val EXPENSE_ID = "expense-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"
        const val BRUNO = "member-3"
        const val NUBANK = "method-nubank"
        const val CASH = "method-cash"
        const val PIX = "method-pix"

        // Why: 2026-10-30T15:00:00Z.
        const val NOW_MILLIS = 1_793_372_400_000L
        const val SAVED_VERSION = 1_793_300_000_000L
        const val SPENT_AT = 1_793_296_800_000L
    }
}
