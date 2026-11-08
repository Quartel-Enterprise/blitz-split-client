package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.BillItem
import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReadFailure
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptScan
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
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
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsFee
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.BillCategoryUseCases
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
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
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

    @Test
    fun `GIVEN Dividir of a new bill WHEN picking Itens THEN sends the mode selected with Itens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.ITEMS))

            // Then
            assertEquals(
                listOf("split_bill_split_mode_selected" to mapOf<String, Any>("mode" to SplitMode.ITEMS)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Itens WHEN tapping Adicionar item THEN sends the item add clicked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)

        // Then
        assertEquals(listOf("bill_item_add_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN a new item WHEN typing its name quantity and price THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnItemNameChanged("Pizza"))
        viewModel.onEvent(SplitBillUiEvent.OnItemQuantityChanged("2"))
        viewModel.onEvent(SplitBillUiEvent.OnItemPriceChanged("60,00"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a new item WHEN adding a unit with the stepper THEN sends the quantity changed up`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemQuantityIncreased)

            // Then
            assertEquals(
                listOf("bill_item_quantity_changed" to mapOf<String, Any>("direction" to "up")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN an item of two units WHEN taking one with the stepper THEN sends the quantity changed down`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
            viewModel.onEvent(SplitBillUiEvent.OnItemQuantityIncreased)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemQuantityDecreased)

            // Then
            assertEquals(
                listOf("bill_item_quantity_changed" to mapOf<String, Any>("direction" to "down")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN an item of two units on Igual WHEN picking Unidades THEN sends the item mode selected`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
            viewModel.onEvent(SplitBillUiEvent.OnItemQuantityIncreased)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemModeClicked(ItemSplitMode.UNITS))

            // Then
            assertEquals(
                listOf("bill_item_mode_selected" to mapOf<String, Any>("mode" to ItemSplitMode.UNITS)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN an item on Igual WHEN picking Igual again THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnItemModeClicked(ItemSplitMode.EQUAL))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN an item of one unit WHEN Unidades arrives THEN sends nothing as the item does not take it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemModeClicked(ItemSplitMode.UNITS))

            // Then
            assertEquals(
                ItemSplitMode.EQUAL,
                viewModel.uiState.value.itemEditor
                    ?.mode,
            )
            assertEquals(emptyList(), trackedEvents)
        }

    @Test
    fun `GIVEN a new item with nobody marked WHEN marking Ana THEN sends the item person toggled as included`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemPersonToggled(ANA))

            // Then
            assertEquals(
                listOf("bill_item_person_toggled" to mapOf<String, Any>("is_included" to true)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Ana marked in an item WHEN unmarking her THEN sends the item person toggled as left out`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
            viewModel.onEvent(SplitBillUiEvent.OnItemPersonToggled(ANA))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemPersonToggled(ANA))

            // Then
            assertEquals(
                listOf("bill_item_person_toggled" to mapOf<String, Any>("is_included" to false)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN no item open WHEN an item person toggle arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnItemPersonToggled(ANA))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN an item by units WHEN adding and taking a unit THEN sends the person changed up then down`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
            viewModel.onEvent(SplitBillUiEvent.OnItemQuantityIncreased)
            viewModel.onEvent(SplitBillUiEvent.OnItemModeClicked(ItemSplitMode.UNITS))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemPersonIncreased(PIERRE))
            viewModel.onEvent(SplitBillUiEvent.OnItemPersonDecreased(PIERRE))

            // Then
            assertEquals(
                listOf(
                    "bill_item_person_changed" to mapOf<String, Any>("direction" to "up"),
                    "bill_item_person_changed" to mapOf<String, Any>("direction" to "down"),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN an item by value WHEN typing what someone consumed THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
        viewModel.onEvent(SplitBillUiEvent.OnItemPriceChanged("60,00"))
        viewModel.onEvent(SplitBillUiEvent.OnItemModeClicked(ItemSplitMode.AMOUNT))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnItemPersonValueChanged(PIERRE, "30,00"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a new item with its name price and someone WHEN tapping Pronto THEN sends the item confirmed as new`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
            viewModel.onEvent(SplitBillUiEvent.OnItemNameChanged("Pizza"))
            viewModel.onEvent(SplitBillUiEvent.OnItemPriceChanged("60,00"))
            viewModel.onEvent(SplitBillUiEvent.OnItemPersonToggled(PIERRE))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemDone)

            // Then
            assertEquals(
                listOf(
                    "bill_item_confirmed" to mapOf<String, Any>(
                        "mode" to ItemSplitMode.EQUAL,
                        "is_new" to true,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a new item with nobody marked WHEN tapping Pronto THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
        viewModel.onEvent(SplitBillUiEvent.OnItemNameChanged("Pizza"))
        viewModel.onEvent(SplitBillUiEvent.OnItemPriceChanged("60,00"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnItemDone)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN an item of the list WHEN tapping it THEN sends the item clicked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        addItem(
            name = "Pizza",
            price = "50,00",
            consumers = listOf(PIERRE),
        )
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnItemClicked(itemId()))

        // Then
        assertEquals(listOf("bill_item_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN an item of the list open WHEN tapping Pronto THEN sends the item confirmed as not new`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            addItem(
                name = "Pizza",
                price = "50,00",
                consumers = listOf(PIERRE),
            )
            viewModel.onEvent(SplitBillUiEvent.OnItemClicked(itemId()))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemDone)

            // Then
            assertEquals(
                listOf(
                    "bill_item_confirmed" to mapOf<String, Any>(
                        "mode" to ItemSplitMode.EQUAL,
                        "is_new" to false,
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN an item open WHEN going back THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnItemDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN an item of the list WHEN tapping Apagar item THEN sends the item delete clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            addItem(
                name = "Pizza",
                price = "50,00",
                consumers = listOf(PIERRE),
            )
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemDeleteClicked(itemId()))

            // Then
            assertEquals(listOf("bill_item_delete_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN an item just deleted WHEN a late Apagar item of it arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            addItem(
                name = "Pizza",
                price = "50,00",
                consumers = listOf(PIERRE),
            )
            val deletedId = itemId()
            viewModel.onEvent(SplitBillUiEvent.OnItemDeleteClicked(deletedId))
            viewModel.onEvent(SplitBillUiEvent.OnItemDeleteConfirmed)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemDeleteClicked(deletedId))

            // Then
            assertNull(viewModel.uiState.value.itemDeletion)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Apagar item asking WHEN confirming THEN sends the item delete confirmed`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        addItem(
            name = "Pizza",
            price = "50,00",
            consumers = listOf(PIERRE),
        )
        viewModel.onEvent(SplitBillUiEvent.OnItemDeleteClicked(itemId()))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnItemDeleteConfirmed)

        // Then
        assertEquals(listOf("bill_item_delete_confirmed" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN Apagar item confirmed WHEN the Apple alert dismisses itself THEN sends nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            addItem(
                name = "Pizza",
                price = "50,00",
                consumers = listOf(PIERRE),
            )
            viewModel.onEvent(SplitBillUiEvent.OnItemDeleteClicked(itemId()))
            viewModel.onEvent(SplitBillUiEvent.OnItemDeleteConfirmed)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemDeleteDismissed)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Apagar item asking WHEN cancelling THEN sends the item delete cancelled`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        addItem(
            name = "Pizza",
            price = "50,00",
            consumers = listOf(PIERRE),
        )
        viewModel.onEvent(SplitBillUiEvent.OnItemDeleteClicked(itemId()))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnItemDeleteDismissed)

        // Then
        assertEquals(listOf("bill_item_delete_cancelled" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN no question open WHEN a delete confirm arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnItemDeleteConfirmed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Itens WHEN tapping the Servico row THEN sends the fee clicked with the service`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnFeeClicked(ItemsFee.SERVICE))

            // Then
            assertEquals(
                listOf("bill_items_fee_clicked" to mapOf<String, Any>("fee" to ItemsFee.SERVICE)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Itens closed back to Dividir WHEN a late tap on the Couvert row arrives THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            viewModel.onEvent(SplitBillUiEvent.OnItemsDismissed)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnFeeClicked(ItemsFee.COUVERT))

            // Then
            assertNull(viewModel.uiState.value.feeEditor)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the couvert typed WHEN tapping Pronto THEN sends the fee confirmed with the couvert only`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            viewModel.onEvent(SplitBillUiEvent.OnFeeClicked(ItemsFee.COUVERT))
            trackedEvents.clear()
            viewModel.onEvent(SplitBillUiEvent.OnFeeValueChanged("2,50"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnFeeDone)

            // Then
            assertEquals(
                listOf("bill_items_fee_confirmed" to mapOf<String, Any>("fee" to ItemsFee.COUVERT)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a service over the limit typed WHEN tapping Pronto THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        viewModel.onEvent(SplitBillUiEvent.OnFeeClicked(ItemsFee.SERVICE))
        viewModel.onEvent(SplitBillUiEvent.OnFeeValueChanged("999"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnFeeDone)

        // Then
        assertNotNull(viewModel.uiState.value.feeEditor)
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the service field open WHEN closing it THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        viewModel.onEvent(SplitBillUiEvent.OnFeeClicked(ItemsFee.SERVICE))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnFeeDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN items short of the total WHEN tapping Adicionar item of the difference THEN sends its click`() =
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
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnAddDifferenceItemClicked)

            // Then
            assertEquals(listOf("bill_items_difference_item_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN items short of the total WHEN tapping Dividir a diferenca THEN sends the difference split clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            changeService("")
            addItem(
                name = "Pizza",
                price = "60,00",
                consumers = listOf(PIERRE, ANA),
            )
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitDifferenceClicked)

            // Then
            assertEquals(listOf("bill_items_difference_split_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN items that close with the total WHEN Dividir a diferenca arrives THEN sends nothing`() =
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
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitDifferenceClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the difference shared WHEN taking it off THEN sends the difference removed`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        changeService("")
        addItem(
            name = "Pizza",
            price = "60,00",
            consumers = listOf(PIERRE, ANA),
        )
        viewModel.onEvent(SplitBillUiEvent.OnSplitDifferenceClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnDifferenceRemoved)

        // Then
        assertEquals(listOf("bill_items_difference_removed" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN items that close with the total WHEN tapping Pronto THEN sends the items confirmed with how many`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            openItems()
            changeService("")
            addItem(
                name = "Pizza",
                price = "60,00",
                consumers = listOf(PIERRE, ANA),
            )
            addItem(
                name = "Vinho",
                price = "40,00",
                consumers = listOf(ANA),
            )
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnItemsDone)

            // Then
            assertEquals(listOf("bill_items_confirmed" to mapOf<String, Any>("items" to 2)), trackedEvents)
        }

    @Test
    fun `GIVEN items short of the total WHEN tapping Pronto THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        changeService("")
        addItem(
            name = "Pizza",
            price = "60,00",
            consumers = listOf(PIERRE),
        )
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnItemsDone)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Itens WHEN going back to Dividir THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        openItems()
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnItemsDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a bill split by its items WHEN tapping Salvar THEN the bill saved says Itens`() =
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
            viewModel.onEvent(SplitBillUiEvent.OnItemsDone)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                "bill_saved" to mapOf<String, Any>(
                    "is_new" to true,
                    "split_mode" to SplitMode.ITEMS,
                    "payers" to 1,
                    "is_from_receipt" to false,
                    "is_category_picked" to false,
                    "is_split_as_default" to false,
                ),
                trackedEvents.last(),
            )
        }

    private fun itemId(): String = viewModel.uiState.value.itemsEditor
        ?.items
        ?.first()
        ?.id
        .orEmpty()

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
        currency = GroupCurrency.BRL,
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
        shape = GroupShape.SQUARE,
        photo = null,
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
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    private val newRoute = Route.SplitBill(
        GROUP_ID,
        expenseId = null,
        picksGroupFirst = false,
        scansReceipt = false,
        duplicatesExpenseId = null,
    )

    private val editRoute = Route.SplitBill(
        GROUP_ID,
        EXPENSE_ID,
        picksGroupFirst = false,
        scansReceipt = false,
        duplicatesExpenseId = null,
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
        hasJoined = isMe,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun prepareScenario(
        storedExpense: Expense? = null,
        route: Route.SplitBill = newRoute,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(dinner))
        expenses = FakeExpenseRepository(initialExpenses = listOfNotNull(storedExpense))
        val navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            )
            it.navigate(route)
        }
        val deletedBills = InMemoryDeletedBillRepository()
        viewModel = SplitBillViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
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
            categoryUseCases = BillCategoryUseCases(
                observeCategoryTree = ObserveCategoryTreeUseCase(FakeCategoryRepository()),
                observeCategoryMap = ObserveCategoryMapUseCase(FakeCategoryTermRepository()),
                saveGroupCategory = { _, _, _, _, _, _, _ -> Result.success("") },
                deleteGroupCategory = { Result.success(Unit) },
            ),
            saveSplitAsDefault = { _, _, _, _ -> Result.success(Unit) },
            offerCategoryTeach = {},
            takePickedReceipt = { null },
            readReceipt = { _, _, _ ->
                ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE)
            },
            attachBillImage = { _, _, _, _, _ -> Result.success(BillAttachmentAddition.ADDED) },
            observeBitcoinInSats = { flowOf(false) },
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
