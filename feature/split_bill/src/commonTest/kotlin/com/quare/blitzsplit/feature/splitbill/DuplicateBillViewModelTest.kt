package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryCategoryTeachRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.BillItem
import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReadFailure
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptScan
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CreateExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryMapUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.OfferCategoryTeachUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.UpdateExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
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
import kotlinx.coroutines.flow.first
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
import kotlin.time.Instant

// Why: "Duplicar" on a bill, SplitBillViewModel opened as a new bill filled in with a saved one.
class DuplicateBillViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var teach: InMemoryCategoryTeachRepository
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
    fun `GIVEN a saved bill WHEN Duplicar opens THEN the form is a new bill with nothing to delete`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertFalse(state.isEditing)
            assertFalse(state.canDelete)
            assertNull(state.conflict)
            assertNull(state.deletion)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a saved bill WHEN Duplicar opens THEN description amount currency category and payer are copied`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("Mercado", state.description)
            assertEquals("312,00", state.amountText)
            assertEquals(GroupCurrency.USD, state.currency)
            assertEquals(AppCategory.FOOD_MARKET.key, state.categoryId)
            assertEquals(ANA, state.payerId)
        }

    @Test
    fun `GIVEN a bill saved days ago WHEN Duplicar opens THEN the date is today and not the original one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(NOW_MILLIS, state.spentAt)
            assertEquals(RelativeDay.TODAY, state.spentDay)
        }

    @Test
    fun `GIVEN a group with a default split WHEN Duplicar opens THEN the split is the one saved and not the default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(defaultSplit = threeToOne))

            // When
            val split = viewModel.uiState.value.split

            // Then
            assertEquals(SplitMode.EQUAL, split.mode)
            assertEquals(
                mapOf(ANA to true, PIERRE to true, BRUNO to true, CARLA to false),
                split.rows.associate { it.person.id to it.isIncluded },
            )
            assertEquals(savedEqualShares, split.shares)
        }

    @Test
    fun `GIVEN a saved Valores bill WHEN Duplicar opens THEN each value is copied`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpense = valoresBill)

        // When
        val split = viewModel.uiState.value.split

        // Then
        assertEquals(SplitMode.AMOUNT, split.mode)
        assertEquals(
            mapOf(ANA to "200,00", PIERRE to "112,00", BRUNO to "", CARLA to ""),
            split.rows.associate { it.person.id to it.valueText },
        )
    }

    @Test
    fun `GIVEN a bill two people paid WHEN Duplicar opens THEN each keeps what they paid`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedExpense = savedBill.copy(
                payers = listOf(ExpensePayer(ANA, 20_000L), ExpensePayer(PIERRE, 11_200L)),
            ),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals("", state.payerId)
        assertEquals(listOf(ANA, PIERRE), state.payerIds)
        assertEquals(true, state.severalPayers?.addsUp)
    }

    @Test
    fun `GIVEN a member of the bill who left the group WHEN Duplicar opens THEN they are out of the split`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(members = listOf(ana, pierre, carla)))

            // When
            val split = viewModel.uiState.value.split

            // Then
            assertFalse(split.rows.any { it.person.id == BRUNO })
            assertTrue(split.shares.orEmpty().none { it.memberId == BRUNO })
        }

    @Test
    fun `GIVEN a payer who left the group WHEN Duplicar opens THEN they are out of the payers`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(members = listOf(ana, pierre, carla)),
                storedExpense = savedBill.copy(
                    payers = listOf(ExpensePayer(ANA, 20_000L), ExpensePayer(BRUNO, 11_200L)),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(BRUNO in state.payerIds)
            assertFalse(state.payersToSave.any { it.memberId == BRUNO })
        }

    @Test
    fun `GIVEN the form open WHEN a member leaves the group THEN they leave the split too`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        groups.groups.value = listOf(trip.copy(members = listOf(ana, pierre, carla)))

        // Then
        val split = viewModel.uiState.value.split
        assertFalse(split.rows.any { it.person.id == BRUNO })
        assertEquals("Mercado", viewModel.uiState.value.description)
    }

    @Test
    fun `GIVEN the form open WHEN someone edits the original THEN what is typed is not overwritten`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))

            // When
            expenses.expenses.value = listOf(
                savedBill.copy(
                    description = "Padaria",
                    amountMinor = 40_000L,
                    payers = listOf(ExpensePayer(PIERRE, 40_000L)),
                    updatedAt = LATER_VERSION,
                    updatedByMemberId = PIERRE,
                ),
            )

            // Then
            val state = viewModel.uiState.value
            assertEquals("Feira", state.description)
            assertEquals("312,00", state.amountText)
            assertEquals(ANA, state.payerId)
            assertNull(state.conflict)
        }

    @Test
    fun `GIVEN the form open WHEN the original is deleted THEN the form stays open as typed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))

            // When
            expenses.expenses.value = emptyList()

            // Then
            assertEquals(fullStack, navigator.backStack.value)
            assertEquals("Feira", viewModel.uiState.value.description)
            assertEquals("312,00", viewModel.uiState.value.amountText)
            assertTrue(viewModel.uiState.value.canSave)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the form open WHEN the groups default split changes THEN the copied split stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            groups.groups.value = listOf(trip.copy(defaultSplit = threeToOne))

            // Then
            val split = viewModel.uiState.value.split
            assertEquals(SplitMode.EQUAL, split.mode)
            assertEquals(savedEqualShares, split.shares)
        }

    @Test
    fun `GIVEN the original is not found WHEN Duplicar opens THEN the form is an ordinary blank new bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = null)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertFalse(state.isEditing)
            assertFalse(state.canDelete)
            assertEquals("", state.description)
            assertEquals("", state.amountText)
            assertEquals(GroupCurrency.BRL, state.currency)
            assertEquals(RelativeDay.TODAY, state.spentDay)
            assertEquals(fullStack, navigator.backStack.value)
        }

    @Test
    fun `GIVEN the original missing as the form opened WHEN it shows up later THEN what is typed stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = null)
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))

            // When
            expenses.expenses.value = listOf(savedBill)

            // Then
            val state = viewModel.uiState.value
            assertEquals("Feira", state.description)
            assertEquals("", state.amountText)
            assertEquals(GroupCurrency.BRL, state.currency)
        }

    @Test
    fun `GIVEN a duplicate WHEN tapping Salvar THEN creates a new bill in the same group and updates none`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val created = expenses.createdExpenses.single()
            assertTrue(created.id != EXPENSE_ID)
            assertEquals(GROUP_ID, created.groupId)
            assertEquals("Mercado", created.description)
            assertEquals(31_200L, created.amountMinor)
            assertEquals(GroupCurrency.USD, created.currency)
            assertEquals(AppCategory.FOOD_MARKET.key, created.categoryId)
            assertEquals(listOf(ExpensePayer(ANA, 31_200L)), created.payers)
            assertEquals(SplitMode.EQUAL, created.splitMode)
            assertEquals(savedEqualShares, created.shares)
            assertEquals(NOW_MILLIS, created.spentAt)
            assertTrue(expenses.updatedExpenses.isEmpty())
            assertEquals(savedBill, expenses.expenses.value.single { it.id == EXPENSE_ID })
        }

    @Test
    fun `GIVEN a duplicate changed WHEN tapping Salvar THEN saves what was typed`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("300,00"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        val created = expenses.createdExpenses.single()
        assertEquals("Feira", created.description)
        assertEquals(30_000L, created.amountMinor)
        assertEquals(listOf(ExpensePayer(ANA, 30_000L)), created.payers)
        assertEquals(30_000L, created.shares.sumOf { it.amountMinor })
    }

    @Test
    fun `GIVEN a duplicate WHEN tapping Salvar THEN goes back to the group like any new bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(fullStack.dropLast(1), navigator.backStack.value)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN a duplicate keeping a category the map would not pick WHEN tapping Salvar THEN nothing asks to teach`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = rentedBoardBill)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(AppCategory.LEISURE_SPORTS.key, expenses.createdExpenses.single().categoryId)
            assertNull(teach.observeOffer().first())
        }

    @Test
    fun `GIVEN a duplicate whose category was picked and put back WHEN tapping Salvar THEN nothing asks to teach`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = rentedBoardBill)
            pickCategory(AppCategory.FOOD_MARKET)
            pickCategory(AppCategory.LEISURE_SPORTS)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertNull(teach.observeOffer().first())
        }

    @Test
    fun `GIVEN a duplicate whose category was changed to one the map would not pick WHEN saving THEN asks to teach`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            pickCategory(AppCategory.LEISURE_SPORTS)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val created = expenses.createdExpenses.single()
            val offer = teach.observeOffer().first()
            assertEquals(AppCategory.LEISURE_SPORTS.key, created.categoryId)
            assertEquals(created.id, offer?.expenseId)
            assertEquals(GROUP_ID, offer?.groupId)
            assertEquals("Mercado", offer?.title)
            assertEquals(AppCategory.LEISURE_SPORTS.key, offer?.categoryId)
            assertEquals(AppCategory.FOOD_MARKET.key, offer?.mapCategoryId)
        }

    @Test
    fun `GIVEN a duplicate whose category moved away from the originals WHEN tapping Salvar THEN asks to teach`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = rentedBoardBill)
            pickCategory(AppCategory.FOOD_MARKET)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val offer = teach.observeOffer().first()
            assertEquals("Aluguel da prancha", offer?.title)
            assertEquals(AppCategory.FOOD_MARKET.key, offer?.categoryId)
            assertEquals(AppCategory.HOME_RENT.key, offer?.mapCategoryId)
        }

    @Test
    fun `GIVEN the original is not found WHEN a hand-picked category is saved THEN asks to teach like a new bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = null)
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("180,00"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Aluguel da prancha"))
            pickCategory(AppCategory.LEISURE_SPORTS)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val offer = teach.observeOffer().first()
            assertEquals(AppCategory.LEISURE_SPORTS.key, offer?.categoryId)
            assertEquals(AppCategory.HOME_RENT.key, offer?.mapCategoryId)
        }

    @Test
    fun `GIVEN a bill split by its items WHEN Duplicar opens THEN the new bill splits by the same items`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = itemsBill)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertFalse(state.isEditing)
            assertTrue(state.canSave)
            assertEquals(SplitMode.ITEMS, state.split.mode)
            assertEquals(billItems, state.split.items)
            assertEquals(
                billItems.items.map { it.name },
                state.split.items
                    ?.items
                    ?.map { it.name },
            )
        }

    @Test
    fun `GIVEN a duplicate split by items WHEN tapping Salvar THEN creates a bill with those items and updates none`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpense = itemsBill)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            val created = expenses.createdExpenses.single()
            assertTrue(created.id != EXPENSE_ID)
            assertEquals(SplitMode.ITEMS, created.splitMode)
            assertEquals(billItems, created.items)
            assertEquals(itemsBill.amountMinor, created.amountMinor)
            assertEquals(itemsBill.shares, created.shares)
            assertEquals(NOW_MILLIS, created.spentAt)
            assertTrue(expenses.updatedExpenses.isEmpty())
            assertEquals(itemsBill, expenses.expenses.value.single { it.id == EXPENSE_ID })
        }

    private val route = Route.SplitBill(
        GROUP_ID,
        expenseId = null,
        picksGroupFirst = false,
        scansReceipt = false,
        duplicatesExpenseId = EXPENSE_ID,
    )

    private val underneath = listOf(
        Route.Groups,
        Route.GroupDetail(
            GROUP_ID,
            joinedAs = null,
            tab = GroupTab.EXPENSES,
            isOverBill = false,
        ),
    )

    private val fullStack = underneath + route

    private val yesterdayMillis = Instant.parse("2026-10-29T20:00:00Z").toEpochMilliseconds()

    private val savedEqualShares = listOf(
        ExpenseShare(ANA, 10_400L, weight = 1L),
        ExpenseShare(PIERRE, 10_400L, weight = 1L),
        ExpenseShare(BRUNO, 10_400L, weight = 1L),
    )

    // Why: "Mercado", US$ 312,00 Ana paid yesterday in a BRL group, split equally among Ana, Pierre and Bruno.
    private val savedBill = Expense(
        id = EXPENSE_ID,
        groupId = GROUP_ID,
        description = "Mercado",
        currency = GroupCurrency.USD,
        amountMinor = 31_200L,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(ANA, 31_200L)),
        shares = savedEqualShares,
        items = null,
        categoryId = AppCategory.FOOD_MARKET.key,
        spentAt = yesterdayMillis,
        isPending = false,
        updatedAt = SAVED_VERSION,
        updatedByMemberId = ANA,
    )

    private val valoresBill = savedBill.copy(
        splitMode = SplitMode.AMOUNT,
        shares = listOf(
            ExpenseShare(ANA, 20_000L, weight = 20_000L),
            ExpenseShare(PIERRE, 11_200L, weight = 11_200L),
        ),
    )

    // Why: "Aluguel da prancha" in Lazer e esportes, which the map reads as rent, so saving it once offered the map.
    private val rentedBoardBill = savedBill.copy(
        description = "Aluguel da prancha",
        categoryId = AppCategory.LEISURE_SPORTS.key,
    )

    // Why: one Pizza of US$ 100,00 Ana and Pierre split, with the service of 10% and a couvert of US$ 5,00 each.
    private val billItems = BillItems(
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
        servicePercent = 10L,
        couvertMinor = 500L,
        couvertMemberIds = listOf(ANA, PIERRE),
        extraMinor = 0L,
        isFromReceipt = false,
    )

    private val itemsBill = savedBill.copy(
        splitMode = SplitMode.ITEMS,
        amountMinor = 12_000L,
        payers = listOf(ExpensePayer(ANA, 12_000L)),
        shares = listOf(
            ExpenseShare(ANA, 6_000L, weight = 6_000L),
            ExpenseShare(PIERRE, 6_000L, weight = 6_000L),
        ),
        items = billItems,
    )

    private val threeToOne = GroupDefaultSplit(
        mode = DefaultSplitMode.SHARES,
        weights = mapOf(PIERRE to 3L, ANA to 1L),
        exceptions = emptyList(),
    )

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

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(ana, pierre, bruno, carla),
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
        color: AvatarColor,
        isMe: Boolean = false,
        hasJoined: Boolean = false,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = color,
        isMe = isMe,
        hasJoined = hasJoined,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun pickCategory(category: AppCategory) {
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(SplitBillUiEvent.OnCategoryPicked(category.key))
    }

    private fun TestScope.prepareScenario(
        storedGroup: Group = trip,
        storedExpense: Expense? = savedBill,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup))
        expenses = FakeExpenseRepository(initialExpenses = listOfNotNull(storedExpense))
        teach = InMemoryCategoryTeachRepository()
        val deletedBills = InMemoryDeletedBillRepository()
        navigator = BackStackNavigator(startRoute = underneath.first()).also { navigator ->
            underneath.drop(1).forEach(navigator::navigate)
            navigator.navigate(route)
        }
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
            offerCategoryTeach = OfferCategoryTeachUseCase(teach),
            takePickedReceipt = { null },
            readReceipt = { _, _, _ -> ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE) },
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
        const val CARLA = "member-4"

        // Why: 2026-10-30T15:00:00Z.
        const val NOW_MILLIS = 1_793_372_400_000L
        const val SAVED_VERSION = 1_793_300_000_000L
        const val LATER_VERSION = 1_793_350_000_000L
    }
}
