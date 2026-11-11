package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
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
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.BillCategoryUseCases
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: the bill's group on Rachar conta and Editar conta opens its screen over the form, which back returns to (beta).
class OpenGroupViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val pendingReads = mutableListOf<CompletableDeferred<ReceiptScan>>()
    private lateinit var expenses: FakeExpenseRepository
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
    fun `GIVEN a new bill from its group WHEN the form opens THEN its group can be opened`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoaded)
        assertTrue(state.canOpenGroup)
    }

    @Test
    fun `GIVEN a new bill from its group WHEN tapping the group THEN opens its Despesas over the form`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    tripExpenses,
                    fromGroup,
                    tripOverBill,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a new bill WHEN tapping the group THEN sends the group opened`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)

        // Then
        assertEquals(listOf("split_bill_group_opened" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN several groups WHEN tapping the group THEN does not ask which group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)

        // Then
        assertNull(viewModel.uiState.value.groupSelection)
        assertTrue(viewModel.uiState.value.canChangeGroup)
    }

    @Test
    fun `GIVEN a bill filled in and its group opened WHEN going back THEN the form still holds what was typed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            fillIn()
            viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)

            // When
            navigator.navigateBack()

            // Then
            val state = viewModel.uiState.value
            assertEquals(fromGroup, navigator.backStack.value.last())
            assertEquals("90,00", state.amountText)
            assertEquals("Mercado", state.description)
            assertEquals(TRIP_ID, state.groupId)
            assertTrue(state.canSave)
            assertTrue(state.canOpenGroup)
        }

    @Test
    fun `GIVEN the bill moved to another group WHEN tapping the group THEN opens the picked group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
            viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(HOME_ID))
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    tripExpenses,
                    fromGroup,
                    Route.GroupDetail(
                        HOME_ID,
                        joinedAs = null,
                        tab = GroupTab.EXPENSES,
                        isOverBill = true,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN Rachar conta from the home with its group kept WHEN tapping the group THEN opens it over the form`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = fromHome,
                underneath = listOf(Route.Groups),
            )
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    fromHome,
                    tripOverBill,
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN a group alone on the device WHEN tapping the group THEN opens it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroups = listOf(trip))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)

        // Then
        assertFalse(viewModel.uiState.value.canChangeGroup)
        assertEquals(tripOverBill, navigator.backStack.value.last())
        assertEquals(fromGroup, navigator.backStack.value[navigator.backStack.value.lastIndex - 1])
    }

    @Test
    fun `GIVEN Editar conta WHEN tapping the group THEN opens its Despesas over the form and sends the group opened`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = listOf(Route.Groups, tripExpenses, billDetail),
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)

            // Then
            assertTrue(viewModel.uiState.value.isEditing)
            assertEquals(
                listOf(
                    Route.Groups,
                    tripExpenses,
                    billDetail,
                    editRoute,
                    tripOverBill,
                ),
                navigator.backStack.value,
            )
            assertEquals(listOf("split_bill_group_opened" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the form not loaded yet WHEN tapping the group THEN nothing opens and nothing is sent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isSatsUnitRead = false)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)

            // Then
            assertFalse(viewModel.uiState.value.isLoaded)
            assertFalse(viewModel.uiState.value.canOpenGroup)
            assertEquals(listOf(Route.Groups, tripExpenses, fromGroup), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the bill saving WHEN tapping the group THEN nothing opens and nothing is sent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            fillIn()
            expenses.createGate = CompletableDeferred()
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)

            // Then
            assertTrue(viewModel.uiState.value.isSaving)
            assertFalse(viewModel.uiState.value.canOpenGroup)
            assertEquals(listOf(Route.Groups, tripExpenses, fromGroup), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a receipt being read WHEN tapping the group THEN nothing opens and nothing is sent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(receiptPhoto))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)

            // Then
            assertTrue(viewModel.uiState.value.isFormLocked)
            assertFalse(viewModel.uiState.value.canOpenGroup)
            assertEquals(listOf(Route.Groups, tripExpenses, fromGroup), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN a receipt that could not be read WHEN tapping the group THEN opens it over the form`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(receiptPhoto))
            pendingReads.single().complete(ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)

            // Then
            assertFalse(viewModel.uiState.value.isFormLocked)
            assertEquals(
                listOf(
                    Route.Groups,
                    tripExpenses,
                    fromGroup,
                    tripOverBill,
                ),
                navigator.backStack.value,
            )
            assertEquals(listOf("split_bill_group_opened" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the form closed by a native back WHEN a late tap on the group arrives THEN nothing opens or is sent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)

            // Then
            assertEquals(listOf(Route.Groups, tripExpenses), navigator.backStack.value)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN the group already opened over the form WHEN tapping the group again THEN opens no second one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    tripExpenses,
                    fromGroup,
                    tripOverBill,
                ),
                navigator.backStack.value,
            )
            assertTrue(trackedEvents.isEmpty())
        }

    private fun fillIn() {
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("90"))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado"))
    }

    private val receiptPhoto = byteArrayOf(1, 2, 3)

    private val tripExpenses = Route.GroupDetail(
        TRIP_ID,
        joinedAs = null,
        tab = GroupTab.EXPENSES,
        isOverBill = false,
    )

    private val tripOverBill = tripExpenses.copy(isOverBill = true)

    private val fromGroup = Route.SplitBill(
        TRIP_ID,
        expenseId = null,
        picksGroupFirst = false,
        scansReceipt = false,
        duplicatesExpenseId = null,
    )

    private val fromHome = Route.SplitBill(
        TRIP_ID,
        expenseId = null,
        picksGroupFirst = true,
        scansReceipt = false,
        duplicatesExpenseId = null,
    )

    private val editRoute = Route.SplitBill(
        TRIP_ID,
        EXPENSE_ID,
        picksGroupFirst = false,
        scansReceipt = false,
        duplicatesExpenseId = null,
    )

    private val billDetail = Route.BillDetail(TRIP_ID, EXPENSE_ID, focusCommentId = null)

    // Why: "Jantar", R$ 100,00 Pierre paid, split equally between Ana and him.
    private val savedBill = Expense(
        id = EXPENSE_ID,
        groupId = TRIP_ID,
        description = "Jantar",
        currency = GroupCurrency.BRL,
        amountMinor = 10_000L,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(PIERRE_TRIP, 10_000L)),
        shares = listOf(
            ExpenseShare(ANA, 5_000L, weight = 1L),
            ExpenseShare(PIERRE_TRIP, 5_000L, weight = 1L),
        ),
        items = null,
        categoryId = null,
        spentAt = YESTERDAY,
        isPending = false,
        updatedAt = YESTERDAY,
        updatedByMemberId = PIERRE_TRIP,
    )

    private val trip = group(
        id = TRIP_ID,
        name = "Viagem Floripa",
        members = listOf(
            member(ANA, "Ana"),
            member(PIERRE_TRIP, "Pierre", isMe = true),
        ),
    )

    private val home = group(
        id = HOME_ID,
        name = "Apê 302",
        members = listOf(
            member(PIERRE_HOME, "Pierre", isMe = true),
            member(CARLA, "Carla"),
        ),
    )

    private fun group(
        id: String,
        name: String,
        members: List<GroupMember>,
    ): Group = Group(
        id = id,
        name = name,
        type = GroupType.TRIP,
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
        isMe: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.PEACH,
        isMe = isMe,
        hasJoined = true,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun prepareScenario(
        screen: Route.SplitBill = fromGroup,
        underneath: List<Route> = listOf(Route.Groups, tripExpenses),
        storedGroups: List<Group> = listOf(trip, home),
        isSatsUnitRead: Boolean = true,
    ) {
        val groups = FakeGroupRepository(initialGroups = storedGroups)
        expenses = FakeExpenseRepository(initialExpenses = listOf(savedBill))
        navigator = BackStackNavigator(startRoute = underneath.first()).also { navigator ->
            underneath.drop(1).forEach(navigator::navigate)
            navigator.navigate(screen)
        }
        val deletedBills = InMemoryDeletedBillRepository()
        viewModel = SplitBillViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            route = screen,
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
            categoryUseCases = BillCategoryUseCases(
                observeCategoryTree = ObserveCategoryTreeUseCase(FakeCategoryRepository()),
                observeCategoryMap = ObserveCategoryMapUseCase(FakeCategoryTermRepository()),
                saveGroupCategory = { _, _, _, _, _, _, _ -> Result.success("") },
                deleteGroupCategory = { Result.success(Unit) },
            ),
            saveSplitAsDefault = { _, _, _, _ -> Result.success(Unit) },
            offerCategoryTeach = {},
            takePickedReceipt = { null },
            readReceipt = { _, _, _ -> CompletableDeferred<ReceiptScan>().also { pendingReads += it }.await() },
            attachBillImage = { _, _, _, _, _ -> Result.success(BillAttachmentAddition.ADDED) },
            // Why: the form opens once bitcoin's unit is read, so one never read keeps it from loading.
            observeBitcoinInSats = { if (isSatsUnitRead) flowOf(false) else MutableSharedFlow() },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
        )
    }

    private companion object {
        // Why: 2025-10-30T15:00:00Z.
        const val NOW = 1_761_836_400_000L
        const val YESTERDAY = NOW - 86_400_000L
        const val TRIP_ID = "trip"
        const val HOME_ID = "home"
        const val EXPENSE_ID = "expense-1"
        const val ANA = "ana"
        const val PIERRE_TRIP = "pierre-trip"
        const val PIERRE_HOME = "pierre-home"
        const val CARLA = "carla"
    }
}
