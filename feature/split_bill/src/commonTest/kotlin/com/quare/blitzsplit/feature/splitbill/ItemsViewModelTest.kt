package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.BillItem
import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CreateExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
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
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsFee
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
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

// Why: "Itens da conta" (7.5) and "Item" (7.6), Dividir's fifth mode.
class ItemsViewModelTest {
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
    fun `GIVEN Dividir of a new bill WHEN picking Itens THEN opens empty with the usual service`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.ITEMS))

            // Then
            val items = assertNotNull(viewModel.uiState.value.itemsEditor)
            assertTrue(items.items.isEmpty())
            assertEquals(10L, items.split.servicePercent)
            assertEquals(listOf(PIERRE, ANA, BRUNO), items.split.couvertMemberIds)
            assertEquals(ItemsStatus.AmountLeft(10_000L, 0L, 10_000L, isFromReceipt = false), items.status)
            assertFalse(items.addsUp)
            assertNotNull(viewModel.uiState.value.splitEditor)
        }

    @Test
    fun `GIVEN a new item with nobody marked WHEN tapping Pronto THEN it is not added`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
        viewModel.onEvent(SplitBillUiEvent.OnItemNameChanged("Pizza"))
        viewModel.onEvent(SplitBillUiEvent.OnItemPriceChanged("60,00"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnItemDone)

        // Then
        val editor = assertNotNull(viewModel.uiState.value.itemEditor)
        assertEquals(ItemStatus.Nobody, editor.status)
        assertFalse(editor.canSave)
        assertTrue(
            viewModel.uiState.value.itemsEditor
                ?.items
                .orEmpty()
                .isEmpty(),
        )
    }

    @Test
    fun `GIVEN an item with its name price and who consumed it WHEN tapping Pronto THEN it is added to the list`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            addItem(
                name = "Pizza",
                price = "60,00",
                consumers = listOf(PIERRE, ANA),
            )

            // When
            val items = assertNotNull(viewModel.uiState.value.itemsEditor)

            // Then
            assertNull(viewModel.uiState.value.itemEditor)
            assertEquals(listOf("Pizza"), items.items.map { it.name })
            assertEquals(
                listOf(PIERRE, ANA),
                items.rows
                    .single()
                    .consumers
                    .map { it.id },
            )
            assertEquals(6_000L, items.breakdown.itemsMinor)
            assertEquals(600L, items.breakdown.serviceMinor)
        }

    @Test
    fun `GIVEN items that close with the total WHEN tapping Pronto THEN the bill splits by them and saves them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            changeService("")
            addItem(
                name = "Pizza",
                price = "100,00",
                consumers = listOf(PIERRE, ANA),
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemsDone)
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.itemsEditor)
            assertNull(state.splitEditor)
            assertEquals(SplitMode.ITEMS, state.split.mode)
            val created = expenses.createdExpenses.single()
            assertEquals(SplitMode.ITEMS, created.splitMode)
            assertEquals(listOf("Pizza"), created.items?.items?.map { it.name })
            assertEquals(listOf(5_000L, 5_000L), created.shares.map { it.amountMinor })
        }

    @Test
    fun `GIVEN items short of the total WHEN tapping Adicionar item de the difference THEN a new item costs it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            changeService("")
            addItem(
                name = "Pizza",
                price = "60,00",
                consumers = listOf(PIERRE),
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnAddDifferenceItemClicked)

            // Then
            val editor = assertNotNull(viewModel.uiState.value.itemEditor)
            assertTrue(editor.isNew)
            assertEquals("40,00", editor.unitPriceText)
            assertEquals(1L, editor.quantity)
        }

    @Test
    fun `GIVEN items short of the total WHEN tapping Dividir a diferenca THEN it closes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        changeService("")
        addItem(
            name = "Pizza",
            price = "60,00",
            consumers = listOf(PIERRE, ANA),
        )

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSplitDifferenceClicked)

        // Then
        val items = assertNotNull(viewModel.uiState.value.itemsEditor)
        assertEquals(4_000L, items.split.extraMinor)
        assertTrue(items.addsUp)
        assertEquals(listOf(5_000L, 5_000L, 0L), items.personRows.map { it.totalMinor })
    }

    @Test
    fun `GIVEN an item of the list WHEN deleting it THEN asks saying what is left out and then takes it off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            addItem(
                name = "Pizza",
                price = "50,00",
                consumers = listOf(PIERRE),
            )
            val itemId = viewModel.uiState.value.itemsEditor
                ?.items
                ?.single()
                ?.id
                .orEmpty()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemDeleteClicked(itemId))
            val deletion = viewModel.uiState.value.itemDeletion
            viewModel.onEvent(SplitBillUiEvent.OnItemDeleteConfirmed)

            // Then
            assertEquals("Pizza", deletion?.name)
            assertEquals(5_000L, deletion?.totalMinor)
            assertEquals(10_000L, deletion?.leftMinor)
            assertTrue(deletion?.redoesService == true)
            assertTrue(
                viewModel.uiState.value.itemsEditor
                    ?.items
                    .orEmpty()
                    .isEmpty(),
            )
            assertNull(viewModel.uiState.value.itemDeletion)
        }

    @Test
    fun `GIVEN items typed in Itens WHEN going back to Dividir and picking Itens again THEN they are still there`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            addItem(
                name = "Pizza",
                price = "50,00",
                consumers = listOf(PIERRE),
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemsDismissed)
            val dividir = viewModel.uiState.value
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.ITEMS))

            // Then
            assertNull(dividir.itemsEditor)
            assertNotNull(dividir.splitEditor)
            assertEquals(
                listOf("Pizza"),
                viewModel.uiState.value.itemsEditor
                    ?.items
                    ?.map { it.name },
            )
        }

    @Test
    fun `GIVEN items typed in Itens WHEN Dividir closes THEN picking Itens later starts over`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            addItem(
                name = "Pizza",
                price = "50,00",
                consumers = listOf(PIERRE),
            )
            viewModel.onEvent(SplitBillUiEvent.OnItemsDismissed)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitDismissed)
            openItems()

            // Then
            assertTrue(
                viewModel.uiState.value.itemsEditor
                    ?.items
                    .orEmpty()
                    .isEmpty(),
            )
        }

    @Test
    fun `GIVEN six beers by units WHEN one is not marked THEN says what is missing next to who changed last`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
            viewModel.onEvent(SplitBillUiEvent.OnItemNameChanged("Cerveja"))
            viewModel.onEvent(SplitBillUiEvent.OnItemPriceChanged("14,00"))
            repeat(5) { viewModel.onEvent(SplitBillUiEvent.OnItemQuantityIncreased) }
            viewModel.onEvent(SplitBillUiEvent.OnItemModeClicked(ItemSplitMode.UNITS))

            // When
            repeat(3) { viewModel.onEvent(SplitBillUiEvent.OnItemPersonIncreased(PIERRE)) }
            repeat(2) { viewModel.onEvent(SplitBillUiEvent.OnItemPersonIncreased(ANA)) }

            // Then
            val editor = assertNotNull(viewModel.uiState.value.itemEditor)
            assertEquals(
                ItemStatus.UnitsLeft(
                    marked = 5L,
                    quantity = 6L,
                ),
                editor.status,
            )
            assertEquals(listOf(false, true, false), editor.lines.map { it.isError })
            assertFalse(editor.canSave)
        }

    @Test
    fun `GIVEN an item by units WHEN its quantity goes down to one THEN it splits equally among who had some`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
            viewModel.onEvent(SplitBillUiEvent.OnItemPriceChanged("14,00"))
            viewModel.onEvent(SplitBillUiEvent.OnItemQuantityIncreased)
            viewModel.onEvent(SplitBillUiEvent.OnItemModeClicked(ItemSplitMode.UNITS))
            viewModel.onEvent(SplitBillUiEvent.OnItemPersonIncreased(PIERRE))
            viewModel.onEvent(SplitBillUiEvent.OnItemPersonIncreased(ANA))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemQuantityDecreased)

            // Then
            val editor = assertNotNull(viewModel.uiState.value.itemEditor)
            assertEquals(ItemSplitMode.EQUAL, editor.mode)
            assertEquals(listOf(true, true, false), editor.rows.map { it.isIncluded })
            assertFalse(ItemSplitMode.UNITS in editor.modes)
        }

    @Test
    fun `GIVEN the couvert row WHEN typing a value per person THEN everyone at the table pays it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            viewModel.onEvent(SplitBillUiEvent.OnFeeClicked(ItemsFee.COUVERT))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnFeeValueChanged("2,50"))
            viewModel.onEvent(SplitBillUiEvent.OnFeeDone)

            // Then
            val items = assertNotNull(viewModel.uiState.value.itemsEditor)
            assertNull(viewModel.uiState.value.feeEditor)
            assertEquals(750L, items.breakdown.couvertMinor)
            assertEquals(listOf(250L, 250L, 250L), items.personRows.map { it.totalMinor })
        }

    @Test
    fun `GIVEN a bill split by its items WHEN Editar conta opens Divisao THEN Itens opens with its items`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = billByItems,
                route = editRoute,
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals(SplitMode.ITEMS, state.split.mode)
            assertTrue(state.canSave)
            assertEquals(listOf("Pizza"), state.itemsEditor?.items?.map { it.name })
            assertTrue(state.itemsEditor?.addsUp == true)
        }

    @Test
    fun `GIVEN Itens of a bill split by its items WHEN going back THEN Dividir shows each total by Valores`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = billByItems,
                route = editRoute,
            )
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemsDismissed)

            // Then
            val editor = assertNotNull(viewModel.uiState.value.splitEditor)
            assertEquals(SplitMode.AMOUNT, editor.mode)
            assertEquals(listOf("50,00", "50,00", ""), editor.rows.map { it.valueText })
            assertTrue(editor.addsUp)
        }

    private fun openItems() {
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.ITEMS))
    }

    private fun changeService(text: String) {
        viewModel.onEvent(SplitBillUiEvent.OnFeeClicked(ItemsFee.SERVICE))
        viewModel.onEvent(SplitBillUiEvent.OnFeeValueChanged(text))
        viewModel.onEvent(SplitBillUiEvent.OnFeeDone)
    }

    private fun addItem(
        name: String,
        price: String,
        consumers: List<String>,
    ) {
        viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
        viewModel.onEvent(SplitBillUiEvent.OnItemNameChanged(name))
        viewModel.onEvent(SplitBillUiEvent.OnItemPriceChanged(price))
        consumers.forEach { viewModel.onEvent(SplitBillUiEvent.OnItemPersonToggled(it)) }
        viewModel.onEvent(SplitBillUiEvent.OnItemDone)
    }

    private val billByItems = Expense(
        id = EXPENSE_ID,
        groupId = GROUP_ID,
        description = "Jantar",
        amountMinor = 10_000L,
        splitMode = SplitMode.ITEMS,
        payers = listOf(ExpensePayer(PIERRE, 10_000L)),
        shares = listOf(ExpenseShare(PIERRE, 5_000L, 5_000L), ExpenseShare(ANA, 5_000L, 5_000L)),
        items = BillItems(
            items = listOf(
                BillItem(
                    id = "pizza",
                    name = "Pizza",
                    quantity = 1L,
                    unitPriceMinor = 10_000L,
                    mode = ItemSplitMode.EQUAL,
                    entries = listOf(SplitEntry(PIERRE, 1L), SplitEntry(ANA, 1L)),
                ),
            ),
            servicePercent = 0L,
            couvertMinor = 0L,
            couvertMemberIds = listOf(PIERRE, ANA, BRUNO),
            extraMinor = 0L,
            isFromReceipt = false,
        ),
        categoryId = null,
        spentAt = NOW_MILLIS,
        isPending = false,
        updatedAt = NOW_MILLIS,
        updatedByMemberId = null,
    )

    private val dinner = Group(
        id = GROUP_ID,
        name = "Jantar",
        type = GroupType.EVENT,
        color = AvatarColor.GREEN,
        currency = GroupCurrency.BRL,
        members = listOf(
            member(PIERRE, "Pierre", isMe = true),
            member(ANA, "Ana", isMe = false),
            member(BRUNO, "Bruno", isMe = false),
        ),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    private val newRoute = Route.SplitBill(
        GROUP_ID,
        expenseId = null,
        picksGroupFirst = false,
    )

    private val editRoute = Route.SplitBill(GROUP_ID, EXPENSE_ID, picksGroupFirst = false)

    private fun member(
        id: String,
        name: String,
        isMe: Boolean,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = isMe,
        userId = null,
        inviteCode = null,
    )

    private fun prepareScenario(
        storedExpense: Expense? = null,
        route: Route.SplitBill = newRoute,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(dinner))
        expenses = FakeExpenseRepository(initialExpenses = listOfNotNull(storedExpense))
        val navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.GroupDetail(GROUP_ID))
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
            decimalSeparator = { ',' },
            observeGroup = ObserveGroupUseCase(groups),
            observeExpense = ObserveExpenseUseCase(expenses),
            observeGroupsByActivity = { groups.groups },
            observeExpenses = ObserveExpensesUseCase(expenses),
            observeSettlements = { flowOf(emptyList()) },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
        )
        if (route.expenseId == null) {
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))
        }
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val EXPENSE_ID = "expense-1"
        const val PIERRE = "member-1"
        const val ANA = "member-2"
        const val BRUNO = "member-3"

        // Why: 2026-10-30T15:00:00Z.
        const val NOW_MILLIS = 1_793_372_400_000L
    }
}
