package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryMatcher
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTerm
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTermChange
import com.quare.blitzsplit.core.expenses.domain.model.DefaultCategoryTerms
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptCategoryHints
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptItem
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReadFailure
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
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.ReceiptPhotoSource
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.provider.image.BillImage
import com.quare.blitzsplit.feature.splitbill.presentation.model.BillReceipt
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategoryHint
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategorySuggestionReason
import com.quare.blitzsplit.feature.splitbill.presentation.model.ReceiptError
import com.quare.blitzsplit.feature.splitbill.presentation.model.ReceiptRequest
import com.quare.blitzsplit.feature.splitbill.presentation.model.ReceiptStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiAction
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.BillCategoryUseCases
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var categories: FakeCategoryRepository
    private lateinit var terms: FakeCategoryTermRepository
    private lateinit var viewModel: SplitBillViewModel
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private val readSources = mutableListOf<List<Byte>>()
    private val readCurrencies = mutableListOf<GroupCurrency>()
    private val readHints = mutableListOf<ReceiptCategoryHints>()
    private val pendingReads = mutableListOf<CompletableDeferred<ReceiptScan>>()
    private val attachments = mutableListOf<RecordedAttachment>()
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
    fun `GIVEN a new bill online WHEN it opens THEN offers Ler cupom fiscal enabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsScanReceipt)
        assertTrue(state.canScanReceipt)
        assertFalse(state.showsReceiptSourceChoice)
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
            assertFalse(viewModel.uiState.value.isReceiptSourceRequested)
            assertFalse(viewModel.uiState.value.showsReceiptSourceChoice)
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
        assertFalse(viewModel.uiState.value.showsReceiptSourceChoice)
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
        assertFalse(state.isReceiptSourceRequested)
        assertFalse(state.showsReceiptSourceChoice)
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
            assertTrue(state.showsReceiptSourceChoice)
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
            assertFalse(state.showsReceiptSourceChoice)
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
            assertTrue(state.showsReceiptSourceChoice)
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
        assertTrue(viewModel.uiState.value.showsReceiptSourceChoice)
    }

    @Test
    fun `GIVEN the camera open WHEN it closes without a photo THEN nothing changes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPickerDismissed)

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showsReceiptSourceChoice)
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
            assertFalse(state.showsReceiptSourceChoice)
            assertEquals(
                BillReceipt(
                    ReceiptStatus.READING,
                    itemCount = 0,
                    hasPhoto = true,
                    error = null,
                ),
                state.receipt,
            )
            assertTrue(state.isReadingReceipt)
            assertFalse(state.showsScanReceipt)
            assertFalse(state.canScanReceipt)
            assertEquals(listOf(source.toList()), readSources)
        }

    @Test
    fun `GIVEN the choice open WHEN picking Tirar foto THEN the choice closes and the camera opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isReceiptSourceRequested)
            assertFalse(state.showsReceiptSourceChoice)
            assertEquals(ReceiptPhotoSource.CAMERA, state.receiptSource)
            assertEquals(ReceiptPhotoSource.CAMERA, state.shownReceiptSource)
            assertTrue(readSources.isEmpty())
        }

    @Test
    fun `GIVEN the receipt camera open WHEN turning the flashlight on THEN the camera stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))
            backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
            val before = viewModel.uiState.value

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptTorchClicked(isOn = true))

            // Then
            assertEquals(before, viewModel.uiState.value)
            assertEquals(ReceiptPhotoSource.CAMERA, viewModel.uiState.value.shownReceiptSource)
            assertTrue(actions.isEmpty())
            assertTrue(readSources.isEmpty())
        }

    @Test
    fun `GIVEN the receipt camera open WHEN tapping Allow camera THEN the camera stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))
            backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
            val before = viewModel.uiState.value

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptCameraAllowClicked)

            // Then
            assertEquals(before, viewModel.uiState.value)
            assertEquals(ReceiptPhotoSource.CAMERA, viewModel.uiState.value.shownReceiptSource)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the receipt camera open WHEN tapping Open settings THEN the camera stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))
            backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
            val before = viewModel.uiState.value

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptCameraSettingsClicked)

            // Then
            assertEquals(before, viewModel.uiState.value)
            assertEquals(ReceiptPhotoSource.CAMERA, viewModel.uiState.value.shownReceiptSource)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN a receipt being read WHEN turning the flashlight on THEN the receipt is still being read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
            val before = viewModel.uiState.value

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptTorchClicked(isOn = true))

            // Then
            assertEquals(before, viewModel.uiState.value)
            assertEquals(
                ReceiptStatus.READING,
                viewModel.uiState.value.receipt
                    ?.status,
            )
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN Inicio shortcut to a group WHEN picking Escolher da galeria THEN the gallery opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(route = newRoute(scansReceipt = true))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.PHOTOS))

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.showsReceiptSourceChoice)
            assertEquals(ReceiptPhotoSource.PHOTOS, state.shownReceiptSource)
        }

    @Test
    fun `GIVEN the choice open WHEN picking Escolher arquivo THEN the file chooser opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.FILES))

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showsReceiptSourceChoice)
        assertEquals(ReceiptPhotoSource.FILES, state.shownReceiptSource)
    }

    @Test
    fun `GIVEN the choice asked for without a camera WHEN it is skipped THEN the gallery opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptChoiceSkipped(ReceiptPhotoSource.PHOTOS))

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.showsReceiptSourceChoice)
            assertEquals(ReceiptPhotoSource.PHOTOS, state.shownReceiptSource)
        }

    @Test
    fun `GIVEN the choice never asked for WHEN it is skipped THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptChoiceSkipped(ReceiptPhotoSource.FILES))

        // Then
        assertNull(viewModel.uiState.value.shownReceiptSource)
    }

    @Test
    fun `GIVEN the choice never asked for WHEN a source is picked THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isReceiptSourceRequested)
        assertNull(state.receiptSource)
        assertNull(state.shownReceiptSource)
    }

    @Test
    fun `GIVEN the camera already picked WHEN a late tap picks the gallery THEN the camera stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.PHOTOS))

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.showsReceiptSourceChoice)
            assertEquals(ReceiptPhotoSource.CAMERA, state.shownReceiptSource)
        }

    @Test
    fun `GIVEN Rachar em qual grupo open WHEN a source is picked THEN the request still waits`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, trip),
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))

            // Then
            val state = viewModel.uiState.value
            assertNotNull(state.groupSelection)
            assertEquals(ReceiptRequest.Picked, state.receiptRequest)
            assertNull(state.receiptSource)
            assertNull(state.shownReceiptSource)
        }

    @Test
    fun `GIVEN a source tapped while Rachar em qual grupo is open WHEN tapping Continuar THEN the choice opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, trip),
            )
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(TRIP_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsReceiptSourceChoice)
            assertNull(state.shownReceiptSource)
        }

    @Test
    fun `GIVEN Rachar em qual grupo open WHEN the choice is skipped THEN the choice still asks after Continuar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, trip),
            )
            viewModel.onEvent(SplitBillUiEvent.OnReceiptChoiceSkipped(ReceiptPhotoSource.FILES))
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(TRIP_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsReceiptSourceChoice)
            assertNull(state.receiptSource)
        }

    @Test
    fun `GIVEN the choice open and the bill saving WHEN a source is picked THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            expenses.createGate = CompletableDeferred()
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isSaving)
            assertNull(state.receiptSource)
            assertNull(state.shownReceiptSource)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the camera picked WHEN tapping Ler cupom fiscal again THEN the choice opens without the camera`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsReceiptSourceChoice)
            assertNull(state.receiptSource)
            assertNull(state.shownReceiptSource)
        }

    @Test
    fun `GIVEN the gallery open WHEN it closes without a photo THEN neither the choice nor the gallery shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.PHOTOS))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPickerDismissed)

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isReceiptSourceRequested)
            assertFalse(state.showsReceiptSourceChoice)
            assertNull(state.receiptSource)
            assertNull(state.shownReceiptSource)
            assertNull(state.receipt)
            assertTrue(readSources.isEmpty())
        }

    @Test
    fun `GIVEN the gallery open WHEN a photo is picked THEN the gallery closes and the receipt is being read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.PHOTOS))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isReceiptSourceRequested)
            assertNull(state.receiptSource)
            assertNull(state.shownReceiptSource)
            assertEquals(ReceiptStatus.READING, state.receipt?.status)
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
                    error = null,
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
            viewModel.pickSpentAt(millisOf(2026, 10, 29, 9, 30))

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
    fun `GIVEN a new bill and a receipt dated but untimed WHEN read THEN Quando is that day at the current time`() =
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

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertEquals(millisOf(2026, 10, 25, 15, 0), state.spentAt)
        }

    @Test
    fun `GIVEN 20h picked and a receipt dated today without a time WHEN read THEN Quando is now`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                scan = scanOf(
                    reading(
                        date = LocalDate(2026, 10, 30),
                        time = null,
                    ),
                ),
            )
            viewModel.pickSpentAt(millisOf(2026, 10, 29, 20, 0))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertEquals(NOW_MILLIS, state.spentAt)
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
        viewModel.pickSpentAt(picked)

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
    fun `GIVEN a receipt with items read WHEN its items are tapped THEN Dividir opens on Itens with them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = scanOf(reading()))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptItemsClicked)

            // Then
            val state = viewModel.uiState.value
            assertNotNull(state.splitEditor)
            val items = assertNotNull(state.itemsEditor)
            assertEquals(listOf("Pizza", "Refrigerante"), items.items.map { it.name })
            assertTrue(items.split.isFromReceipt)
            assertEquals(SplitMode.EQUAL, state.split.mode)
        }

    @Test
    fun `GIVEN a receipt that lists no items WHEN its items are tapped THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(scan = scanOf(reading(items = emptyList())))
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptItemsClicked)

        // Then
        assertNull(viewModel.uiState.value.splitEditor)
        assertNull(viewModel.uiState.value.itemsEditor)
    }

    @Test
    fun `GIVEN a receipt read and the bill moved to another currency WHEN its items are tapped THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = false),
                storedGroups = listOf(dinner, trip),
                scan = scanOf(reading()),
            )
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(TRIP_ID))
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptItemsClicked)

            // Then
            assertEquals(GroupCurrency.USD, viewModel.uiState.value.currency)
            assertNull(viewModel.uiState.value.splitEditor)
            assertNull(viewModel.uiState.value.itemsEditor)
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
                scan = ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE),
            )
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertEquals(
                BillReceipt(
                    ReceiptStatus.FAILED,
                    itemCount = 0,
                    hasPhoto = false,
                    error = ReceiptError.UNREADABLE,
                ),
                state.receipt,
            )
            assertEquals(ReceiptError.UNREADABLE, state.receiptError)
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
                scan = ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE),
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            assertEquals(
                BillReceipt(
                    ReceiptStatus.FAILED,
                    itemCount = 0,
                    hasPhoto = false,
                    error = ReceiptError.UNREADABLE,
                ),
                viewModel.uiState.value.receipt,
            )
        }

    @Test
    fun `GIVEN a receipt that could not be read WHEN tapping Tirar outra foto THEN the camera opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                scan = ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE),
            )
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)

            // Then
            assertTrue(viewModel.uiState.value.showsReceiptSourceChoice)
        }

    @Test
    fun `GIVEN a receipt that could not be read WHEN another photo is read THEN it fills the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            answerRead(
                0,
                ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE),
            )
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(otherSource))

            // When
            answerRead(1, scanOf(reading()))

            // Then
            val state = viewModel.uiState.value
            assertEquals(ReceiptStatus.READ, state.receipt?.status)
            assertNull(state.receiptError)
            assertEquals("259,90", state.amountText)
        }

    @Test
    fun `GIVEN a photo nothing could be read from WHEN the reading comes back THEN offers Tirar outra foto only`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.offersReceiptRetake)
            assertFalse(state.offersReceiptRetry)
        }

    @Test
    fun `GIVEN the reader does not answer WHEN the read comes back THEN offers Tentar de novo and keeps the fields`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertEquals(
                BillReceipt(
                    ReceiptStatus.FAILED,
                    itemCount = 0,
                    hasPhoto = false,
                    error = ReceiptError.READER_UNAVAILABLE,
                ),
                state.receipt,
            )
            assertEquals(ReceiptError.READER_UNAVAILABLE, state.receiptError)
            assertTrue(state.offersReceiptRetry)
            assertFalse(state.offersReceiptRetake)
            assertFalse(state.isReadingReceipt)
            assertEquals("100,00", state.amountText)
            assertEquals("Jantar", state.description)
            assertEquals(NOW_MILLIS, state.spentAt)
            assertNull(state.receiptCategoryId)
        }

    @Test
    fun `GIVEN the photo never reached the server WHEN the reading comes back THEN offers Tentar de novo only`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.OFFLINE))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertEquals(
                BillReceipt(
                    ReceiptStatus.FAILED,
                    itemCount = 0,
                    hasPhoto = false,
                    error = ReceiptError.OFFLINE,
                ),
                state.receipt,
            )
            assertEquals(ReceiptError.OFFLINE, state.receiptError)
            assertTrue(state.offersReceiptRetry)
            assertFalse(state.offersReceiptRetake)
            assertEquals("", state.amountText)
            assertEquals("Jantar", state.description)
        }

    @Test
    fun `GIVEN the day limit of reads WHEN the reading comes back THEN offers neither another photo nor a retry`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.LIMIT_REACHED))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertEquals(
                BillReceipt(
                    ReceiptStatus.FAILED,
                    itemCount = 0,
                    hasPhoto = false,
                    error = ReceiptError.LIMIT_REACHED,
                ),
                state.receipt,
            )
            assertEquals(ReceiptError.LIMIT_REACHED, state.receiptError)
            assertFalse(state.offersReceiptRetry)
            assertFalse(state.offersReceiptRetake)
            assertEquals("", state.amountText)
            assertEquals("Jantar", state.description)
        }

    @Test
    fun `GIVEN a receipt being read WHEN a sync moves the bill to another currency THEN reads it again in that one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            groups.groups.value = listOf(dinner.copy(currency = GroupCurrency.USD))

            // When
            answerRead(0, scanOf(reading()))

            // Then
            val state = viewModel.uiState.value
            assertEquals(
                BillReceipt(
                    ReceiptStatus.READING,
                    itemCount = 0,
                    hasPhoto = true,
                    error = null,
                ),
                state.receipt,
            )
            assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD), readCurrencies)
            assertEquals(listOf(source.toList(), source.toList()), readSources)
            assertEquals("", state.amountText)
            assertEquals("", state.description)
        }

    @Test
    fun `GIVEN the reader did not answer WHEN saving the bill typed by hand THEN its photo is not kept`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(1, expenses.createdExpenses.size)
            assertTrue(attachments.isEmpty())
        }

    @Test
    fun `GIVEN the reader did not answer WHEN tapping Tentar de novo THEN the same photo is being read again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            answerRead(0, ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            val state = viewModel.uiState.value
            assertEquals(
                BillReceipt(
                    ReceiptStatus.READING,
                    itemCount = 0,
                    hasPhoto = true,
                    error = null,
                ),
                state.receipt,
            )
            assertTrue(state.isReadingReceipt)
            assertNull(state.receiptError)
            assertFalse(state.offersReceiptRetry)
            assertFalse(state.canSave)
            assertEquals(listOf(source.toList(), source.toList()), readSources)
            assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.BRL), readCurrencies)
        }

    @Test
    fun `GIVEN the photo never reached the server WHEN Tentar de novo reads it THEN it fills the bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            answerRead(0, ReceiptScan.Failed(ReceiptReadFailure.OFFLINE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // When
            answerRead(1, scanOf(reading()))

            // Then
            val state = viewModel.uiState.value
            assertEquals(
                BillReceipt(
                    ReceiptStatus.READ,
                    itemCount = 2,
                    hasPhoto = true,
                    error = null,
                ),
                state.receipt,
            )
            assertNull(state.receiptError)
            assertFalse(state.offersReceiptRetry)
            assertEquals("259,90", state.amountText)
            assertEquals("Bom Preço", state.description)
            assertEquals(listOf(source.toList(), source.toList()), readSources)
        }

    @Test
    fun `GIVEN a retried receipt read WHEN saving THEN attaches the photo of that read`() = runTest(testDispatcher) {
        // Given
        prepareScenario(holdsReads = true)
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        answerRead(0, ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))
        viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)
        answerRead(1, scanOf(reading()))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        val created = expenses.createdExpenses.single()
        assertEquals(listOf(encodedPhoto.jpeg.toList()), attachments.map { it.source })
        assertEquals(listOf(created.id), attachments.map { it.expenseId })
    }

    @Test
    fun `GIVEN a failed read and a sync to another currency WHEN tapping Tentar de novo THEN reads in that currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            groups.groups.value = listOf(dinner.copy(currency = GroupCurrency.USD))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.USD), readCurrencies)
            assertEquals(listOf(source.toList(), source.toList()), readSources)
        }

    @Test
    fun `GIVEN a retry the reader did not answer either WHEN tapping Tentar de novo again THEN reads it once more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            assertEquals(List(3) { source.toList() }, readSources)
            assertTrue(viewModel.uiState.value.offersReceiptRetry)
        }

    @Test
    fun `GIVEN a retry answered with the day limit WHEN the reading comes back THEN offers no retry`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            answerRead(0, ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // When
            answerRead(1, ReceiptScan.Failed(ReceiptReadFailure.LIMIT_REACHED))

            // Then
            val state = viewModel.uiState.value
            assertEquals(ReceiptError.LIMIT_REACHED, state.receiptError)
            assertFalse(state.offersReceiptRetry)
            assertFalse(state.offersReceiptRetake)
        }

    @Test
    fun `GIVEN a retry answered with the day limit WHEN tapping Tentar de novo THEN nothing is read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            answerRead(0, ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)
            answerRead(1, ReceiptScan.Failed(ReceiptReadFailure.LIMIT_REACHED))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            assertEquals(2, readSources.size)
            assertEquals(ReceiptError.LIMIT_REACHED, viewModel.uiState.value.receiptError)
        }

    @Test
    fun `GIVEN a photo nothing could be read from WHEN tapping Tentar de novo THEN nothing is read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            assertEquals(1, readSources.size)
            assertEquals(ReceiptError.UNREADABLE, viewModel.uiState.value.receiptError)
        }

    @Test
    fun `GIVEN the day limit of reads WHEN tapping Tentar de novo THEN nothing is read`() = runTest(testDispatcher) {
        // Given
        prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.LIMIT_REACHED))
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

        // Then
        assertEquals(1, readSources.size)
        assertEquals(ReceiptError.LIMIT_REACHED, viewModel.uiState.value.receiptError)
    }

    @Test
    fun `GIVEN no receipt WHEN tapping Tentar de novo THEN nothing is read`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

        // Then
        assertTrue(readSources.isEmpty())
        assertNull(viewModel.uiState.value.receipt)
    }

    @Test
    fun `GIVEN a failed read and the device offline WHEN tapping Tentar de novo THEN nothing is read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            isOnline.value = false

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            assertEquals(1, readSources.size)
            assertEquals(ReceiptError.READER_UNAVAILABLE, viewModel.uiState.value.receiptError)
            assertFalse(viewModel.uiState.value.canScanReceipt)
        }

    @Test
    fun `GIVEN a failed read and the bill saving WHEN tapping Tentar de novo THEN nothing is read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))
            expenses.createGate = CompletableDeferred()
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            assertEquals(1, readSources.size)
            assertEquals(ReceiptError.READER_UNAVAILABLE, viewModel.uiState.value.receiptError)
        }

    @Test
    fun `GIVEN a failed read and the receipt removed WHEN tapping Tentar de novo THEN nothing is read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRemoved)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            assertEquals(1, readSources.size)
            assertNull(viewModel.uiState.value.receipt)
            assertTrue(viewModel.uiState.value.showsScanReceipt)
        }

    @Test
    fun `GIVEN a failed read and a newer photo that read nothing WHEN tapping Tentar de novo THEN nothing is read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            answerRead(0, ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(otherSource))
            answerRead(1, ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            assertEquals(listOf(source.toList(), otherSource.toList()), readSources)
            assertEquals(ReceiptError.UNREADABLE, viewModel.uiState.value.receiptError)
        }

    @Test
    fun `GIVEN a newer photo the reader did not answer either WHEN tapping Tentar de novo THEN sends that photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            answerRead(0, ReceiptScan.Failed(ReceiptReadFailure.OFFLINE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(otherSource))
            answerRead(1, ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            assertEquals(listOf(source.toList(), otherSource.toList(), otherSource.toList()), readSources)
        }

    @Test
    fun `GIVEN a failed read and a newer photo being read WHEN tapping Tentar de novo THEN nothing more is read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            answerRead(0, ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(otherSource))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            assertEquals(listOf(source.toList(), otherSource.toList()), readSources)
            assertTrue(viewModel.uiState.value.isReadingReceipt)
        }

    @Test
    fun `GIVEN a retry that read the receipt WHEN tapping Tentar de novo THEN nothing is read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            answerRead(0, ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)
            answerRead(1, scanOf(reading()))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            assertEquals(2, readSources.size)
            assertEquals(
                ReceiptStatus.READ,
                viewModel.uiState.value.receipt
                    ?.status,
            )
            assertEquals("259,90", viewModel.uiState.value.amountText)
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
    fun `GIVEN a receipt being read WHEN typing in the fields THEN nothing changes and the reading fills them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            val spentAt = viewModel.uiState.value.spentAt
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado do mês"))
            viewModel.pickSpentAt(millisOf(2026, 10, 29, 9, 30))
            assertEquals("", viewModel.uiState.value.amountText)
            assertEquals("", viewModel.uiState.value.description)
            assertEquals(spentAt, viewModel.uiState.value.spentAt)

            // When
            answerRead(
                0,
                scanOf(
                    reading(
                        date = LocalDate(2026, 10, 28),
                        time = LocalTime(12, 34),
                    ),
                ),
            )

            // Then
            val state = viewModel.uiState.value
            assertEquals("259,90", state.amountText)
            assertEquals("Bom Preço", state.description)
            assertEquals(millisOf(2026, 10, 28, 12, 34), state.spentAt)
            assertEquals(ReceiptStatus.READ, state.receipt?.status)
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
                        source = encodedPhoto.jpeg.toList(),
                        addedByMemberId = PIERRE,
                    ),
                ),
                attachments,
            )
            assertEquals(25_990L, created.amountMinor)
            assertEquals("Bom Preço", created.description)
        }

    @Test
    fun `GIVEN a receipt that could not be read WHEN saving the bill typed by hand THEN its photo is not kept`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                scan = ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE),
            )
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(1, expenses.createdExpenses.size)
            assertTrue(attachments.isEmpty())
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
                    isOverBill = false,
                ),
                navigator.backStack.value.last(),
            )
        }

    @Test
    fun `GIVEN a group with categories of its own WHEN a receipt is read THEN sends only its live ones`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(groupCategories = listOf(pet, petFood, deletedGym, museum))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val sent = readHints.single().groupCategories
            assertEquals(
                listOf(
                    Triple(PET_ID, "Pet", null),
                    Triple(PET_FOOD_ID, "Ração", PET_ID),
                ),
                sent
                    .map { Triple(it.id, it.name, it.parentId) }
                    .sortedBy { it.first },
            )
            assertTrue(sent.all { it.isGroupCategory })
        }

    @Test
    fun `GIVEN map terms of several groups WHEN a receipt is read THEN sends only those that count here`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                groupCategories = listOf(pet, petFood, museum),
                termChanges = listOf(
                    termChange("ração", PET_FOOD_ID),
                    termChange("pizzaria", AppCategory.FOOD_RESTAURANT.key),
                    termChange("museu", MUSEUM_ID),
                ),
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val sent = readHints.single().terms
            assertTrue(CategoryTerm("ração", PET_FOOD_ID) in sent)
            assertTrue(CategoryTerm("pizzaria", AppCategory.FOOD_RESTAURANT.key) in sent)
            assertTrue(sent.none { it.categoryId == MUSEUM_ID })
            assertTrue(sent.none { it.term == "museu" })
        }

    @Test
    fun `GIVEN no group category nor map change WHEN a receipt is read THEN sends only the app terms`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val hints = readHints.single()
            assertTrue(hints.groupCategories.isEmpty())
            assertEquals(DefaultCategoryTerms.terms, hints.terms)
        }

    @Test
    fun `GIVEN a group category synced after the form opened WHEN a receipt is read THEN sends it too`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(groupCategories = listOf(pet))
            categories.groupCategories.update { it + petFood }

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            assertEquals(
                setOf(PET_ID, PET_FOOD_ID),
                readHints
                    .single()
                    .groupCategories
                    .map { it.id }
                    .toSet(),
            )
        }

    @Test
    fun `GIVEN a term added to the map after the form opened WHEN a receipt is read THEN sends it too`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(groupCategories = listOf(pet, petFood))
            terms.changes.value = listOf(termChange("ração", PET_FOOD_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            assertTrue(CategoryTerm("ração", PET_FOOD_ID) in readHints.single().terms)
        }

    @Test
    fun `GIVEN another group picked WHEN a receipt is read THEN sends that group categories and terms`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, trip),
                groupCategories = listOf(pet, petFood, museum),
                termChanges = listOf(
                    termChange("ração", PET_FOOD_ID),
                    termChange("museu", MUSEUM_ID),
                ),
            )
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(TRIP_ID))
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val hints = readHints.single()
            assertEquals(listOf(MUSEUM_ID), hints.groupCategories.map { it.id })
            assertTrue(CategoryTerm("museu", MUSEUM_ID) in hints.terms)
            assertTrue(hints.terms.none { it.categoryId == PET_FOOD_ID })
        }

    @Test
    fun `GIVEN no map word in the place WHEN the receipt reads a group category THEN the bill takes it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                groupCategories = listOf(pet, petFood),
                scan = scanOf(reading(categoryId = PET_FOOD_ID)),
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertEquals(PET_FOOD_ID, state.receiptCategory?.id)
            assertEquals(PET_FOOD_ID, state.category.id)
            assertEquals("Ração", state.category.name)
            assertEquals(PET_ID, state.categoryParent?.id)
            assertEquals(CategoryHint.BY_RECEIPT, state.categoryHint)
            val suggestion = state.categorySuggestions.single { it.category.id == PET_FOOD_ID }
            assertEquals(CategorySuggestionReason.RECEIPT, suggestion.reason)
            assertTrue(suggestion.isSelected)
        }

    @Test
    fun `GIVEN a map word in the place WHEN the receipt reads a group category THEN the map category wins`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                groupCategories = listOf(pet, petFood),
                scan = scanOf(
                    reading(
                        description = "Supermercado Bom Preço",
                        categoryId = PET_FOOD_ID,
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
                    PET_FOOD_ID to CategorySuggestionReason.RECEIPT,
                ),
                state.categorySuggestions.take(2).map { it.category.id to it.reason },
            )
        }

    @Test
    fun `GIVEN a category picked by hand WHEN the receipt reads a group category THEN the picked one stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                groupCategories = listOf(pet, petFood),
                scan = scanOf(reading(categoryId = PET_FOOD_ID)),
            )
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
    fun `GIVEN the receipt chose a group category WHEN removing it THEN the category falls back to Outros`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                groupCategories = listOf(pet, petFood),
                scan = scanOf(reading(categoryId = PET_FOOD_ID)),
            )
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
    fun `GIVEN the receipt chose a group subcategory WHEN a sync deletes it THEN the bill reads as its parent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                groupCategories = listOf(pet, petFood),
                scan = scanOf(reading(categoryId = PET_FOOD_ID)),
            )
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            categories.groupCategories.update { listOf(pet, petFood.copy(isDeleted = true)) }

            // Then
            val state = viewModel.uiState.value
            assertEquals(PET_ID, state.receiptCategory?.id)
            assertEquals(PET_ID, state.category.id)
            assertEquals(CategoryHint.BY_RECEIPT, state.categoryHint)
        }

    @Test
    fun `GIVEN a reading with another group category WHEN it comes back THEN the bill stays in Outros`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                groupCategories = listOf(pet, museum),
                scan = scanOf(reading(categoryId = MUSEUM_ID)),
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            val state = viewModel.uiState.value
            assertNull(state.receiptCategory)
            assertTrue(state.category.isOther)
            assertEquals(CategoryHint.NO_TERM, state.categoryHint)
        }

    @Test
    fun `GIVEN a new bill online WHEN tapping Ler cupom fiscal THEN sends the scan clicked from Rachar conta`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)

            // Then
            assertEquals(
                listOf("scan_receipt_clicked" to mapOf<String, Any>("source" to "split_bill")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a new bill offline WHEN tapping Ler cupom fiscal THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Editar conta WHEN tapping Ler cupom fiscal THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(route = editRoute(scansReceipt = false))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the camera open WHEN it closes without a photo THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPickerDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the choice open WHEN picking Tirar foto THEN sends the source picked as camera`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))

            // Then
            assertEquals(
                listOf("receipt_source_picked" to mapOf<String, Any>("source" to ReceiptPhotoSource.CAMERA)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the choice open WHEN picking Escolher da galeria THEN sends the source picked as photos`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.PHOTOS))

            // Then
            assertEquals(
                listOf("receipt_source_picked" to mapOf<String, Any>("source" to ReceiptPhotoSource.PHOTOS)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the choice open WHEN picking Escolher arquivo THEN sends the source picked as files`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.FILES))

            // Then
            assertEquals(
                listOf("receipt_source_picked" to mapOf<String, Any>("source" to ReceiptPhotoSource.FILES)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the choice asked for without a camera WHEN it is skipped THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptChoiceSkipped(ReceiptPhotoSource.FILES))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the choice never asked for WHEN a source is picked THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the camera already picked WHEN a late tap picks the gallery THEN sends nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.PHOTOS))

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Rachar em qual grupo open WHEN a source is picked THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            route = fromHome(scansReceipt = true),
            storedGroups = listOf(dinner, trip),
        )
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a receipt with two items WHEN its photo is read THEN sends the photo picked and the read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = scanOf(reading()))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            assertEquals(
                listOf(
                    "photo_picked" to mapOf<String, Any>("source" to "receipt"),
                    "receipt_read" to mapOf<String, Any>("result" to "read", "items" to 2),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a receipt that lists no items WHEN its photo is read THEN sends the read with no items`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = scanOf(reading(items = emptyList())))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            assertEquals(
                "receipt_read" to mapOf<String, Any>("result" to "read", "items" to 0),
                trackedEvents.last(),
            )
        }

    @Test
    fun `GIVEN a photo nothing could be read from WHEN it comes back THEN sends the read as unreadable`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            assertEquals(
                listOf(
                    "photo_picked" to mapOf<String, Any>("source" to "receipt"),
                    "receipt_read" to mapOf<String, Any>("result" to ReceiptError.UNREADABLE, "items" to 0),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the reader does not answer WHEN the read comes back THEN sends the read as reader unavailable`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            assertEquals(
                "receipt_read" to mapOf<String, Any>("result" to ReceiptError.READER_UNAVAILABLE, "items" to 0),
                trackedEvents.last(),
            )
        }

    @Test
    fun `GIVEN the day limit of reads WHEN the read comes back THEN sends the read as limit reached`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.LIMIT_REACHED))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            assertEquals(
                "receipt_read" to mapOf<String, Any>("result" to ReceiptError.LIMIT_REACHED, "items" to 0),
                trackedEvents.last(),
            )
        }

    @Test
    fun `GIVEN the photo never reached the server WHEN the read comes back THEN sends the read as offline`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.OFFLINE))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            assertEquals(
                "receipt_read" to mapOf<String, Any>("result" to ReceiptError.OFFLINE, "items" to 0),
                trackedEvents.last(),
            )
        }

    @Test
    fun `GIVEN Editar conta WHEN a receipt photo arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            route = editRoute(scansReceipt = false),
            scan = scanOf(reading()),
        )

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the bill saving WHEN a receipt photo arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(scan = scanOf(reading()))
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))
        expenses.createGate = CompletableDeferred()
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

        // Then
        assertTrue(readSources.isEmpty())
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a receipt being read WHEN a second photo is read THEN sends two photos and only the second read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(otherSource))

            // When
            answerRead(1, scanOf(reading()))

            // Then
            assertEquals(
                listOf(
                    "photo_picked" to mapOf<String, Any>("source" to "receipt"),
                    "photo_picked" to mapOf<String, Any>("source" to "receipt"),
                    "receipt_read" to mapOf<String, Any>("result" to "read", "items" to 2),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a receipt read again in another currency WHEN that read ends THEN sends the read once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            groups.groups.value = listOf(dinner.copy(currency = GroupCurrency.USD))
            answerRead(0, scanOf(reading()))

            // When
            answerRead(1, scanOf(reading()))

            // Then
            assertEquals(
                listOf(
                    "photo_picked" to mapOf<String, Any>("source" to "receipt"),
                    "receipt_read" to mapOf<String, Any>("result" to "read", "items" to 2),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the reader did not answer WHEN tapping Tentar de novo THEN sends the retry then how it ended`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            assertEquals(
                listOf(
                    "retry_clicked" to mapOf<String, Any>("source" to "receipt"),
                    "receipt_read" to mapOf<String, Any>("result" to ReceiptError.READER_UNAVAILABLE, "items" to 0),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a photo nothing could be read from WHEN tapping Tentar de novo THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a failed read and the device offline WHEN tapping Tentar de novo THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            isOnline.value = false
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a failed read and the bill saving WHEN tapping Tentar de novo THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))
            expenses.createGate = CompletableDeferred()
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a receipt being read WHEN removing it THEN sends only the receipt removed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(holdsReads = true)
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptRemoved)

        // Then
        assertEquals(listOf("receipt_removed" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN a receipt with items read WHEN its items are tapped THEN sends the receipt items clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = scanOf(reading()))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptItemsClicked)

            // Then
            assertEquals(listOf("receipt_items_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN no receipt read WHEN a tap on its items arrives THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(scan = scanOf(reading()))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptItemsClicked)

        // Then
        assertNull(viewModel.uiState.value.itemsEditor)
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a receipt read and the bill moved to another currency WHEN its items are tapped THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = false),
                storedGroups = listOf(dinner, trip),
                scan = scanOf(reading()),
            )
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(TRIP_ID))
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptItemsClicked)

            // Then
            assertEquals(GroupCurrency.USD, viewModel.uiState.value.currency)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the receipt camera open WHEN turning the flashlight on THEN sends the torch toggled on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptTorchClicked(isOn = true))

            // Then
            assertEquals(
                listOf("receipt_camera_torch_toggled" to mapOf<String, Any>("is_on" to true)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the receipt camera open WHEN turning the flashlight off THEN sends the torch toggled off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptTorchClicked(isOn = false))

            // Then
            assertEquals(
                listOf("receipt_camera_torch_toggled" to mapOf<String, Any>("is_on" to false)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the receipt camera open WHEN tapping Allow camera THEN sends the allow clicked from the receipt`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptCameraAllowClicked)

            // Then
            assertEquals(
                listOf("camera_allow_clicked" to mapOf<String, Any>("source" to "receipt")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the receipt camera open WHEN tapping Open settings THEN sends the settings clicked from the receipt`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnScanReceiptClicked)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptSourcePicked(ReceiptPhotoSource.CAMERA))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptCameraSettingsClicked)

            // Then
            assertEquals(
                listOf("camera_settings_clicked" to mapOf<String, Any>("source" to "receipt")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a receipt being read WHEN turning the flashlight on THEN still sends the torch toggled on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptTorchClicked(isOn = true))

            // Then
            assertEquals(
                listOf("receipt_camera_torch_toggled" to mapOf<String, Any>("is_on" to true)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a receipt being read WHEN tapping Allow camera THEN still sends the camera allow clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptCameraAllowClicked)

            // Then
            assertEquals(
                listOf("camera_allow_clicked" to mapOf<String, Any>("source" to "receipt")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a receipt being read WHEN tapping Open settings THEN still sends the camera settings clicked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsReads = true)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptCameraSettingsClicked)

            // Then
            assertEquals(
                listOf("camera_settings_clicked" to mapOf<String, Any>("source" to "receipt")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a receipt read WHEN saving THEN the bill saved says it came from a receipt`() = runTest(testDispatcher) {
        // Given
        prepareScenario(scan = scanOf(reading()))
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        assertEquals(
            listOf(
                "split_bill_save_clicked" to emptyMap(),
                "bill_saved" to mapOf<String, Any>(
                    "is_new" to true,
                    "split_mode" to SplitMode.EQUAL,
                    "payers" to 1,
                    "is_from_receipt" to true,
                    "is_category_picked" to false,
                    "is_split_as_default" to false,
                ),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a receipt that could not be read WHEN saving the bill typed by hand THEN it is not from a receipt`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(scan = ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Jantar"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                false,
                trackedEvents
                    .last()
                    .second["is_from_receipt"],
            )
        }

    @Test
    fun `GIVEN a photo from Inicio WHEN Rachar conta opens THEN takes it and reads it in the group currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = newRoute(scansReceipt = true),
                holdsReads = true,
                pickedReceipt = source,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(source.toList()), readSources)
            assertEquals(listOf(GroupCurrency.BRL), readCurrencies)
            assertNull(heldReceipt)
            assertEquals(
                BillReceipt(
                    ReceiptStatus.READING,
                    itemCount = 0,
                    hasPhoto = true,
                    error = null,
                ),
                state.receipt,
            )
            assertNull(state.receiptRequest)
        }

    @Test
    fun `GIVEN a photo from Inicio being read WHEN Rachar conta shows THEN the form locks and no choice opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = newRoute(scansReceipt = true),
                holdsReads = true,
                pickedReceipt = source,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isReadingReceipt)
            assertTrue(state.isFormLocked)
            assertFalse(state.canSave)
            assertFalse(state.showsReceiptSourceChoice)
            assertFalse(state.isReceiptSourceRequested)
            assertNull(state.shownReceiptSource)
        }

    @Test
    fun `GIVEN a photo from Inicio and a single group WHEN Rachar conta opens THEN reads it without asking`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                holdsReads = true,
                pickedReceipt = source,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.groupSelection)
            assertEquals(listOf(source.toList()), readSources)
            assertEquals(listOf(GroupCurrency.BRL), readCurrencies)
            assertEquals(ReceiptStatus.READING, state.receipt?.status)
            assertFalse(state.showsReceiptSourceChoice)
        }

    @Test
    fun `GIVEN a photo from Inicio and two groups WHEN Rachar em qual grupo is open THEN the photo waits unread`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, trip),
                holdsReads = true,
                pickedReceipt = source,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNotNull(state.groupSelection)
            assertTrue(readSources.isEmpty())
            assertNull(state.receipt)
            assertEquals(ReceiptRequest.Picked, state.receiptRequest)
            assertFalse(state.readsPickedReceipt)
            assertFalse(state.showsReceiptSourceChoice)
            assertFalse(state.isFormLocked)
        }

    @Test
    fun `GIVEN a photo from Inicio WHEN another group is confirmed THEN reads it once in that group currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, trip),
                holdsReads = true,
                pickedReceipt = source,
            )
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(TRIP_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            val state = viewModel.uiState.value
            assertEquals(TRIP_ID, state.groupId)
            assertEquals(listOf(GroupCurrency.USD), readCurrencies)
            assertEquals(listOf(source.toList()), readSources)
            assertEquals(ReceiptStatus.READING, state.receipt?.status)
            assertNull(state.receiptRequest)
            assertFalse(state.showsReceiptSourceChoice)
        }

    @Test
    fun `GIVEN a photo from Inicio WHEN tapping Continuar on the same group THEN reads it in its currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, trip),
                holdsReads = true,
                pickedReceipt = source,
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            assertEquals(GROUP_ID, viewModel.uiState.value.groupId)
            assertEquals(listOf(GroupCurrency.BRL), readCurrencies)
            assertEquals(listOf(source.toList()), readSources)
        }

    @Test
    fun `GIVEN a photo from Inicio WHEN Rachar em qual grupo is dismissed THEN reads it in the bill currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, trip),
                holdsReads = true,
                pickedReceipt = source,
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDismissed)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.groupSelection)
            assertEquals(GROUP_ID, state.groupId)
            assertEquals(listOf(GroupCurrency.BRL), readCurrencies)
            assertEquals(ReceiptStatus.READING, state.receipt?.status)
            assertFalse(state.showsReceiptSourceChoice)
        }

    @Test
    fun `GIVEN a photo from Inicio read WHEN the bill moves to another group THEN it is not read again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, trip),
                scan = scanOf(reading()),
                pickedReceipt = source,
            )
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(TRIP_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            val state = viewModel.uiState.value
            assertEquals(TRIP_ID, state.groupId)
            assertEquals(listOf(source.toList()), readSources)
            assertEquals(listOf(GroupCurrency.BRL), readCurrencies)
            assertFalse(state.showsReceiptSourceChoice)
        }

    @Test
    fun `GIVEN a photo from Inicio for a bitcoin group WHEN Rachar conta opens THEN the bill opens alone`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = newRoute(scansReceipt = true),
                storedGroups = listOf(dinner.copy(currency = GroupCurrency.BTC)),
                pickedReceipt = source,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertTrue(readSources.isEmpty())
            assertNull(state.receipt)
            assertNull(state.receiptRequest)
            assertFalse(state.showsReceiptSourceChoice)
        }

    @Test
    fun `GIVEN a photo from Inicio WHEN a bitcoin group is confirmed THEN nothing is read and no choice opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, wallet),
                pickedReceipt = source,
            )
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(WALLET_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            val state = viewModel.uiState.value
            assertEquals(WALLET_ID, state.groupId)
            assertTrue(readSources.isEmpty())
            assertNull(state.receipt)
            assertNull(state.receiptRequest)
            assertFalse(state.showsReceiptSourceChoice)
        }

    @Test
    fun `GIVEN a photo from Inicio WHEN a bitcoin group drops it THEN Inicio no longer holds it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, wallet),
                pickedReceipt = source,
            )
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(WALLET_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            assertNull(heldReceipt)
            assertTrue(readSources.isEmpty())
        }

    @Test
    fun `GIVEN a photo dropped for a bitcoin group WHEN the bill moves to a real group THEN it is still not read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, wallet),
                pickedReceipt = source,
            )
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(WALLET_ID))
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(GROUP_ID))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            val state = viewModel.uiState.value
            assertEquals(GROUP_ID, state.groupId)
            assertTrue(readSources.isEmpty())
            assertNull(state.receipt)
            assertNull(state.receiptRequest)
            assertFalse(state.showsReceiptSourceChoice)
        }

    @Test
    fun `GIVEN no photo waiting from Inicio WHEN tapping Continuar THEN asks for the source and reads nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, trip),
                pickedReceipt = null,
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // Then
            val state = viewModel.uiState.value
            assertEquals(ReceiptRequest.Choice, state.receiptRequest)
            assertTrue(state.showsReceiptSourceChoice)
            assertFalse(state.readsPickedReceipt)
            assertTrue(readSources.isEmpty())
            assertNull(state.receipt)
        }

    @Test
    fun `GIVEN a photo held and Editar conta asking to scan WHEN it opens THEN it is neither taken nor read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = editRoute(scansReceipt = true),
                pickedReceipt = source,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertNotNull(heldReceipt)
            assertTrue(readSources.isEmpty())
            assertNull(state.receipt)
            assertNull(state.receiptRequest)
            assertEquals("100,00", state.amountText)
        }

    @Test
    fun `GIVEN a photo held and a bill not asking to scan WHEN it opens THEN it is neither taken nor read`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = newRoute(scansReceipt = false),
                pickedReceipt = source,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNotNull(heldReceipt)
            assertTrue(readSources.isEmpty())
            assertNull(state.receipt)
            assertNull(state.receiptRequest)
        }

    @Test
    fun `GIVEN a photo from Inicio offline WHEN Rachar conta opens THEN sends it and says it never got there`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = newRoute(scansReceipt = true),
                online = false,
                scan = ReceiptScan.Failed(ReceiptReadFailure.OFFLINE),
                pickedReceipt = source,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(source.toList()), readSources)
            assertEquals(ReceiptStatus.FAILED, state.receipt?.status)
            assertEquals(ReceiptError.OFFLINE, state.receiptError)
        }

    @Test
    fun `GIVEN a photo from Inicio the reader did not answer WHEN tapping Tentar de novo THEN reads it again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = newRoute(scansReceipt = true),
                scan = ReceiptScan.Failed(ReceiptReadFailure.READER_UNAVAILABLE),
                pickedReceipt = source,
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRetryClicked)

            // Then
            assertEquals(listOf(source.toList(), source.toList()), readSources)
            assertEquals(listOf(GroupCurrency.BRL, GroupCurrency.BRL), readCurrencies)
        }

    @Test
    fun `GIVEN a photo from Inicio being read WHEN removing it THEN the scan button is back and nothing rereads`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = newRoute(scansReceipt = true),
                holdsReads = true,
                pickedReceipt = source,
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRemoved)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.receipt)
            assertTrue(state.showsScanReceipt)
            assertFalse(state.showsReceiptSourceChoice)
            assertEquals(listOf(source.toList()), readSources)
        }

    @Test
    fun `GIVEN a photo from Inicio read in another group WHEN saving THEN attaches it as the bill receipt`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, trip),
                scan = scanOf(reading()),
                pickedReceipt = source,
            )
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(TRIP_ID))
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val created = expenses.createdExpenses.single()
            assertEquals(TRIP_ID, created.groupId)
            assertEquals(25_990L, created.amountMinor)
            assertEquals("Bom Preço", created.description)
            assertEquals(
                listOf(
                    RecordedAttachment(
                        expenseId = created.id,
                        groupId = TRIP_ID,
                        kind = BillAttachmentKind.RECEIPT,
                        source = encodedPhoto.jpeg.toList(),
                        addedByMemberId = PIERRE_TRIP,
                    ),
                ),
                attachments,
            )
        }

    @Test
    fun `GIVEN a photo from Inicio WHEN it is read THEN sends only the read and no photo picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = newRoute(scansReceipt = true),
                scan = scanOf(reading()),
                pickedReceipt = source,
            )

            // When
            val sent = trackedEvents.toList()

            // Then
            assertEquals(
                listOf("receipt_read" to mapOf<String, Any>("result" to "read", "items" to 2)),
                sent,
            )
        }

    @Test
    fun `GIVEN a photo from Inicio and two groups WHEN its read ends THEN sends the read once and no photo picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                route = fromHome(scansReceipt = true),
                storedGroups = listOf(dinner, trip),
                holdsReads = true,
                pickedReceipt = source,
            )
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(TRIP_ID))
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // When
            answerRead(0, scanOf(reading()))

            // Then
            assertTrue(trackedEvents.none { it.first == "photo_picked" })
            assertEquals(
                listOf(mapOf<String, Any>("result" to "read", "items" to 2)),
                trackedEvents.filter { it.first == "receipt_read" }.map { it.second },
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

    private fun scanOf(reading: ReceiptReading): ReceiptScan = ReceiptScan.Read(
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

    private val encodedPhoto = BillImage(
        jpeg = byteArrayOf(9, 8, 7),
        width = 1200,
        height = 1600,
    )

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

    private val wallet = group(
        id = WALLET_ID,
        name = "Carteira",
        currency = GroupCurrency.BTC,
        members = listOf(
            member(PIERRE_WALLET, "Pierre", isMe = true),
            member(DANI, "Dani", isMe = false),
        ),
    )

    private val storedBill = Expense(
        id = EXPENSE_ID,
        groupId = GROUP_ID,
        description = "Jantar",
        currency = GroupCurrency.BRL,
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

    private val pet = groupCategory(
        id = PET_ID,
        groupId = GROUP_ID,
        parentId = null,
        name = "Pet",
        icon = CategoryIcon.PAW,
    )

    private val petFood = groupCategory(
        id = PET_FOOD_ID,
        groupId = GROUP_ID,
        parentId = PET_ID,
        name = "Ração",
        icon = null,
    )

    private val deletedGym = groupCategory(
        id = GYM_ID,
        groupId = GROUP_ID,
        parentId = null,
        name = "Academia",
        icon = CategoryIcon.GYM,
        isDeleted = true,
    )

    private val museum = groupCategory(
        id = MUSEUM_ID,
        groupId = TRIP_ID,
        parentId = AppCategory.LEISURE.key,
        name = "Museu",
        icon = null,
    )

    private fun groupCategory(
        id: String,
        groupId: String,
        parentId: String?,
        name: String,
        icon: CategoryIcon?,
        isDeleted: Boolean = false,
    ): GroupCategory = GroupCategory(
        id = id,
        groupId = groupId,
        parentId = parentId,
        name = name,
        icon = icon,
        color = null,
        createdByMemberId = null,
        isDeleted = isDeleted,
        isPending = false,
    )

    private fun termChange(
        term: String,
        categoryId: String,
    ): CategoryTermChange = CategoryTermChange(
        key = CategoryMatcher.keyOf(term),
        term = term,
        categoryId = categoryId,
        isRestored = false,
        isPending = false,
    )

    private fun newRoute(scansReceipt: Boolean = false): Route.SplitBill = Route.SplitBill(
        GROUP_ID,
        expenseId = null,
        picksGroupFirst = false,
        scansReceipt = scansReceipt,
        duplicatesExpenseId = null,
    )

    private fun editRoute(scansReceipt: Boolean): Route.SplitBill = Route.SplitBill(
        GROUP_ID,
        EXPENSE_ID,
        picksGroupFirst = false,
        scansReceipt = scansReceipt,
        duplicatesExpenseId = null,
    )

    private fun fromHome(scansReceipt: Boolean): Route.SplitBill = Route.SplitBill(
        GROUP_ID,
        expenseId = null,
        picksGroupFirst = true,
        scansReceipt = scansReceipt,
        duplicatesExpenseId = null,
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
        shape = GroupShape.SQUARE,
        photo = null,
        currency = currency,
        members = members,
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
        hasJoined = isMe,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    // Why: the photo Início held for Rachar conta, which the ViewModel takes once as it opens.
    private var heldReceipt: ByteArray? = null

    private fun prepareScenario(
        route: Route.SplitBill = newRoute(),
        storedGroups: List<Group> = listOf(dinner),
        online: Boolean = true,
        scan: ReceiptScan = ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE),
        holdsReads: Boolean = false,
        attachResult: Result<BillAttachmentAddition> = Result.success(BillAttachmentAddition.ADDED),
        groupCategories: List<GroupCategory> = emptyList(),
        termChanges: List<CategoryTermChange> = emptyList(),
        pickedReceipt: ByteArray? = null,
    ) {
        heldReceipt = pickedReceipt
        isOnline = MutableStateFlow(online)
        groups = FakeGroupRepository(initialGroups = storedGroups)
        categories = FakeCategoryRepository(initialGroupCategories = groupCategories)
        terms = FakeCategoryTermRepository(initialChanges = termChanges)
        expenses = FakeExpenseRepository(initialExpenses = listOf(storedBill))
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            if (!route.picksGroupFirst) {
                it.navigate(
                    Route.GroupDetail(
                        GROUP_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = false,
                    ),
                )
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
                observeCategoryTree = ObserveCategoryTreeUseCase(categories),
                observeCategoryMap = ObserveCategoryMapUseCase(terms),
                saveGroupCategory = { _, _, _, _, _, _, _ -> Result.success("") },
                deleteGroupCategory = { Result.success(Unit) },
            ),
            saveSplitAsDefault = { _, _, _, _ -> Result.success(Unit) },
            offerCategoryTeach = {},
            takePickedReceipt = { heldReceipt.also { heldReceipt = null } },
            readReceipt = { photo, currency, hints ->
                readSources += photo.toList()
                readCurrencies += currency
                readHints += hints
                if (holdsReads) {
                    CompletableDeferred<ReceiptScan>().also { pendingReads += it }.await()
                } else {
                    scan
                }
            },
            attachBillImage = { expenseId, groupId, kind, image, addedByMemberId ->
                attachments += RecordedAttachment(expenseId, groupId, kind, image.jpeg.toList(), addedByMemberId)
                attachResult
            },
            observeBitcoinInSats = { flowOf(false) },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
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
        const val WALLET_ID = "group-3"
        const val PIERRE_WALLET = "member-6"
        const val DANI = "member-7"
        const val PET_ID = "pet"
        const val PET_FOOD_ID = "pet-food"
        const val GYM_ID = "gym"
        const val MUSEUM_ID = "museum"

        // Why: 2026-10-30T15:00:00Z.
        const val NOW_MILLIS = 1_793_372_400_000L
    }
}
