package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptItem
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReadFailure
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReading
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptScan
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
import com.quare.blitzsplit.core.provider.image.BillImage
import com.quare.blitzsplit.feature.splitbill.presentation.model.ReceiptStatus
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.BillCategoryUseCases
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

// Why: Rachar conta locked while "Ler cupom fiscal" reads a receipt (beta feedback 17), so the read overwrites nothing.
class ReceiptLockViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: SplitBillViewModel
    private val pendingReads = mutableListOf<CompletableDeferred<ReceiptScan>>()
    private var clockMillis = NOW_MILLIS

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val groups = FakeGroupRepository(initialGroups = listOf(dinner, trip))
        val expenses = FakeExpenseRepository(initialExpenses = emptyList())
        val categories = FakeCategoryRepository(initialGroupCategories = emptyList())
        val terms = FakeCategoryTermRepository(initialChanges = emptyList())
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            )
            it.navigate(newRoute)
        }
        val deletedBills = InMemoryDeletedBillRepository()
        viewModel = SplitBillViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            route = newRoute,
            navigator = navigator,
            createExpense = CreateExpenseUseCase(expenses) { NOW_MILLIS },
            updateExpense = UpdateExpenseUseCase(expenses) { NOW_MILLIS },
            deleteExpense = DeleteExpenseUseCase(expenses, deletedBills) { NOW_MILLIS },
            noticeBillGone = NoticeBillGoneUseCase(deletedBills),
            currentTimeMillis = { clockMillis },
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
            takePickedReceipt = { null },
            readReceipt = { _, _, _ -> CompletableDeferred<ReceiptScan>().also { pendingReads += it }.await() },
            attachBillImage = { _, _, _, _, _ -> Result.failure(IllegalStateException("not attached here")) },
            observeBitcoinInSats = { flowOf(false) },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a new bill without a receipt WHEN it opens THEN the form is not locked`() = runTest(testDispatcher) {
        // When
        val state = viewModel.uiState.value

        // Then
        assertNull(state.receipt)
        assertFalse(state.isFormLocked)
    }

    @Test
    fun `GIVEN a new bill WHEN a receipt photo is being read THEN the form is locked`() = runTest(testDispatcher) {
        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

        // Then
        assertEquals(
            ReceiptStatus.READING,
            viewModel.uiState.value.receipt
                ?.status,
        )
        assertTrue(viewModel.uiState.value.isFormLocked)
    }

    @Test
    fun `GIVEN a receipt being read WHEN it is read THEN the form unlocks`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

        // When
        answerRead(scanOf(reading()))

        // Then
        assertEquals(
            ReceiptStatus.READ,
            viewModel.uiState.value.receipt
                ?.status,
        )
        assertFalse(viewModel.uiState.value.isFormLocked)
    }

    @Test
    fun `GIVEN a receipt being read WHEN the read fails THEN the form unlocks`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

        // When
        answerRead(ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE))

        // Then
        assertEquals(
            ReceiptStatus.FAILED,
            viewModel.uiState.value.receipt
                ?.status,
        )
        assertFalse(viewModel.uiState.value.isFormLocked)
    }

    @Test
    fun `GIVEN a receipt being read WHEN removing it THEN the form unlocks`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptRemoved)

        // Then
        assertNull(viewModel.uiState.value.receipt)
        assertFalse(viewModel.uiState.value.isFormLocked)
    }

    @Test
    fun `GIVEN Dividir open WHEN a receipt photo is being read THEN Dividir closes`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        assertNotNull(viewModel.uiState.value.splitEditor)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

        // Then
        assertNull(viewModel.uiState.value.splitEditor)
    }

    @Test
    fun `GIVEN the category picker open WHEN a receipt photo is being read THEN the picker closes`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
            assertNotNull(viewModel.uiState.value.categoryPicker)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // Then
            assertNull(viewModel.uiState.value.categoryPicker)
        }

    @Test
    fun `GIVEN Quem pagou open WHEN a receipt photo is being read THEN the payer sheet closes and picks nothing`() =
        runTest(testDispatcher) {
            // Given
            val payerId = viewModel.uiState.value.payerId
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(ANA))
            viewModel.onEvent(SplitBillUiEvent.OnPayerDone)

            // Then
            assertNull(viewModel.uiState.value.payerSelection)
            assertEquals(payerId, viewModel.uiState.value.payerId)
        }

    @Test
    fun `GIVEN a receipt being read WHEN tapping Quem pagou THEN the payer sheet stays closed`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)

            // Then
            assertNull(viewModel.uiState.value.payerSelection)
        }

    @Test
    fun `GIVEN a receipt being read WHEN the Mac pop-up picks a payer THEN the payer stays`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            val payerId = viewModel.uiState.value.payerId

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(ANA))
            viewModel.onEvent(SplitBillUiEvent.OnPayerDone)

            // Then
            assertEquals(payerId, viewModel.uiState.value.payerId)
            assertNull(viewModel.uiState.value.payerSelection)
        }

    @Test
    fun `GIVEN a receipt being read WHEN tapping Varias pessoas THEN the several payers sheet stays closed`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSeveralPayersClicked)

            // Then
            assertNull(viewModel.uiState.value.payerAmounts)
        }

    @Test
    fun `GIVEN a receipt being read WHEN tapping Divisao THEN Dividir stays closed`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

        // Then
        assertNull(viewModel.uiState.value.splitEditor)
        assertNull(viewModel.uiState.value.itemsEditor)
    }

    @Test
    fun `GIVEN two groups and a receipt being read WHEN tapping the group THEN Rachar em qual grupo stays closed`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)

            // Then
            assertNull(viewModel.uiState.value.groupSelection)
        }

    @Test
    fun `GIVEN a receipt being read WHEN tapping the category THEN the picker stays closed`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)

            // Then
            assertNull(viewModel.uiState.value.categoryPicker)
        }

    @Test
    fun `GIVEN a receipt being read WHEN the Mac menu picks a category THEN the bill category stays`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            val category = viewModel.uiState.value.category

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCategoryPicked(AppCategory.LEISURE_SPORTS.key))

            // Then
            assertEquals(category, viewModel.uiState.value.category)
            assertFalse(viewModel.uiState.value.isCategoryPicked)
        }

    @Test
    fun `GIVEN a receipt being read WHEN the Mac menu asks for a new category THEN its form stays closed`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            viewModel.onEvent(
                SplitBillUiEvent.OnNewCategoryClicked(
                    parentId = null,
                    name = "",
                ),
            )

            // Then
            assertNull(viewModel.uiState.value.categoryForm)
        }

    @Test
    fun `GIVEN a receipt being read WHEN tapping Mapa de categorias THEN the map does not open`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCategoryMapClicked)

            // Then
            assertEquals(newRoute, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN a receipt being read past a minute WHEN opening Quando THEN its limit is not moved`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            clockMillis = LATER_MILLIS

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSpentAtOpened)

            // Then
            assertEquals(NOW_MILLIS, viewModel.uiState.value.latestSpentAt)
        }

    @Test
    fun `GIVEN a receipt being read WHEN closing the form THEN it leaves to the group`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCloseClicked)

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a receipt read WHEN typing in the fields THEN they change`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        answerRead(scanOf(reading()))
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged(""))
        val spentAt = millisOf(2026, 10, 29, 9, 30)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado do mês"))
        viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(spentAt))

        // Then
        val state = viewModel.uiState.value
        assertEquals("100,00", state.amountText)
        assertEquals("Mercado do mês", state.description)
        assertEquals(spentAt, state.spentAt)
    }

    @Test
    fun `GIVEN a receipt that could not be read WHEN typing in the fields THEN they change`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            answerRead(ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE))
            val spentAt = millisOf(2026, 10, 29, 9, 30)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado do mês"))
            viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(spentAt))

            // Then
            val state = viewModel.uiState.value
            assertEquals("100,00", state.amountText)
            assertEquals("Mercado do mês", state.description)
            assertEquals(spentAt, state.spentAt)
        }

    @Test
    fun `GIVEN a receipt removed while being read WHEN typing the amount THEN it changes`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        viewModel.onEvent(SplitBillUiEvent.OnReceiptRemoved)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))

        // Then
        assertEquals("100,00", viewModel.uiState.value.amountText)
    }

    @Test
    fun `GIVEN a receipt read WHEN tapping Quem pagou THEN the payer sheet opens`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        answerRead(scanOf(reading()))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)

        // Then
        assertNotNull(viewModel.uiState.value.payerSelection)
    }

    @Test
    fun `GIVEN two groups and a receipt read WHEN tapping the group THEN Rachar em qual grupo opens`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            answerRead(scanOf(reading()))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)

            // Then
            assertEquals(
                GROUP_ID,
                viewModel.uiState.value.groupSelection
                    ?.groupId,
            )
        }

    @Test
    fun `GIVEN a receipt that could not be read past a minute WHEN opening Quando THEN its limit is now`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            answerRead(ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE))
            clockMillis = LATER_MILLIS

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSpentAtOpened)

            // Then
            assertEquals(LATER_MILLIS, viewModel.uiState.value.latestSpentAt)
        }

    @Test
    fun `GIVEN the fields typed before the photo WHEN the receipt is read THEN it fills them with what it read`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("100,00"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado do mês"))
            viewModel.onEvent(SplitBillUiEvent.OnSpentAtChanged(millisOf(2026, 10, 29, 9, 30)))
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))

            // When
            answerRead(
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
        }

    @Test
    fun `GIVEN a receipt being read WHEN tapping Quem pagou THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a receipt being read WHEN the Mac pop-up picks a payer THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(ANA))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a receipt being read WHEN tapping Divisao THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a receipt being read WHEN opening Quando THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSpentAtOpened)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN two groups and a receipt being read WHEN tapping the group THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)

            // Then
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a receipt being read WHEN tapping the category THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a receipt being read WHEN the Mac menu picks a category THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryPicked(AppCategory.FOOD_MARKET.key))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a receipt being read WHEN tapping Mapa de categorias THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryMapClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a receipt being read WHEN removing the photo THEN sends the receipt removed`() =
        runTest(testDispatcher) {
            // Given
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnReceiptRemoved)

            // Then
            assertEquals(listOf("receipt_removed" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN a receipt being read WHEN tapping Salvar THEN sends only the tap`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        assertEquals(listOf("split_bill_save_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN a receipt read WHEN tapping Quem pagou THEN sends the payer clicked`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(source))
        answerRead(scanOf(reading()))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)

        // Then
        assertEquals(listOf("split_bill_payer_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    private fun TestScope.answerRead(scan: ReceiptScan) {
        pendingReads.last().complete(scan)
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
        date: LocalDate? = null,
        time: LocalTime? = null,
    ): ReceiptReading = ReceiptReading(
        totalMinor = 25_990L,
        description = "Bom Preço",
        categoryId = null,
        date = date,
        time = time,
        items = listOf(
            ReceiptItem(
                name = "Pizza",
                quantity = 1L,
                unitPriceMinor = 25_990L,
            ),
        ),
        servicePercent = 0L,
    )

    private fun group(
        id: String,
        name: String,
        members: List<GroupMember>,
    ): Group = Group(
        id = id,
        name = name,
        type = GroupType.EVENT,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
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

    private val source = byteArrayOf(1, 2, 3)

    private val encodedPhoto = BillImage(
        jpeg = byteArrayOf(9, 8, 7),
        width = 1200,
        height = 1600,
    )

    private val newRoute = Route.SplitBill(
        GROUP_ID,
        expenseId = null,
        picksGroupFirst = false,
        scansReceipt = false,
    )

    private val dinner = group(
        id = GROUP_ID,
        name = "Jantar",
        members = listOf(
            member(PIERRE, "Pierre", isMe = true),
            member(ANA, "Ana", isMe = false),
        ),
    )

    private val trip = group(
        id = TRIP_ID,
        name = "Viagem Floripa",
        members = listOf(
            member(PIERRE_TRIP, "Pierre", isMe = true),
            member(CARLA, "Carla", isMe = false),
        ),
    )

    private companion object {
        const val GROUP_ID = "group-1"
        const val TRIP_ID = "group-2"
        const val PIERRE = "member-1"
        const val ANA = "member-2"
        const val PIERRE_TRIP = "member-3"
        const val CARLA = "member-4"

        // Why: 2026-10-30T15:00:00Z.
        const val NOW_MILLIS = 1_793_372_400_000L

        // Why: two minutes later, when "Quando" is opened again.
        const val LATER_MILLIS = NOW_MILLIS + 120_000L
    }
}
