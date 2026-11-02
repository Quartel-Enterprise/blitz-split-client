package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptItem
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
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
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillReceipt
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategoryHint
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategorySuggestionReason
import com.quare.blitzsplit.feature.splitbill.presentation.model.ReceiptStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: "Ler cupom fiscal" (7.1e to 7.1g) on Rachar conta: the camera, the reading, what it fills and the photo saved.
class ReceiptViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: SplitBillViewModel
    private val readSources = mutableListOf<List<Byte>>()
    private val readCurrencies = mutableListOf<GroupCurrency>()
    private val pendingReads = mutableListOf<CompletableDeferred<ReceiptScan>>()
    private val attachments = mutableListOf<RecordedAttachment>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a new bill online WHEN it opens THEN offers Ler cupom fiscal enabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsScanReceipt)
        assertTrue(state.canScanReceipt)
        assertFalse(state.showsReceiptCamera)
        assertNull(state.receipt)
    }

    @Test
    fun `GIVEN a new bill offline WHEN it opens THEN Ler cupom fiscal shows disabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsScanReceipt)
        assertFalse(state.canScanReceipt)
    }

    @Test
    fun `GIVEN a new bill offline WHEN tapping Ler cupom fiscal THEN the camera does not open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)

            // Then
            assertFalse(viewModel.uiState.value.isReceiptCameraRequested)
            assertFalse(viewModel.uiState.value.showsReceiptCamera)
        }

    @Test
    fun `GIVEN Editar conta WHEN it opens THEN Ler cupom fiscal is not offered`() = runTest(testDispatcher) {
        // Given
        prepareScenario(route = editRoute(scansReceipt = false))

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoaded)
        assertFalse(state.showsScanReceipt)
        assertFalse(state.canScanReceipt)
    }

    @Test
    fun `GIVEN Editar conta WHEN tapping Ler cupom fiscal THEN the camera does not open`() = runTest(testDispatcher) {
        // Given
        prepareScenario(route = editRoute(scansReceipt = false))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)

        // Then
        assertFalse(viewModel.uiState.value.showsReceiptCamera)
    }

    @Test
    fun `GIVEN Editar conta WHEN a receipt photo arrives THEN nothing is read and the bill stays as saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(route = editRoute(scansReceipt = false))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertTrue(readSources.isEmpty())
            assertNull(state.receipt)
            assertEquals("100,00", state.amountText)
            assertEquals("Jantar", state.description)
        }

    @Test
    fun `GIVEN an edit route asking to scan WHEN it opens THEN the camera does not open`() = runTest(testDispatcher) {
        // Given
        prepareScenario(route = editRoute(scansReceipt = true))

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isReceiptCameraRequested)
        assertFalse(state.showsReceiptCamera)
    }

    @Test
    fun `GIVEN Inicio shortcut to a group WHEN Rachar conta opens THEN the camera opens at once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(route = newRoute(scansReceipt = true))

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertTrue(state.showsReceiptCamera)
        }

    @Test
    fun `GIVEN Inicio shortcut with two groups WHEN Rachar em qual grupo is open THEN the camera waits`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, trip),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNotNull(state.groupSelection)
            assertFalse(state.showsReceiptCamera)
        }

    @Test
    fun `GIVEN Inicio shortcut with two groups WHEN tapping Continuar THEN the camera opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, trip),
            )
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(TRIP_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.groupSelection)
            assertEquals(TRIP_ID, state.groupId)
            assertTrue(state.showsReceiptCamera)
        }

    @Test
    fun `GIVEN another group picked WHEN a receipt photo arrives THEN it is read in that group currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, trip),
            )
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(TRIP_ID))
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            assertEquals(listOf(GroupCurrency.USD), readCurrencies)
        }

    @Test
    fun `GIVEN a new bill WHEN tapping Ler cupom fiscal THEN the camera opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)

        // Then
        assertTrue(viewModel.uiState.value.showsReceiptCamera)
    }

    @Test
    fun `GIVEN the camera open WHEN it closes without a photo THEN nothing changes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptCameraDismissed)

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showsReceiptCamera)
        assertNull(state.receipt)
        assertTrue(state.showsScanReceipt)
        assertTrue(readSources.isEmpty())
    }

    @Test
    fun `GIVEN the camera open WHEN a photo is picked THEN the camera closes and the receipt is being read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.showsReceiptCamera)
            assertEquals(
                BillReceipt(
                    ReceiptStatus.READING,
                    itemCount = 0,
                    hasPhoto = true,
                ),
                state.receipt,
            )
            assertTrue(state.isReadingReceipt)
            assertFalse(state.showsScanReceipt)
            assertFalse(state.canScanReceipt)
            assertEquals(listOf(source.toList()), readSources)
        }

    @Test
    fun `GIVEN a filled bill WHEN a receipt is being read THEN Salvar is disabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario(holdsReads = true)
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

        // Then
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN a receipt being read WHEN tapping Salvar THEN nothing is saved`() = runTest(testDispatcher) {
        // Given
        prepareScenario(holdsReads = true)
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        assertTrue(expenses.createdExpenses.isEmpty())
        assertTrue(navigator.backStack.value.last() is Route.SplitBill)
    }

    @Test
    fun `GIVEN a receipt read WHEN the reading comes back THEN fills the amount and description and the split total`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = scanOf(reading()))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertEquals(
                BillReceipt(
                    ReceiptStatus.READ,
                    itemCount = 2,
                    hasPhoto = true,
                ),
                state.receipt,
            )
            assertFalse(state.isReadingReceipt)
            assertEquals("259,90", state.amountText)
            assertTrue(state.hasEditedAmount)
            assertEquals(25_990L, state.amountMinor)
            assertEquals("Bom Preço", state.description)
            assertEquals(25_990L, state.split.totalMinor)
            assertEquals(25_990L, state.split.shares?.sumOf { it.amountMinor })
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a description typed WHEN a receipt without a place is read THEN keeps the description`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = scanOf(reading(description = null)))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Compras da semana"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertEquals("Compras da semana", state.description)
            assertEquals("259,90", state.amountText)
        }

    @Test
    fun `GIVEN a receipt with its date and time WHEN read THEN Quando is the printed date and time`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                scan = scanOf(
                    reading(
                        date = LocalDate(2026, 10, 28),
                        time = LocalTime(12, 34),
                    ),
                ),
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            assertEquals(millisOf(2026, 10, 28, 12, 34), viewModel.uiState.value.spentAt)
        }

    @Test
    fun `GIVEN a receipt with a date but no time WHEN read THEN Quando keeps the time it had on the printed date`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                scan = scanOf(
                    reading(
                        date = LocalDate(2026, 10, 25),
                        time = null,
                    ),
                ),
            )
            viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(millisOf(2026, 10, 29, 9, 30)))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            assertEquals(millisOf(2026, 10, 25, 9, 30), viewModel.uiState.value.spentAt)
        }

    @Test
    fun `GIVEN a receipt printed later today than now WHEN read THEN Quando is now`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            scan = scanOf(
                reading(
                    date = LocalDate(2026, 10, 30),
                    time = LocalTime(18, 0),
                ),
            ),
        )

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

        // Then
        assertEquals(NOW_MILLIS, viewModel.uiState.value.spentAt)
    }

    @Test
    fun `GIVEN a receipt without a date WHEN read THEN Quando stays as it was`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            scan = scanOf(
                reading(
                    date = null,
                    time = LocalTime(12, 34),
                ),
            ),
        )
        val picked = millisOf(2026, 10, 29, 9, 30)
        viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(picked))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

        // Then
        assertEquals(picked, viewModel.uiState.value.spentAt)
    }

    @Test
    fun `GIVEN a place the map has no word for WHEN the receipt is read THEN the bill takes the receipt category`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = scanOf(reading(categoryId = AppCategory.FOOD_MARKET.key)))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertEquals(AppCategory.FOOD_MARKET.key, state.category.id)
            assertEquals(AppCategory.FOOD_MARKET.key, state.receiptCategory?.id)
            assertEquals(CategoryHint.BY_RECEIPT, state.categoryHint)
            val suggestion = state.categorySuggestions.single { it.category.id == AppCategory.FOOD_MARKET.key }
            assertEquals(CategorySuggestionReason.RECEIPT, suggestion.reason)
            assertTrue(suggestion.isSelected)
        }

    @Test
    fun `GIVEN a place the map has a word for WHEN the receipt is read THEN the map category wins`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                scan = scanOf(
                    reading(
                        description = "Supermercado Bom Preço",
                        categoryId = AppCategory.SHOPPING.key,
                    ),
                ),
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertEquals(AppCategory.FOOD_MARKET.key, state.category.id)
            assertEquals(CategoryHint.BY_TERM, state.categoryHint)
            assertEquals(
                listOf(
                    AppCategory.FOOD_MARKET.key to CategorySuggestionReason.DESCRIPTION,
                    AppCategory.SHOPPING.key to CategorySuggestionReason.RECEIPT,
                ),
                state.categorySuggestions.take(2).map { it.category.id to it.reason },
            )
        }

    @Test
    fun `GIVEN a category picked by hand WHEN the receipt is read THEN the picked category stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = scanOf(reading(categoryId = AppCategory.FOOD_MARKET.key)))
            viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
            viewModel.onEvent(SplitBillUiEvent.OnCategoryPicked(AppCategory.LEISURE_SPORTS.key))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertEquals(AppCategory.LEISURE_SPORTS.key, state.category.id)
            assertEquals(CategoryHint.PICKED, state.categoryHint)
        }

    @Test
    fun `GIVEN a receipt read WHEN the description changes to a word the map knows THEN the map category wins`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = scanOf(reading(categoryId = AppCategory.FOOD_MARKET.key)))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Farmácia"))

            // Then
            val state = viewModel.uiState.value
            assertEquals(AppCategory.HEALTH_PHARMACY.key, state.category.id)
            assertEquals(CategoryHint.BY_TERM, state.categoryHint)
        }

    @Test
    fun `GIVEN a receipt with items WHEN read THEN the bill split stays as it was`() = runTest(testDispatcher) {
        // Given
        prepareScenario(scan = scanOf(reading()))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

        // Then
        val split = viewModel.uiState.value.split
        assertEquals(SplitMode.EQUAL, split.mode)
        assertNull(split.items)
        assertEquals(listOf(PIERRE, ANA, BRUNO), split.included.map { it.id })
    }

    @Test
    fun `GIVEN a receipt with items read WHEN opening Itens THEN they come with nobody marked and its service`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = scanOf(reading()))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.ITEMS))

            // Then
            val state = viewModel.uiState.value
            val items = assertNotNull(state.itemsEditor)
            assertEquals(
                listOf(
                    Triple("Pizza", 2L, 9_900L),
                    Triple("Refrigerante", 1L, 800L),
                ),
                items.items.map { Triple(it.name, it.quantity, it.unitPriceMinor) },
            )
            assertTrue(items.items.all { item -> item.entries.none { it.value > 0 } })
            assertTrue(items.rows.all { it.consumers.isEmpty() })
            assertTrue(items.split.isFromReceipt)
            assertEquals(15L, items.split.servicePercent)
            assertEquals(0L, items.split.couvertMinor)
            assertFalse(items.addsUp)
            assertEquals(SplitMode.EQUAL, state.split.mode)
        }

    @Test
    fun `GIVEN a receipt that lists no items WHEN read and opening Itens THEN Itens opens empty`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = scanOf(reading(items = emptyList())))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.ITEMS))

            // Then
            assertEquals(
                0,
                viewModel.uiState.value.receipt
                    ?.itemCount,
            )
            val items = assertNotNull(viewModel.uiState.value.itemsEditor)
            assertTrue(items.items.isEmpty())
            assertFalse(items.split.isFromReceipt)
        }

    @Test
    fun `GIVEN a photo nothing could be read from WHEN the reading comes back THEN says so and leaves the fields`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                scan = ReceiptScan(
                    photo = encodedPhoto,
                    reading = null,
                ),
            )
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertEquals(
                BillReceipt(
                    ReceiptStatus.UNREADABLE,
                    itemCount = 0,
                    hasPhoto = true,
                ),
                state.receipt,
            )
            assertTrue(state.showsReceiptFailure)
            assertFalse(state.isReadingReceipt)
            assertEquals("", state.amountText)
            assertFalse(state.hasEditedAmount)
            assertEquals("Jantar", state.description)
            assertEquals(NOW_MILLIS, state.spentAt)
            assertNull(state.receiptCategoryId)
            assertTrue(state.canScanReceipt)
        }

    @Test
    fun `GIVEN an image that could not be opened WHEN the reading comes back THEN the receipt has no photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                scan = ReceiptScan(
                    photo = null,
                    reading = null,
                ),
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            assertEquals(
                BillReceipt(
                    ReceiptStatus.UNREADABLE,
                    itemCount = 0,
                    hasPhoto = false,
                ),
                viewModel.uiState.value.receipt,
            )
        }

    @Test
    fun `GIVEN a receipt that could not be read WHEN tapping Tirar outra foto THEN the camera opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                scan = ReceiptScan(
                    photo = encodedPhoto,
                    reading = null,
                ),
            )
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)

            // Then
            assertTrue(viewModel.uiState.value.showsReceiptCamera)
        }

    @Test
    fun `GIVEN a receipt that could not be read WHEN another photo is read THEN it fills the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            answerRead(
                0,
                ReceiptScan(
                    photo = encodedPhoto,
                    reading = null,
                ),
            )
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(otherSource))

            // When
            answerRead(1, scanOf(reading()))

            // Then
            val state = viewModel.uiState.value
            assertEquals(ReceiptStatus.READ, state.receipt?.status)
            assertFalse(state.showsReceiptFailure)
            assertEquals("259,90", state.amountText)
        }

    @Test
    fun `GIVEN a receipt read WHEN removing it THEN the scan button is back and what it filled stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                scan = scanOf(
                    reading(
                        date = LocalDate(2026, 10, 28),
                        time = LocalTime(12, 34),
                    ),
                ),
            )
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRemoved)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.receipt)
            assertTrue(state.showsScanReceipt)
            assertEquals("259,90", state.amountText)
            assertEquals("Bom Preço", state.description)
            assertEquals(millisOf(2026, 10, 28, 12, 34), state.spentAt)
        }

    @Test
    fun `GIVEN the receipt chose the category WHEN removing it THEN the category falls back to Outros`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = scanOf(reading(categoryId = AppCategory.FOOD_MARKET.key)))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRemoved)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.receiptCategoryId)
            assertNull(state.receiptCategory)
            assertTrue(state.category.isOther)
            assertEquals(CategoryHint.NO_TERM, state.categoryHint)
            assertTrue(state.categorySuggestions.none { it.reason == CategorySuggestionReason.RECEIPT })
        }

    @Test
    fun `GIVEN a category picked by hand over a receipt WHEN removing the receipt THEN the picked category stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = scanOf(reading(categoryId = AppCategory.FOOD_MARKET.key)))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
            viewModel.onEvent(SplitBillUiEvent.OnCategoryPicked(AppCategory.LEISURE_SPORTS.key))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRemoved)

            // Then
            assertEquals(AppCategory.LEISURE_SPORTS.key, viewModel.uiState.value.category.id)
        }

    @Test
    fun `GIVEN a receipt with items removed WHEN opening Itens THEN Itens opens empty`() = runTest(testDispatcher) {
        // Given
        prepareScenario(scan = scanOf(reading()))
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        viewModel.onEvent(SplitBillUiEvent.OnReceiptRemoved)
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.ITEMS))

        // Then
        val items = assertNotNull(viewModel.uiState.value.itemsEditor)
        assertTrue(items.items.isEmpty())
        assertFalse(items.split.isFromReceipt)
    }

    @Test
    fun `GIVEN a receipt being read WHEN removing it THEN a late answer never fills the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRemoved)

            // When
            answerRead(0, scanOf(reading()))

            // Then
            val state = viewModel.uiState.value
            assertNull(state.receipt)
            assertEquals("", state.amountText)
            assertEquals("", state.description)
            assertNull(state.receiptCategoryId)
            assertTrue(state.canScanReceipt)
        }

    @Test
    fun `GIVEN a receipt being read WHEN a second photo is read THEN only the second fills the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(otherSource))
            answerRead(
                1,
                scanOf(
                    reading(
                        totalMinor = 4_250L,
                        description = "Padaria Central",
                    ),
                ),
            )

            // When
            answerRead(0, scanOf(reading()))

            // Then
            val state = viewModel.uiState.value
            assertEquals("42,50", state.amountText)
            assertEquals("Padaria Central", state.description)
            assertEquals(listOf(source.toList(), otherSource.toList()), readSources)
        }

    @Test
    fun `GIVEN a receipt being read WHEN a second photo arrives THEN it is still reading`() = runTest(testDispatcher) {
        // Given
        prepareScenario(holdsReads = true)
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(otherSource))

        // When
        answerRead(0, scanOf(reading()))

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isReadingReceipt)
        assertEquals("", state.amountText)
    }

    @Test
    fun `GIVEN a new bill with a receipt read WHEN saving THEN attaches the receipt photo to the new bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = scanOf(reading()))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val created = expenses.createdExpenses.single()
            assertEquals(
                listOf(
                    RecordedAttachment(
                        expenseId = created.id,
                        groupId = GROUP_ID,
                        kind = BillAttachmentKind.RECEIPT,
                        source = encodedPhoto.toList(),
                        addedByMemberId = PIERRE,
                    ),
                ),
                attachments,
            )
            assertEquals(25_990L, created.amountMinor)
            assertEquals("Bom Preço", created.description)
        }

    @Test
    fun `GIVEN a receipt that could not be read WHEN saving the bill typed by hand THEN attaches its photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                scan = ReceiptScan(
                    photo = encodedPhoto,
                    reading = null,
                ),
            )
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(listOf(encodedPhoto.toList()), attachments.map { it.source })
        }

    @Test
    fun `GIVEN a new bill without a receipt WHEN saving THEN nothing is attached`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        assertEquals(1, expenses.createdExpenses.size)
        assertTrue(attachments.isEmpty())
    }

    @Test
    fun `GIVEN a receipt read and removed WHEN saving THEN nothing is attached`() = runTest(testDispatcher) {
        // Given
        prepareScenario(scan = scanOf(reading()))
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        viewModel.onEvent(SplitBillUiEvent.OnReceiptRemoved)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        assertEquals(1, expenses.createdExpenses.size)
        assertTrue(attachments.isEmpty())
    }

    @Test
    fun `GIVEN the receipt photo fails to attach WHEN saving THEN the bill is saved and the form leaves`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                scan = scanOf(reading()),
                attachResult = Result.failure(IllegalStateException("upload failed")),
            )
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(1, expenses.createdExpenses.size)
            assertEquals(1, attachments.size)
            assertEquals(
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
                navigator.backStack.value.last(),
            )
        }

    private fun TestScope.answerRead(
        index: Int,
        scan: ReceiptScan,
    ) {
        pendingReads[index].complete(scan)
        runCurrent()
    }

    private fun millisOf(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = LocalDateTime(year, month, day, hour, minute).toInstant(TimeZone.UTC).toEpochMilliseconds()

    private fun scanOf(reading: ReceiptReading): ReceiptScan = ReceiptScan(
        photo = encodedPhoto,
        reading = reading,
    )

    private fun reading(
        totalMinor: Long = 25_990L,
        description: String? = "Bom Preço",
        categoryId: String? = null,
        date: LocalDate? = null,
        time: LocalTime? = null,
        items: List<ReceiptItem> = receiptItems,
    ): ReceiptReading = ReceiptReading(
        totalMinor = totalMinor,
        description = description,
        categoryId = categoryId,
        date = date,
        time = time,
        items = items,
        servicePercent = 15L,
    )

    private val source = byteArrayOf(1, 2, 3)

    private val otherSource = byteArrayOf(4, 5, 6)

    private val encodedPhoto = byteArrayOf(9, 8, 7)

    private val receiptItems = listOf(
        ReceiptItem(
            name = "Pizza",
            quantity = 2L,
            unitPriceMinor = 9_900L,
        ),
        ReceiptItem(
            name = "Refrigerante",
            quantity = 1L,
            unitPriceMinor = 800L,
        ),
    )

    private val dinner = group(
        id = GROUP_ID,
        name = "Jantar",
        currency = GroupCurrency.BRL,
        members = listOf(
            member(PIERRE, "Pierre", isMe = true),
            member(ANA, "Ana", isMe = false),
            member(BRUNO, "Bruno", isMe = false),
        ),
    )

    private val trip = group(
        id = TRIP_ID,
        name = "Viagem NY",
        currency = GroupCurrency.USD,
        members = listOf(
            member(PIERRE_TRIP, "Pierre", isMe = true),
            member(CARLA, "Carla", isMe = false),
        ),
    )

    private val storedBill = Expense(
        id = EXPENSE_ID,
        groupId = GROUP_ID,
        description = "Jantar",
        amountMinor = 10_000L,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(PIERRE, 10_000L)),
        shares = listOf(ExpenseShare(PIERRE, 1L, 5_000L), ExpenseShare(ANA, 1L, 5_000L)),
        items = null,
        categoryId = null,
        spentAt = NOW_MILLIS,
        isPending = false,
        updatedAt = NOW_MILLIS,
        updatedByMemberId = null,
    )

    private fun newRoute(scansReceipt: Boolean = false): Route.SplitBill = Route.SplitBill(
        GROUP_ID,
        expenseId = null,
        picksGroupFirst = false,
        scansReceipt = scansReceipt,
    )

    private fun editRoute(scansReceipt: Boolean): Route.SplitBill = Route.SplitBill(
        GROUP_ID,
        EXPENSE_ID,
        picksGroupFirst = false,
        scansReceipt = scansReceipt,
    )

    private fun fromHome(scansReceipt: Boolean): Route.SplitBill = Route.SplitBill(
        GROUP_ID,
        expenseId = null,
        picksGroupFirst = true,
        scansReceipt = scansReceipt,
    )

    private fun group(
        id: String,
        name: String,
        currency: GroupCurrency,
        members: List<GroupMember>,
    ): Group = Group(
        id = id,
        name = name,
        type = GroupType.EVENT,
        color = AvatarColor.GREEN,
        currency = currency,
        members = members,
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
        hasJoined = isMe,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun prepareScenario(
        route: Route.SplitBill = newRoute(),
        storedGroups: List<Group> = listOf(dinner),
        online: Boolean = true,
        scan: ReceiptScan = ReceiptScan(
            photo = encodedPhoto,
            reading = null,
        ),
        holdsReads: Boolean = false,
        attachResult: Result<BillAttachmentAddition> = Result.success(BillAttachmentAddition.ADDED),
    ) {
        groups = FakeGroupRepository(initialGroups = storedGroups)
        expenses = FakeExpenseRepository(initialExpenses = listOf(storedBill))
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            if (!route.picksGroupFirst) {
                it.navigate(
                    Route.GroupDetail(
                        GROUP_ID,
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
            decimalSeparator = { ',' },
            observeGroup = ObserveGroupUseCase(groups),
            observeExpense = ObserveExpenseUseCase(expenses),
            observeGroupsByActivity = { groups.groups },
            observeExpenses = ObserveExpensesUseCase(expenses),
            observeSettlements = { flowOf(emptyList()) },
            observeCategoryTree = ObserveCategoryTreeUseCase(FakeCategoryRepository()),
            observeCategoryMap = ObserveCategoryMapUseCase(FakeCategoryTermRepository()),
            offerCategoryTeach = {},
            readReceipt = { photo, currency ->
                readSources += photo.toList()
                readCurrencies += currency
                if (holdsReads) {
                    CompletableDeferred<ReceiptScan>().also { pendingReads += it }.await()
                } else {
                    scan
                }
            },
            addBillAttachment = { expenseId, groupId, kind, photo, addedByMemberId ->
                attachments += RecordedAttachment(expenseId, groupId, kind, photo.toList(), addedByMemberId)
                attachResult
            },
            saveGroupCategory = { _, _, _, _, _, _ -> Result.success("") },
            deleteGroupCategory = { Result.success(Unit) },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(online) },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val TRIP_ID = "group-2"
        const val EXPENSE_ID = "expense-1"
        const val PIERRE = "member-1"
        const val ANA = "member-2"
        const val BRUNO = "member-3"
        const val PIERRE_TRIP = "member-4"
        const val CARLA = "member-5"

        // Why: 2026-10-30T15:00:00Z.
        const val NOW_MILLIS = 1_793_372_400_000L
    }
}
