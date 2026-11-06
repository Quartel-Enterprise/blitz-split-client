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
import com.quare.blitzsplit.feature.splitbill.presentation.model.ItemsFee
import com.quare.blitzsplit.feature.splitbill.presentation.model.PayerAmounts
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.BillCategoryUseCases
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import kotlinx.coroutines.CompletableDeferred
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

// Why: the currency beside "Valor" on "Rachar conta", which belongs to each bill, the group's being only its default.
class CurrencyPickerViewModelTest {
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
    fun `GIVEN a group in reais WHEN a new bill opens THEN it is in reais with no currency picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals(GroupCurrency.BRL, state.currency)
            assertNull(state.pickedCurrency)
            assertEquals(GroupCurrency.entries, state.currencies)
        }

    @Test
    fun `GIVEN a dollar bill picked and left WHEN a new bill opens in the group again THEN it is in reais`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("12,50")
            describe("Pizza")
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.USD))
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GroupCurrency.BRL, state.currency)
            assertNull(state.pickedCurrency)
        }

    @Test
    fun `GIVEN a dollar bill in a reais group WHEN Editar conta opens THEN it is in dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = dollarBill,
                route = editRoute,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isEditing)
            assertEquals(GroupCurrency.USD, state.currency)
            assertEquals(GroupCurrency.USD, state.pickedCurrency)
            assertEquals("312,00", state.amountText)
            assertEquals(31_200L, state.amountMinor)
        }

    @Test
    fun `GIVEN a dollar bill in a reais group WHEN Editar conta opens THEN its split and payers are in dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = dollarBill,
                route = editRoute,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GroupCurrency.USD, state.split.currency)
            assertEquals(31_200L, state.split.totalMinor)
            assertEquals(listOf(15_600L, 15_600L), state.split.shares?.map { it.amountMinor })
        }

    @Test
    fun `GIVEN a bill in the currency of its group WHEN Editar conta opens THEN it is in that currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = dollarBill.copy(currency = GroupCurrency.BRL),
                route = editRoute,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GroupCurrency.BRL, state.currency)
            assertEquals("312,00", state.amountText)
        }

    @Test
    fun `GIVEN a dollar bill opened to edit WHEN saving THEN the bill is saved in dollars`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedExpense = dollarBill,
            route = editRoute,
        )
        describe("Feira")

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        val saved = expenses.updatedExpenses.single()
        assertEquals(GroupCurrency.USD, saved.currency)
        assertEquals(31_200L, saved.amountMinor)
        assertEquals("Feira", saved.description)
    }

    @Test
    fun `GIVEN a dollar bill opened to edit WHEN a sync brings the group again THEN it stays in dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = dollarBill,
                route = editRoute,
            )

            // When
            groups.groups.value = listOf(trip.copy(name = "Viagem Sampa"), home)

            // Then
            val state = viewModel.uiState.value
            assertEquals("Viagem Sampa", state.groupName)
            assertEquals(GroupCurrency.USD, state.currency)
            assertEquals("312,00", state.amountText)
        }

    @Test
    fun `GIVEN a dollar bill opened to edit WHEN a sync turns the group to yen THEN it stays in dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = dollarBill,
                route = editRoute,
            )

            // When
            groups.groups.value = listOf(trip.copy(currency = GroupCurrency.JPY), home)

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.USD, state.currency)
            assertEquals("312,00", state.amountText)
            assertEquals(31_200L, state.split.totalMinor)
        }

    @Test
    fun `GIVEN a dollar bill opened to edit WHEN picking yen and saving THEN the bill is saved in yen`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = dollarBill,
                route = editRoute,
            )
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val saved = expenses.updatedExpenses.single()
            assertEquals(GroupCurrency.JPY, saved.currency)
            assertEquals(312L, saved.amountMinor)
            assertEquals(312L, saved.shares.sumOf { it.amountMinor })
            assertEquals(312L, saved.payers.sumOf { it.amountMinor })
        }

    @Test
    fun `GIVEN yen picked on a bill being edited WHEN a sync brings the group again THEN it stays in yen`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = dollarBill,
                route = editRoute,
            )
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))

            // When
            groups.groups.value = listOf(trip.copy(name = "Viagem Sampa"), home)

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.JPY, state.currency)
            assertEquals("312", state.amountText)
        }

    @Test
    fun `GIVEN a bill split by items being edited WHEN picking yen THEN it splits equally and adds up`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedExpense = billByItems,
                route = editRoute,
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))

            // Then
            val state = viewModel.uiState.value
            assertEquals(SplitMode.EQUAL, state.split.mode)
            assertNull(state.split.items)
            assertNull(state.itemsEditor)
            assertEquals(100L, state.split.totalMinor)
            assertEquals(listOf(50L, 50L), state.split.shares?.map { it.amountMinor })
            assertTrue(state.split.addsUp)
        }

    @Test
    fun `GIVEN 12 reais and 50 typed WHEN picking dollars THEN it is 12 dollars and 50 cents`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("12,50")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.USD))

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.USD, state.currency)
            assertEquals(GroupCurrency.USD, state.pickedCurrency)
            assertEquals("12,50", state.amountText)
            assertEquals(1_250L, state.amountMinor)
            assertEquals(1_250L, state.split.totalMinor)
            assertEquals(GroupCurrency.USD, state.split.currency)
        }

    @Test
    fun `GIVEN 12 reais and 50 typed WHEN picking yen THEN the cents are cut and it is 12 yen`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("12,50")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))

            // Then
            val state = viewModel.uiState.value
            assertEquals("12", state.amountText)
            assertEquals(12L, state.amountMinor)
            assertEquals(12L, state.split.totalMinor)
            assertEquals(12L, state.split.shares?.sumOf { it.amountMinor })
        }

    @Test
    fun `GIVEN 12 yen after 12 reais and 50 WHEN picking reais again THEN it is 12 reais with no cents`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("12,50")
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.BRL))

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.BRL, state.currency)
            assertEquals("12,00", state.amountText)
            assertEquals(1_200L, state.amountMinor)
            assertEquals(1_200L, state.split.totalMinor)
        }

    @Test
    fun `GIVEN no amount typed WHEN picking dollars THEN the field stays empty in dollars`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.USD))

        // Then
        val state = viewModel.uiState.value
        assertEquals(GroupCurrency.USD, state.currency)
        assertEquals("", state.amountText)
        assertNull(state.amountMinor)
        assertEquals(0L, state.split.totalMinor)
    }

    @Test
    fun `GIVEN 12 reais and 50 typed and Bitcoin em satoshis on WHEN picking bitcoin THEN it is 12 sats`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = true)
            typeAmount("12,50")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.BTC))

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.BTC, state.currency)
            assertEquals("12", state.amountText)
            assertEquals(12L, state.amountMinor)
            assertEquals(12L, state.split.totalMinor)
            assertTrue(state.split.showsSats)
        }

    @Test
    fun `GIVEN 12 reais and 50 typed and Bitcoin em satoshis off WHEN picking bitcoin THEN it keeps 12 and 50`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = false)
            typeAmount("12,50")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.BTC))

            // Then
            val state = viewModel.uiState.value
            assertEquals("12,50000000", state.amountText)
            assertEquals(1_250_000_000L, state.amountMinor)
            assertEquals(1_250_000_000L, state.split.totalMinor)
        }

    @Test
    fun `GIVEN 30 million reais typed WHEN picking bitcoin THEN the amount is over the cap and Salvar is disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = false)
            describe("Ilha")
            typeAmount("30000000,00")
            assertTrue(viewModel.uiState.value.canSave)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.BTC))

            // Then
            val state = viewModel.uiState.value
            assertEquals(3_000_000_000_000_000L, state.amountMinor)
            assertFalse(state.isAmountValid)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN 30 million over the bitcoin cap WHEN picking reais again THEN Salvar is enabled again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = false)
            describe("Ilha")
            typeAmount("30000000,00")
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.BTC))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.BRL))

            // Then
            val state = viewModel.uiState.value
            assertEquals("30.000.000,00", state.amountText)
            assertTrue(state.isAmountValid)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a Valores split WHEN picking yen THEN each part keeps its number and the split adds up`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("50,00")
            confirmAmounts(ANA to "10,00", PIERRE to "40,00")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))

            // Then
            val state = viewModel.uiState.value
            assertEquals(SplitMode.AMOUNT, state.split.mode)
            assertEquals(50L, state.split.totalMinor)
            assertEquals(
                mapOf(ANA to 10L, PIERRE to 40L),
                state.split.shares?.associate { it.memberId to it.amountMinor },
            )
            assertTrue(state.split.addsUp)
        }

    @Test
    fun `GIVEN a Valores split with cents WHEN picking yen THEN the cut parts no longer add up and Salvar waits`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("50,00")
            confirmAmounts(ANA to "12,50", PIERRE to "37,50")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))

            // Then
            val state = viewModel.uiState.value
            assertEquals(50L, state.split.totalMinor)
            assertFalse(state.split.addsUp)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN Dividir open on Valores WHEN picking yen THEN Dividir closes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("50,00")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.AMOUNT))
        viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "12,50"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))

        // Then
        assertNull(viewModel.uiState.value.splitEditor)
    }

    @Test
    fun `GIVEN Dividir reopened after yen was picked WHEN it opens THEN it adds up to the amount in yen`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("50,00")
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // Then
            val editor = assertNotNull(viewModel.uiState.value.splitEditor)
            assertEquals(GroupCurrency.JPY, editor.currency)
            assertEquals(50L, editor.totalMinor)
            assertEquals(50L, editor.shares?.sumOf { it.amountMinor })
        }

    @Test
    fun `GIVEN Varias pessoas open WHEN picking yen THEN each amount keeps its number`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("312,00")
        viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)
        viewModel.onEvent(SplitBillUiEvent.OnPayerAmountChanged(ANA, "12,50"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))

        // Then
        val amounts = assertNotNull(viewModel.uiState.value.payerAmounts)
        assertEquals("12", amounts.textByMember().getValue(ANA))
        assertEquals("312", amounts.textByMember().getValue(PIERRE))
    }

    @Test
    fun `GIVEN several payers set WHEN picking yen THEN they keep their numbers and still add up`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("312,00")
            describe("Mercado")
            paySeveral(ANA to "200,00", PIERRE to "112,00")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val saved = expenses.createdExpenses.single()
            assertEquals(GroupCurrency.JPY, saved.currency)
            assertEquals(312L, saved.amountMinor)
            assertEquals(mapOf(ANA to 200L, PIERRE to 112L), saved.payers.associate { it.memberId to it.amountMinor })
        }

    @Test
    fun `GIVEN a bill split by items WHEN picking yen THEN the split starts over equally and adds up`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            splitByItems(
                name = "Pizza",
                price = "100,00",
            )
            assertEquals(SplitMode.ITEMS, viewModel.uiState.value.split.mode)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.JPY, state.currency)
            assertEquals(SplitMode.EQUAL, state.split.mode)
            assertNull(state.split.items)
            assertNull(state.itemsEditor)
            assertEquals("100", state.amountText)
            assertEquals(100L, state.split.totalMinor)
            assertEquals(listOf(50L, 50L), state.split.shares?.map { it.amountMinor })
        }

    @Test
    fun `GIVEN a bill split by items and yen picked WHEN saving THEN it is saved equally in yen`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            splitByItems(
                name = "Pizza",
                price = "100,00",
            )
            describe("Jantar")
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val saved = expenses.createdExpenses.single()
            assertEquals(GroupCurrency.JPY, saved.currency)
            assertEquals(SplitMode.EQUAL, saved.splitMode)
            assertNull(saved.items)
            assertEquals(100L, saved.amountMinor)
            assertEquals(100L, saved.shares.sumOf { it.amountMinor })
        }

    @Test
    fun `GIVEN dollars picked on a new bill WHEN saving THEN the bill is created in dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("12,50")
            describe("Pizza")
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.USD))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val saved = expenses.createdExpenses.single()
            assertEquals(TRIP_ID, saved.groupId)
            assertEquals(GroupCurrency.USD, saved.currency)
            assertEquals(1_250L, saved.amountMinor)
            assertEquals(1_250L, saved.shares.sumOf { it.amountMinor })
        }

    @Test
    fun `GIVEN no currency picked WHEN saving a new bill THEN the bill is created in the group currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("12,50")
            describe("Pizza")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(GroupCurrency.BRL, expenses.createdExpenses.single().currency)
        }

    @Test
    fun `GIVEN a new bill in reais WHEN it opens THEN Ler cupom fiscal is offered`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsScanReceipt)
    }

    @Test
    fun `GIVEN a new bill in reais WHEN picking bitcoin THEN Ler cupom fiscal goes away`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.BTC))

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showsScanReceipt)
        assertFalse(state.canScanReceipt)
    }

    @Test
    fun `GIVEN a new bill in ether WHEN picking dollars THEN Ler cupom fiscal comes back`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.ETH))
        assertFalse(viewModel.uiState.value.showsScanReceipt)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.USD))

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showsScanReceipt)
        assertTrue(state.canScanReceipt)
    }

    @Test
    fun `GIVEN the currency of the bill WHEN picking that same currency THEN nothing changes and nothing is tracked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("12,50")
            val before = viewModel.uiState.value

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.BRL))

            // Then
            assertEquals(before, viewModel.uiState.value)
            assertTrue(trackedEvents.none { it.first == CURRENCY_PICKED })
        }

    @Test
    fun `GIVEN dollars picked WHEN picking dollars again THEN the second pick is not tracked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.USD))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.USD))

            // Then
            assertEquals(1, trackedEvents.count { it.first == CURRENCY_PICKED })
        }

    @Test
    fun `GIVEN a new bill in reais WHEN picking dollars THEN tracks the pick with the currency code`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.USD))

            // Then
            assertEquals(
                listOf(CURRENCY_PICKED to mapOf<String, Any>("currency" to "USD")),
                trackedEvents.filter { it.first == CURRENCY_PICKED },
            )
        }

    @Test
    fun `GIVEN a receipt being read WHEN picking dollars THEN the pick is ignored`() = runTest(testDispatcher) {
        // Given
        val readGate = CompletableDeferred<ReceiptScan>()
        prepareScenario(readGate = readGate)
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(byteArrayOf(1, 2, 3)))
        assertTrue(viewModel.uiState.value.isFormLocked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.USD))

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.canChangeCurrency)
        assertEquals(GroupCurrency.BRL, state.currency)
        assertNull(state.pickedCurrency)
        assertTrue(trackedEvents.none { it.first == CURRENCY_PICKED })
    }

    @Test
    fun `GIVEN a bill being saved WHEN picking dollars THEN the pick is ignored`() = runTest(testDispatcher) {
        // Given
        val createGate = CompletableDeferred<Unit>()
        prepareScenario(createGate = createGate)
        typeAmount("12,50")
        describe("Pizza")
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)
        assertTrue(viewModel.uiState.value.isSaving)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.USD))

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.canChangeCurrency)
        assertEquals(GroupCurrency.BRL, state.currency)
        assertEquals("12,50", state.amountText)
        assertTrue(trackedEvents.none { it.first == CURRENCY_PICKED })
    }

    @Test
    fun `GIVEN a bill that can be edited WHEN it is loaded THEN the currency can be changed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.canChangeCurrency)
        }

    @Test
    fun `GIVEN yen picked WHEN a sync brings the group again THEN the bill stays in yen`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        typeAmount("12,50")
        viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))

        // When
        groups.groups.value = listOf(trip.copy(name = "Viagem Sampa"), home)

        // Then
        val state = viewModel.uiState.value
        assertEquals("Viagem Sampa", state.groupName)
        assertEquals(GroupCurrency.JPY, state.currency)
        assertEquals("12", state.amountText)
    }

    @Test
    fun `GIVEN dollars picked WHEN a sync turns the group to yen THEN the bill stays in dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("12,50")
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.USD))

            // When
            groups.groups.value = listOf(trip.copy(currency = GroupCurrency.JPY), home)

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.USD, state.currency)
            assertEquals("12,50", state.amountText)
        }

    @Test
    fun `GIVEN no currency picked WHEN a sync turns the group to yen THEN the bill follows the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("12,50")

            // When
            groups.groups.value = listOf(trip.copy(currency = GroupCurrency.JPY), home)

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.JPY, state.currency)
            assertEquals("12", state.amountText)
        }

    @Test
    fun `GIVEN yen picked WHEN moving the bill to a group in dollars THEN the pick is reset to dollars`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            typeAmount("12,50")
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))
            moveToGroup(HOME_ID)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(HOME_ID, state.groupId)
            assertEquals(GroupCurrency.USD, state.currency)
            assertNull(state.pickedCurrency)
            assertEquals("12,00", state.amountText)
            assertEquals(1_200L, state.split.totalMinor)
        }

    @Test
    fun `GIVEN a group moved with yen picked WHEN picking yen again THEN the pick counts as a change`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))
            moveToGroup(HOME_ID)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.JPY))

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.JPY, state.currency)
            assertEquals(GroupCurrency.JPY, state.pickedCurrency)
            assertEquals(2, trackedEvents.count { it.first == CURRENCY_PICKED })
        }

    @Test
    fun `GIVEN bitcoin picked WHEN moving the bill to a group in reais THEN Ler cupom fiscal is back`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(home, trip),
                route = newRoute(HOME_ID),
            )
            viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.BTC))
            moveToGroup(TRIP_ID)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(GroupCurrency.BRL, state.currency)
            assertTrue(state.showsScanReceipt)
        }

    // Why: the field emptied, then the amount put in whole, as pasting it does.
    private fun typeAmount(text: String) {
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged(""))
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged(text))
    }

    private fun describe(text: String) {
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged(text))
    }

    // Why: opens Dividir, switches to Valores, types each part and taps Pronto.
    private fun confirmAmounts(vararg values: Pair<String, String>) {
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.AMOUNT))
        values.forEach { (memberId, text) ->
            viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(memberId, text))
        }
        viewModel.onEvent(SplitBillUiEvent.OnSplitDone)
        assertNull(viewModel.uiState.value.splitEditor, "the Valores split should add up")
    }

    // Why: "Quem pagou", then "Várias pessoas pagaram" with what each one paid, confirmed with "Pronto".
    private fun paySeveral(vararg paid: Pair<String, String>) {
        viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)
        val typed = paid.toMap()
        assertNotNull(viewModel.uiState.value.payerAmounts).rows.forEach { row ->
            viewModel.onEvent(SplitBillUiEvent.OnPayerAmountChanged(row.memberId, typed[row.memberId].orEmpty()))
        }
        viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersDone)
        assertNull(viewModel.uiState.value.payerAmounts, "the amounts should add up")
    }

    // Why: the total typed, then Dividir by Itens with one item of that price that Pierre and Ana consumed, no service.
    private fun splitByItems(
        name: String,
        price: String,
    ) {
        typeAmount(price)
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.ITEMS))
        viewModel.onEvent(SplitBillUiEvent.OnFeeClicked(ItemsFee.SERVICE))
        viewModel.onEvent(SplitBillUiEvent.OnFeeValueChanged(""))
        viewModel.onEvent(SplitBillUiEvent.OnFeeDone)
        viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
        viewModel.onEvent(SplitBillUiEvent.OnItemNameChanged(name))
        viewModel.onEvent(SplitBillUiEvent.OnItemPriceChanged(price))
        viewModel.onEvent(SplitBillUiEvent.OnItemPersonToggled(PIERRE))
        viewModel.onEvent(SplitBillUiEvent.OnItemPersonToggled(ANA))
        viewModel.onEvent(SplitBillUiEvent.OnItemDone)
        viewModel.onEvent(SplitBillUiEvent.OnItemsDone)
    }

    private fun moveToGroup(groupId: String) {
        viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
        viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(groupId))
        viewModel.onEvent(SplitBillUiEvent.OnGroupDone)
    }

    private fun PayerAmounts.textByMember(): Map<String, String> = rows.associate { it.memberId to it.amountText }

    private fun newRoute(groupId: String = TRIP_ID): Route.SplitBill = Route.SplitBill(
        groupId,
        expenseId = null,
        picksGroupFirst = false,
        scansReceipt = false,
    )

    private val editRoute = Route.SplitBill(
        TRIP_ID,
        EXPENSE_ID,
        picksGroupFirst = false,
        scansReceipt = false,
    )

    private val pierre = member(PIERRE, "Pierre", isMe = true)
    private val ana = member(ANA, "Ana", isMe = false)

    private val trip = group(TRIP_ID, "Viagem Floripa", GroupCurrency.BRL)
    private val home = group(HOME_ID, "Casa", GroupCurrency.USD)

    // Why: "Mercado", US$ 312,00 Ana paid, split equally between Ana and Pierre, in a group that splits in reais.
    private val dollarBill = Expense(
        id = EXPENSE_ID,
        groupId = TRIP_ID,
        description = "Mercado",
        currency = GroupCurrency.USD,
        amountMinor = 31_200L,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(ANA, 31_200L)),
        shares = listOf(
            ExpenseShare(ANA, 15_600L, weight = 1L),
            ExpenseShare(PIERRE, 15_600L, weight = 1L),
        ),
        items = null,
        categoryId = null,
        spentAt = NOW_MILLIS,
        isPending = false,
        updatedAt = NOW_MILLIS,
        updatedByMemberId = ANA,
    )

    // Why: "Jantar", US$ 100,00 in one pizza that Ana and Pierre ate, in a group that splits in reais.
    private val billByItems = dollarBill.copy(
        description = "Jantar",
        amountMinor = 10_000L,
        splitMode = SplitMode.ITEMS,
        payers = listOf(ExpensePayer(PIERRE, 10_000L)),
        shares = listOf(
            ExpenseShare(ANA, 5_000L, weight = 5_000L),
            ExpenseShare(PIERRE, 5_000L, weight = 5_000L),
        ),
        items = BillItems(
            items = listOf(
                BillItem(
                    id = "pizza",
                    name = "Pizza",
                    quantity = 1L,
                    unitPriceMinor = 10_000L,
                    mode = ItemSplitMode.EQUAL,
                    entries = listOf(SplitEntry(ANA, 1L), SplitEntry(PIERRE, 1L)),
                ),
            ),
            servicePercent = 0L,
            couvertMinor = 0L,
            couvertMemberIds = listOf(ANA, PIERRE),
            extraMinor = 0L,
            isFromReceipt = false,
        ),
    )

    private fun group(
        id: String,
        name: String,
        currency: GroupCurrency,
    ): Group = Group(
        id = id,
        name = name,
        type = GroupType.EVENT,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = currency,
        members = listOf(ana, pierre),
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

    private fun prepareScenario(
        storedGroups: List<Group> = listOf(trip, home),
        route: Route.SplitBill = newRoute(),
        storedExpense: Expense? = null,
        sats: Boolean = true,
        createGate: CompletableDeferred<Unit>? = null,
        readGate: CompletableDeferred<ReceiptScan>? = null,
    ) {
        groups = FakeGroupRepository(initialGroups = storedGroups)
        expenses = FakeExpenseRepository(initialExpenses = listOfNotNull(storedExpense)).also {
            it.createGate = createGate
        }
        trackedEvents.clear()
        val navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(
                Route.GroupDetail(
                    route.groupId,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            )
            route.expenseId?.let { expenseId ->
                it.navigate(Route.BillDetail(route.groupId, expenseId, focusCommentId = null))
            }
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
                saveGroupCategory = { _, _, _, _, _, _ -> Result.success("") },
                deleteGroupCategory = { Result.success(Unit) },
            ),
            saveSplitAsDefault = { _, _, _, _ -> Result.success(Unit) },
            offerCategoryTeach = {},
            takePickedReceipt = { null },
            readReceipt = { _, _, _ -> readGate?.await() ?: ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE) },
            attachBillImage = { _, _, _, _, _ -> Result.success(BillAttachmentAddition.ADDED) },
            observeBitcoinInSats = { flowOf(sats) },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
        )
    }

    private companion object {
        const val CURRENCY_PICKED = "split_bill_currency_picked"
        const val TRIP_ID = "trip"
        const val HOME_ID = "home"
        const val EXPENSE_ID = "expense-1"
        const val ANA = "member-1"
        const val PIERRE = "member-2"

        // Why: 2026-10-30T15:00:00Z.
        const val NOW_MILLIS = 1_793_372_400_000L
    }
}
