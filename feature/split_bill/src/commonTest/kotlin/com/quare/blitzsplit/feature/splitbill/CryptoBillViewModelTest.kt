package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
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
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitEditor
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
import kotlin.test.assertTrue

// Why: Rachar conta in a group that splits in bitcoin or ether, with or without Preferências' "Bitcoin em satoshis".
class CryptoBillViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var bitcoinInSats: MutableStateFlow<Boolean>
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
    fun `GIVEN Bitcoin em satoshis on WHEN Rachar conta opens THEN the form and its split take whole sats`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = true)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals(GroupCurrency.BTC, state.currency)
            assertTrue(state.showsSats)
            assertTrue(state.split.showsSats)
        }

    @Test
    fun `GIVEN Bitcoin em satoshis off WHEN Rachar conta opens THEN the form does not take sats`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertFalse(state.showsSats)
            assertFalse(state.split.showsSats)
        }

    @Test
    fun `GIVEN a bitcoin group in sats WHEN typing 1 THEN the bill is 1 sat`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sats = true)

        // When
        typeDigits("1")

        // Then
        val state = viewModel.uiState.value
        assertEquals("1", state.amountText)
        assertEquals(1L, state.amountMinor)
        assertTrue(state.isAmountValid)
    }

    @Test
    fun `GIVEN a bitcoin group in sats WHEN typing 12345 THEN shows 12 thousand sats grouped`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = true)

            // When
            typeDigits("12345")

            // Then
            val state = viewModel.uiState.value
            assertEquals("12.345", state.amountText)
            assertEquals(12_345L, state.amountMinor)
            assertEquals(12_345L, state.split.totalMinor)
        }

    @Test
    fun `GIVEN a bitcoin group in sats in English WHEN typing 12345 THEN groups with a comma`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                sats = true,
                decimalSeparator = '.',
            )

            // When
            typeDigits("12345")

            // Then
            val state = viewModel.uiState.value
            assertEquals("12,345", state.amountText)
            assertEquals(12_345L, state.amountMinor)
        }

    @Test
    fun `GIVEN a bitcoin group in sats WHEN typing a comma after the sats THEN nothing changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = true)
            typeDigits("12")

            // When
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("12,"))

            // Then
            val state = viewModel.uiState.value
            assertEquals("12", state.amountText)
            assertEquals(12L, state.amountMinor)
        }

    @Test
    fun `GIVEN a bitcoin group not in sats WHEN typing 1 THEN the bill is 1 sat with 8 decimals`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = false)

            // When
            typeDigits("1")

            // Then
            val state = viewModel.uiState.value
            assertEquals("0,00000001", state.amountText)
            assertEquals(1L, state.amountMinor)
        }

    @Test
    fun `GIVEN an ether group with Bitcoin em satoshis on WHEN typing 1 THEN it keeps 8 decimals`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(ether),
                route = newRoute(ETH_ID),
                sats = true,
            )

            // When
            typeDigits("1")

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.ETH, state.currency)
            assertEquals("0,00000001", state.amountText)
            assertEquals(1L, state.amountMinor)
        }

    @Test
    fun `GIVEN a real group with Bitcoin em satoshis on WHEN typing 123 THEN it keeps the cents`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trip),
                route = newRoute(TRIP_ID),
                sats = true,
            )

            // When
            typeDigits("123")

            // Then
            val state = viewModel.uiState.value
            assertEquals("1,23", state.amountText)
            assertEquals(123L, state.amountMinor)
        }

    @Test
    fun `GIVEN sats typed WHEN Bitcoin em satoshis turns off meanwhile THEN the amount keeps its meaning`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = true)
            typeDigits("12345")

            // When
            bitcoinInSats.value = false

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsSats)
            assertTrue(state.split.showsSats)
            assertEquals("12.345", state.amountText)
            assertEquals(12_345L, state.amountMinor)
        }

    @Test
    fun `GIVEN Bitcoin em satoshis turned off while open WHEN typing a digit more THEN it still types sats`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = true)
            typeDigits("12")
            bitcoinInSats.value = false

            // When
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("123"))

            // Then
            val state = viewModel.uiState.value
            assertEquals("123", state.amountText)
            assertEquals(123L, state.amountMinor)
        }

    @Test
    fun `GIVEN Bitcoin em satoshis turned on while open WHEN typing 1 THEN it still types 8 decimals`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = false)
            bitcoinInSats.value = true

            // When
            typeDigits("1")

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.showsSats)
            assertEquals("0,00000001", state.amountText)
            assertEquals(1L, state.amountMinor)
        }

    @Test
    fun `GIVEN a bill typed in sats WHEN saving THEN the bill is saved in sats`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sats = true)
        typeDigits("12345")
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Pizza"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        val saved = expenses.createdExpenses.single()
        assertEquals(12_345L, saved.amountMinor)
        assertEquals(12_345L, saved.payers.sumOf { it.amountMinor })
        assertEquals(12_345L, saved.shares.sumOf { it.amountMinor })
    }

    @Test
    fun `GIVEN a bitcoin group in sats WHEN typing 21 million bitcoin THEN Salvar is enabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = true)
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Ilha"))

            // When
            typeDigits("2100000000000000")

            // Then
            val state = viewModel.uiState.value
            assertEquals(2_100_000_000_000_000L, state.amountMinor)
            assertTrue(state.isAmountValid)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a bitcoin group in sats WHEN typing 1 sat over 21 million bitcoin THEN Salvar is disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = true)
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Ilha"))

            // When
            typeDigits("2100000000000001")

            // Then
            val state = viewModel.uiState.value
            assertEquals(2_100_000_000_000_001L, state.amountMinor)
            assertFalse(state.isAmountValid)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN a bitcoin group not in sats WHEN typing 1 sat over 21 million bitcoin THEN Salvar is disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = false)
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Ilha"))

            // When
            typeDigits("2100000000000001")

            // Then
            val state = viewModel.uiState.value
            assertEquals("21.000.000,00000001", state.amountText)
            assertFalse(state.isAmountValid)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN an ether group WHEN typing 90 million ether THEN Salvar is enabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(ether),
            route = newRoute(ETH_ID),
        )
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Ilha"))

        // When
        typeDigits("9000000000000000")

        // Then
        val state = viewModel.uiState.value
        assertEquals("90.000.000,00000000", state.amountText)
        assertTrue(state.isAmountValid)
        assertTrue(state.canSave)
    }

    @Test
    fun `GIVEN an ether group WHEN typing just over 90 million ether THEN Salvar is disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(ether),
                route = newRoute(ETH_ID),
            )
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Ilha"))

            // When
            typeDigits("9000000000000001")

            // Then
            val state = viewModel.uiState.value
            assertEquals(9_000_000_000_000_001L, state.amountMinor)
            assertFalse(state.isAmountValid)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN a real group WHEN typing 22 million THEN Salvar is enabled under the 100 million limit`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trip),
                route = newRoute(TRIP_ID),
            )
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Ilha"))

            // When
            typeDigits("2200000000")

            // Then
            val state = viewModel.uiState.value
            assertEquals("22.000.000,00", state.amountText)
            assertTrue(state.isAmountValid)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a bitcoin group WHEN typing 22 million bitcoin THEN Salvar is disabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sats = false)
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Ilha"))

        // When
        typeDigits("2200000000000000")

        // Then
        val state = viewModel.uiState.value
        assertEquals("22.000.000,00000000", state.amountText)
        assertFalse(state.isAmountValid)
        assertFalse(state.canSave)
    }

    @Test
    fun `GIVEN a bill of 1000 sats WHEN opening Varias pessoas pagaram THEN its rows take whole sats`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = true)
            typeAmount("1000")
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // Then
            val sheet = sheet()
            assertTrue(sheet.showsSats)
            assertEquals("1.000", sheet.rows.first { it.memberId == PIERRE }.amountText)
            assertEquals(1_000L, sheet.paidMinor)
        }

    @Test
    fun `GIVEN Varias pessoas pagaram in sats WHEN typing what each paid THEN they add up in sats`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(sats = true)
            typeAmount("1000")
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPayerAmountChanged(PIERRE, "600"))
            viewModel.onEvent(SplitBillUiEvent.OnPayerAmountChanged(ANA, "400"))

            // Then
            val sheet = sheet()
            assertEquals("600", sheet.rows.first { it.memberId == PIERRE }.amountText)
            assertEquals("400", sheet.rows.first { it.memberId == ANA }.amountText)
            assertEquals(listOf(ExpensePayer(PIERRE, 600L), ExpensePayer(ANA, 400L)), sheet.payers)
            assertTrue(sheet.addsUp)
        }

    @Test
    fun `GIVEN Valores in sats WHEN typing each part THEN they add up in sats`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sats = true)
        typeAmount("1000")
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.AMOUNT))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(PIERRE, "600"))
        viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(ANA, "400"))
        viewModel.onEvent(SplitBillUiEvent.OnPersonValueChanged(BRUNO, ""))

        // Then
        val editor = editor()
        assertTrue(editor.showsSats)
        assertEquals(mapOf(ANA to "400", PIERRE to "600", BRUNO to ""), editor.valueByMember())
        assertEquals(mapOf(ANA to 400L, PIERRE to 600L, BRUNO to null), editor.shareByMember())
        assertTrue(editor.addsUp)
    }

    @Test
    fun `GIVEN Itens in sats WHEN typing an item price THEN the item costs whole sats`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sats = true)
        typeAmount("1000")
        openItems()
        viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnItemPriceChanged("1000"))

        // Then
        val item = assertNotNull(viewModel.uiState.value.itemEditor)
        assertTrue(assertNotNull(viewModel.uiState.value.itemsEditor).showsSats)
        assertTrue(item.showsSats)
        assertEquals("1.000", item.unitPriceText)
        assertEquals(1_000L, item.unitPriceMinor)
    }

    @Test
    fun `GIVEN an item split by Valor in sats WHEN typing a part THEN it is whole sats`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sats = true)
        typeAmount("1000")
        openItems()
        viewModel.onEvent(SplitBillUiEvent.OnAddItemClicked)
        viewModel.onEvent(SplitBillUiEvent.OnItemPriceChanged("1000"))
        viewModel.onEvent(SplitBillUiEvent.OnItemModeClicked(ItemSplitMode.AMOUNT))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnItemPersonValueChanged(PIERRE, "600"))

        // Then
        val item = assertNotNull(viewModel.uiState.value.itemEditor)
        assertEquals("600", item.rows.first { it.person.id == PIERRE }.valueText)
    }

    @Test
    fun `GIVEN Itens in sats WHEN typing the couvert THEN it is whole sats per person`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sats = true)
        typeAmount("1000")
        openItems()
        viewModel.onEvent(SplitBillUiEvent.OnFeeClicked(ItemsFee.COUVERT))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnFeeValueChanged("50"))

        // Then
        val fee = assertNotNull(viewModel.uiState.value.feeEditor)
        assertTrue(fee.showsSats)
        assertEquals("50", fee.valueText)
        assertEquals(50L, fee.value)
    }

    @Test
    fun `GIVEN a couvert typed in sats WHEN tapping Pronto THEN the items take it in sats`() = runTest(testDispatcher) {
        // Given
        prepareScenario(sats = true)
        typeAmount("1000")
        openItems()
        viewModel.onEvent(SplitBillUiEvent.OnFeeClicked(ItemsFee.COUVERT))
        viewModel.onEvent(SplitBillUiEvent.OnFeeValueChanged("50"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnFeeDone)

        // Then
        assertEquals(50L, assertNotNull(viewModel.uiState.value.itemsEditor).split.couvertMinor)
    }

    @Test
    fun `GIVEN a bitcoin bill and Bitcoin em satoshis on WHEN Editar conta opens THEN the amount is whole sats`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                sats = true,
                route = editRoute(),
                storedBills = listOf(bitcoinBill()),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isEditing)
            assertEquals("12.345", state.amountText)
            assertEquals(12_345L, state.amountMinor)
        }

    @Test
    fun `GIVEN a bitcoin bill and Bitcoin em satoshis off WHEN Editar conta opens THEN the amount has 8 decimals`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                sats = false,
                route = editRoute(),
                storedBills = listOf(bitcoinBill()),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("0,00012345", state.amountText)
            assertEquals(12_345L, state.amountMinor)
        }

    @Test
    fun `GIVEN a bitcoin bill by Valores WHEN Editar conta opens in sats THEN each part is whole sats`() =
        runTest(testDispatcher) {
            // Given
            val bill = bitcoinBill(
                splitMode = SplitMode.AMOUNT,
                shares = listOf(
                    ExpenseShare(PIERRE, 12_000L, weight = 12_000L),
                    ExpenseShare(ANA, 345L, weight = 345L),
                ),
            )
            prepareScenario(
                sats = true,
                route = editRoute(),
                storedBills = listOf(bill),
            )

            // When
            val split = viewModel.uiState.value.split

            // Then
            assertTrue(split.showsSats)
            assertEquals(mapOf(PIERRE to "12.000", ANA to "345", BRUNO to ""), split.valueByMember())
            assertTrue(split.addsUp)
        }

    @Test
    fun `GIVEN a bitcoin bill several people paid WHEN Editar conta opens in sats THEN each paid whole sats`() =
        runTest(testDispatcher) {
            // Given
            val bill = bitcoinBill(payers = listOf(ExpensePayer(PIERRE, 12_000L), ExpensePayer(ANA, 345L)))
            prepareScenario(
                sats = true,
                route = editRoute(),
                storedBills = listOf(bill),
            )

            // When
            val several = assertNotNull(viewModel.uiState.value.severalPayers)

            // Then
            assertTrue(several.showsSats)
            assertEquals("12.000", several.rows.first { it.memberId == PIERRE }.amountText)
            assertEquals("345", several.rows.first { it.memberId == ANA }.amountText)
            assertTrue(several.addsUp)
        }

    @Test
    fun `GIVEN a new bitcoin bill online WHEN it opens THEN Ler cupom fiscal is not offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.showsScanReceipt)
            assertFalse(state.canScanReceipt)
        }

    @Test
    fun `GIVEN a new ether bill online WHEN it opens THEN Ler cupom fiscal is not offered`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroups = listOf(ether),
            route = newRoute(ETH_ID),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.showsScanReceipt)
        assertFalse(state.canScanReceipt)
    }

    @Test
    fun `GIVEN a new bitcoin bill WHEN tapping Ler cupom fiscal THEN the camera does not open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)

            // Then
            assertFalse(viewModel.uiState.value.showsReceiptCamera)
        }

    @Test
    fun `GIVEN Inicio Ler cupom fiscal on a bitcoin group WHEN Rachar conta opens THEN the bill opens alone`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(route = newRoute(BTC_ID, scansReceipt = true))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertFalse(state.showsReceiptCamera)
            assertFalse(state.showsScanReceipt)
        }

    @Test
    fun `GIVEN Inicio Ler cupom fiscal on an ether group WHEN Rachar conta opens THEN the bill opens alone`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(ether),
                route = newRoute(ETH_ID, scansReceipt = true),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertFalse(state.showsReceiptCamera)
        }

    @Test
    fun `GIVEN Inicio Ler cupom fiscal on a real group WHEN Rachar conta opens THEN the camera opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trip),
                route = newRoute(TRIP_ID, scansReceipt = true),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsScanReceipt)
            assertTrue(state.showsReceiptCamera)
        }

    @Test
    fun `GIVEN Inicio Ler cupom fiscal asking the group WHEN a bitcoin group is picked THEN no camera opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trip, bitcoin),
                route = fromHome(TRIP_ID, scansReceipt = true),
            )
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(BTC_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            val state = viewModel.uiState.value
            assertEquals(BTC_ID, state.groupId)
            assertFalse(state.showsReceiptCamera)
            assertFalse(state.showsScanReceipt)
        }

    @Test
    fun `GIVEN Inicio Ler cupom fiscal asking the group WHEN a real group is picked THEN the camera opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(bitcoin, trip),
                route = fromHome(BTC_ID, scansReceipt = true),
            )
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(TRIP_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            val state = viewModel.uiState.value
            assertEquals(TRIP_ID, state.groupId)
            assertTrue(state.showsReceiptCamera)
        }

    @Test
    fun `GIVEN 12 reais and 50 typed WHEN moving the bill to a bitcoin group in sats THEN it is 12 sats`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trip, bitcoin),
                route = newRoute(TRIP_ID),
                sats = true,
            )
            typeAmount("12,50")
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(BTC_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.BTC, state.currency)
            assertEquals("12", state.amountText)
            assertEquals(12L, state.amountMinor)
            assertEquals(12L, state.split.totalMinor)
            assertTrue(state.split.showsSats)
        }

    @Test
    fun `GIVEN 12345 sats typed WHEN moving the bill to a real group THEN it is 12345 reais`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(bitcoin, trip),
                route = newRoute(BTC_ID),
                sats = true,
            )
            typeDigits("12345")
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(TRIP_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            val state = viewModel.uiState.value
            assertEquals(GroupCurrency.BRL, state.currency)
            assertEquals("12.345,00", state.amountText)
            assertEquals(1_234_500L, state.amountMinor)
        }

    @Test
    fun `GIVEN 12 reais and 50 typed WHEN moving the bill to a bitcoin group not in sats THEN keeps 12 and 50`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroups = listOf(trip, bitcoin),
                route = newRoute(TRIP_ID),
                sats = false,
            )
            typeAmount("12,50")
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(BTC_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            val state = viewModel.uiState.value
            assertEquals("12,50000000", state.amountText)
            assertEquals(1_250_000_000L, state.amountMinor)
        }

    // Why: the field emptied, then the amount put in whole, as pasting it does.
    private fun typeAmount(text: String) {
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged(""))
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged(text))
    }

    // Why: the field emptied, then each digit typed after what it shows, as the keyboard does.
    private fun typeDigits(digits: String) {
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged(""))
        digits.forEach { digit ->
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged(viewModel.uiState.value.amountText + digit))
        }
    }

    private fun openItems() {
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.ITEMS))
    }

    private fun sheet(): PayerAmounts = assertNotNull(viewModel.uiState.value.payerAmounts)

    private fun editor(): SplitEditor = assertNotNull(viewModel.uiState.value.splitEditor)

    private fun SplitEditor.shareByMember(): Map<String, Long?> = rows.associate { it.person.id to it.shareMinor }

    private fun SplitEditor.valueByMember(): Map<String, String> = rows.associate { it.person.id to it.valueText }

    private fun newRoute(
        groupId: String = BTC_ID,
        scansReceipt: Boolean = false,
    ): Route.SplitBill = Route.SplitBill(
        groupId,
        expenseId = null,
        picksGroupFirst = false,
        scansReceipt = scansReceipt,
    )

    private fun editRoute(): Route.SplitBill = Route.SplitBill(
        BTC_ID,
        EXPENSE_ID,
        picksGroupFirst = false,
        scansReceipt = false,
    )

    private fun fromHome(
        groupId: String,
        scansReceipt: Boolean,
    ): Route.SplitBill = Route.SplitBill(
        groupId,
        expenseId = null,
        picksGroupFirst = true,
        scansReceipt = scansReceipt,
    )

    // Why: "Pizza", 12.345 sats Pierre paid, split equally among Pierre, Ana and Bruno unless said otherwise.
    private fun bitcoinBill(
        splitMode: SplitMode = SplitMode.EQUAL,
        payers: List<ExpensePayer> = listOf(ExpensePayer(PIERRE, 12_345L)),
        shares: List<ExpenseShare> = listOf(
            ExpenseShare(PIERRE, 4_115L, weight = 1L),
            ExpenseShare(ANA, 4_115L, weight = 1L),
            ExpenseShare(BRUNO, 4_115L, weight = 1L),
        ),
    ): Expense = Expense(
        id = EXPENSE_ID,
        groupId = BTC_ID,
        description = "Pizza",
        amountMinor = 12_345L,
        splitMode = splitMode,
        payers = payers,
        shares = shares,
        items = null,
        categoryId = null,
        spentAt = NOW_MILLIS,
        isPending = false,
        updatedAt = NOW_MILLIS,
        updatedByMemberId = PIERRE,
    )

    private val pierre = member(PIERRE, "Pierre", isMe = true)
    private val ana = member(ANA, "Ana", isMe = false)
    private val bruno = member(BRUNO, "Bruno", isMe = false)

    private val bitcoin = group(BTC_ID, "Satoshis", GroupCurrency.BTC)
    private val ether = group(ETH_ID, "Gás", GroupCurrency.ETH)
    private val trip = group(TRIP_ID, "Viagem Floripa", GroupCurrency.BRL)

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
        members = listOf(pierre, ana, bruno),
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
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = true,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun prepareScenario(
        storedGroups: List<Group> = listOf(bitcoin),
        route: Route.SplitBill = newRoute(),
        sats: Boolean = true,
        storedBills: List<Expense> = emptyList(),
        decimalSeparator: Char = ',',
    ) {
        groups = FakeGroupRepository(initialGroups = storedGroups)
        expenses = FakeExpenseRepository(initialExpenses = storedBills)
        bitcoinInSats = MutableStateFlow(sats)
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            if (!route.picksGroupFirst) {
                it.navigate(
                    Route.GroupDetail(
                        route.groupId,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                    ),
                )
            }
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
            readReceipt = { _, _, _ ->
                ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE)
            },
            attachBillImage = { _, _, _, _, _ -> Result.success(BillAttachmentAddition.ADDED) },
            observeBitcoinInSats = { bitcoinInSats },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
        )
    }

    private companion object {
        const val BTC_ID = "bitcoin"
        const val ETH_ID = "ether"
        const val TRIP_ID = "trip"
        const val EXPENSE_ID = "expense-1"
        const val PIERRE = "member-1"
        const val ANA = "member-2"
        const val BRUNO = "member-3"

        // Why: 2026-10-30T15:00:00Z.
        const val NOW_MILLIS = 1_793_372_400_000L
    }
}
