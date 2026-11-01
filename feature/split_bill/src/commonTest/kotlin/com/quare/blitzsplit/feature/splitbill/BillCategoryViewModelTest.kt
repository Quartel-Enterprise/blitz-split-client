package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CreateExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteGroupCategoryUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SaveGroupCategoryUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.UpdateExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.groups.domain.model.CategorySplit
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategoryHint
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategorySuggestionReason
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: a bill's category (7.7): suggested by the map from its description, picked by hand, and the group's own (7.8).
class BillCategoryViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var categories: FakeCategoryRepository
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
    fun `GIVEN Mercado typed WHEN saving THEN the bill is Alimentacao in Mercado by the map word`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("312,00"))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado"))
        val state = viewModel.uiState.value
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        assertEquals(AppCategory.FOOD_MARKET.key, state.category.id)
        assertEquals(AppCategory.FOOD.key, state.categoryParent?.id)
        assertEquals(CategoryHint.BY_TERM, state.categoryHint)
        assertEquals("mercado", state.categoryMatches.single().term)
        assertEquals(AppCategory.FOOD_MARKET.key, expenses.createdExpenses.single().categoryId)
    }

    @Test
    fun `GIVEN several terms WHEN typing THEN the longest chooses and every one shows`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Uber Eats no hotel"))

        // Then
        val state = viewModel.uiState.value
        assertEquals(AppCategory.FOOD_DELIVERY.key, state.category.id)
        assertEquals(listOf("uber eats", "hotel", "uber"), state.categoryMatches.map { it.term })
        assertEquals(CategoryHint.BY_LONGEST_TERM, state.categoryHint)
    }

    @Test
    fun `GIVEN no term WHEN saving THEN the bill reads as Outros and stores none`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("10,00"))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Rifa da formatura"))
        val state = viewModel.uiState.value
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        assertTrue(state.category.isOther)
        assertEquals(CategoryHint.NO_TERM, state.categoryHint)
        assertNull(expenses.createdExpenses.single().categoryId)
    }

    @Test
    fun `GIVEN a category picked by hand WHEN typing more THEN it stays and the hint says the map differs`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Aluguel"))
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryPicked(AppCategory.LEISURE_SPORTS.key))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Aluguel da prancha"))

        // Then
        val state = viewModel.uiState.value
        assertEquals(AppCategory.LEISURE_SPORTS.key, state.category.id)
        assertEquals(AppCategory.HOME_RENT.key, state.mapCategory?.id)
        assertEquals(CategoryHint.CHANGED, state.categoryHint)
        assertNull(state.categoryPicker)
    }

    @Test
    fun `GIVEN a saved bill WHEN editing it THEN keeps its category whatever the map says`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                bill.copy(
                    description = "Mercado",
                    categoryId = AppCategory.HOME_GAS.key,
                ),
            ),
            expenseId = BILL_ID,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(AppCategory.HOME_GAS.key, state.category.id)
        assertEquals(CategoryHint.PICKED, state.categoryHint)
    }

    @Test
    fun `GIVEN bills of the group WHEN opening the picker THEN suggests the map first then the most used`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                bill.copy(
                    id = "b1",
                    categoryId = AppCategory.FOOD_RESTAURANT.key,
                ),
                bill.copy(
                    id = "b2",
                    categoryId = AppCategory.FOOD_RESTAURANT.key,
                ),
                bill.copy(
                    id = "b3",
                    categoryId = AppCategory.TRAVEL_LODGING.key,
                ),
            ),
            groupCategories = listOf(churrasco),
        )
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)

        // Then
        val state = viewModel.uiState.value
        assertEquals(
            listOf(
                AppCategory.FOOD_MARKET.key,
                AppCategory.FOOD_RESTAURANT.key,
                AppCategory.TRAVEL_LODGING.key,
                "churrasco",
            ),
            state.categorySuggestions.map { it.category.id },
        )
        assertEquals(
            listOf(
                CategorySuggestionReason.DESCRIPTION,
                CategorySuggestionReason.MOST_USED,
                CategorySuggestionReason.USED,
                CategorySuggestionReason.GROUP,
            ),
            state.categorySuggestions.map { it.reason },
        )
        assertTrue(state.categorySuggestions.first().isSelected)
        assertEquals(AppCategory.FOOD.key, state.categoryPicker?.openMainId)
        assertEquals(AppCategory.FOOD_RESTAURANT.key, state.mostUsedCategory?.id)
    }

    @Test
    fun `GIVEN a search WHEN typing THEN finds by the start of a word and nothing for an unknown one`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryQueryChanged("alug"))
        val found = viewModel.uiState.value
        viewModel.onEvent(SplitBillUiEvent.OnCategoryQueryChanged("Pet"))
        val none = viewModel.uiState.value

        // Then
        assertEquals(
            listOf(AppCategory.HOME_RENT.key, AppCategory.TRAVEL_CAR_RENTAL.key),
            found.categoryResults.map { it.id },
        )
        assertFalse(found.showsNoCategoryFound)
        assertTrue(none.showsNoCategoryFound)
    }

    @Test
    fun `GIVEN Pet not found WHEN creating it from the search THEN the group has it and the bill takes it`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)

        // When
        viewModel.onEvent(
            SplitBillUiEvent.OnNewCategoryClicked(
                parentId = null,
                name = "Pet",
            ),
        )
        viewModel.onEvent(SplitBillUiEvent.OnCategoryIconPicked(CategoryIcon.PAW))
        viewModel.onEvent(SplitBillUiEvent.OnCategoryFormSaveClicked)

        // Then
        val created = categories.groupCategories.value.single()
        assertEquals(listOf("Pet", null, PIERRE), listOf(created.name, created.parentId, created.createdByMemberId))
        val state = viewModel.uiState.value
        assertEquals(created.id, state.category.id)
        assertNull(state.categoryPicker)
        assertNull(state.categoryForm)
    }

    @Test
    fun `GIVEN a name already in the same category WHEN typing it THEN Salvar waits elsewhere it does not`() = runTest {
        // Given
        prepareScenario(groupCategories = listOf(churrasco))
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(
            SplitBillUiEvent.OnNewCategoryClicked(
                parentId = AppCategory.FOOD.key,
                name = "",
            ),
        )

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryNameChanged("churrasco"))
        val group = viewModel.uiState.value
        viewModel.onEvent(SplitBillUiEvent.OnCategoryNameChanged("mercado"))
        val app = viewModel.uiState.value
        viewModel.onEvent(SplitBillUiEvent.OnCategoryParentPicked(AppCategory.HOME.key))
        val elsewhere = viewModel.uiState.value

        // Then
        assertTrue(group.isCategoryNameTaken)
        assertFalse(group.canSaveCategory)
        assertTrue(app.isCategoryNameTaken)
        assertFalse(elsewhere.isCategoryNameTaken)
        assertEquals(CategoryIcon.HOME, elsewhere.categoryForm?.icon)
    }

    @Test
    fun `GIVEN the bill on a group subcategory WHEN deleting it THEN its bills move to the parent`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(
                bill.copy(categoryId = "churrasco"),
                bill.copy(
                    id = "b2",
                    categoryId = "churrasco",
                ),
            ),
            groupCategories = listOf(churrasco),
        )
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(SplitBillUiEvent.OnCategoryPicked("churrasco"))
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(SplitBillUiEvent.OnEditCategoryClicked("churrasco"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryDeleteClicked)
        val asked = viewModel.uiState.value.categoryDeletion
        viewModel.onEvent(SplitBillUiEvent.OnCategoryDeleteConfirmed)

        // Then
        assertEquals(2, asked?.billCount)
        assertEquals(AppCategory.FOOD.key, asked?.movesTo?.id)
        val state = viewModel.uiState.value
        assertEquals(AppCategory.FOOD.key, state.category.id)
        assertNull(state.categoryDeletion)
        assertTrue(
            categories.groupCategories.value
                .single()
                .isDeleted,
        )
    }

    @Test
    fun `GIVEN an exception for Mercado WHEN the bills category changes THEN its split follows the category`() =
        runTest {
            // Given
            prepareScenario(
                storedGroup = home.copy(
                    defaultSplit = GroupDefaultSplit(
                        mode = DefaultSplitMode.SHARES,
                        weights = mapOf(PIERRE to 3L, ANA to 1L),
                        exceptions = listOf(
                            CategorySplit(
                                AppCategory.FOOD_MARKET.key,
                                DefaultSplitMode.EQUAL,
                                mapOf(PIERRE to 1L, ANA to 1L),
                            ),
                        ),
                    ),
                ),
            )
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("248,00"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Supermercado"))
            val market = viewModel.uiState.value
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Conta de luz"))
            val power = viewModel.uiState.value

            // Then
            assertEquals(SplitMode.EQUAL, market.split.mode)
            assertEquals(AppCategory.FOOD_MARKET.key, market.defaultSplitException?.id)
            assertTrue(market.splitFollowsDefault)
            assertEquals(SplitMode.SHARES, power.split.mode)
            assertNull(power.defaultSplitException)
        }

    private val pierre = GroupMember(
        id = PIERRE,
        name = "Pierre",
        avatarColor = AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
        userId = null,
        inviteCode = null,
    )
    private val ana = pierre.copy(
        id = ANA,
        name = "Ana",
        avatarColor = AvatarColor.GREEN,
        isMe = false,
    )

    private val home = Group(
        id = GROUP_ID,
        name = "Apê 302",
        type = GroupType.HOME,
        color = AvatarColor.BLUE,
        currency = GroupCurrency.BRL,
        members = listOf(pierre, ana),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    private val churrasco = GroupCategory(
        id = "churrasco",
        groupId = GROUP_ID,
        parentId = AppCategory.FOOD.key,
        name = "Churrasco",
        icon = null,
        createdByMemberId = ANA,
        isDeleted = false,
        isPending = false,
    )

    private val bill = Expense(
        id = BILL_ID,
        groupId = GROUP_ID,
        description = "Luz",
        amountMinor = 10_000,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(PIERRE, 10_000)),
        shares = listOf(
            ExpenseShare(
                memberId = PIERRE,
                amountMinor = 5_000,
                weight = 1,
            ),
            ExpenseShare(
                memberId = ANA,
                amountMinor = 5_000,
                weight = 1,
            ),
        ),
        items = null,
        categoryId = null,
        spentAt = 1_000L,
        isPending = false,
        updatedAt = 1_000L,
        updatedByMemberId = null,
    )

    private fun prepareScenario(
        storedGroup: Group = home,
        stored: List<Expense> = emptyList(),
        groupCategories: List<GroupCategory> = emptyList(),
        expenseId: String? = null,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup))
        expenses = FakeExpenseRepository(initialExpenses = stored)
        categories = FakeCategoryRepository(initialGroupCategories = groupCategories)
        val route = Route.SplitBill(
            GROUP_ID,
            expenseId = expenseId,
            picksGroupFirst = false,
        )
        val navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.GroupDetail(GROUP_ID, joinedAs = null))
            it.navigate(route)
        }
        val deletedBills = InMemoryDeletedBillRepository()
        viewModel = SplitBillViewModel(
            route = route,
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
            observeCategoryTree = ObserveCategoryTreeUseCase(categories),
            saveGroupCategory = SaveGroupCategoryUseCase(categories) { NOW },
            deleteGroupCategory = DeleteGroupCategoryUseCase(categories) { NOW },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val BILL_ID = "bill-1"
        const val PIERRE = "member-1"
        const val ANA = "member-2"
        const val NOW = 10_000L
    }
}
