package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryCategoryTeachRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTermChange
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptReadFailure
import com.quare.blitzsplit.core.expenses.domain.model.ReceiptScan
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CreateExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteGroupCategoryUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryMapUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.OfferCategoryTeachUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SaveGroupCategoryUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.UpdateExpenseUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
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
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategoryHint
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategorySuggestionReason
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.BillCategoryUseCases
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var categories: FakeCategoryRepository
    private lateinit var terms: FakeCategoryTermRepository
    private lateinit var teach: InMemoryCategoryTeachRepository
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
        assertEquals(true, group.categoryEditor?.isNameTaken)
        assertEquals(false, group.categoryEditor?.canSave)
        assertEquals(true, app.categoryEditor?.isNameTaken)
        assertEquals(false, elsewhere.categoryEditor?.isNameTaken)
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

    @Test
    fun `GIVEN Igual with everyone only for an exception WHEN another category opens THEN no group default`() =
        runTest {
            // Given
            prepareScenario(
                storedGroup = home.copy(
                    defaultSplit = GroupDefaultSplit(
                        mode = DefaultSplitMode.EQUAL,
                        weights = mapOf(PIERRE to 1L, ANA to 1L),
                        exceptions = listOf(
                            CategorySplit(
                                AppCategory.FOOD_MARKET.key,
                                DefaultSplitMode.SHARES,
                                mapOf(PIERRE to 3L, ANA to 1L),
                            ),
                        ),
                    ),
                ),
            )
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("120,00"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Conta de luz"))
            val power = viewModel.uiState.value
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Supermercado"))
            val market = viewModel.uiState.value

            // Then
            assertNull(power.defaultSplit)
            assertEquals(SplitMode.EQUAL, power.split.mode)
            assertEquals(AppCategory.FOOD_MARKET.key, market.defaultSplitException?.id)
        }

    private val pierre = GroupMember(
        id = PIERRE,
        name = "Pierre",
        avatarColor = AvatarColor.BLUE,
        isMe = true,
        hasJoined = true,
        userId = null,
        inviteCode = null,
        photoUrl = null,
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
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(pierre, ana),
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

    private val churrasco = GroupCategory(
        id = "churrasco",
        groupId = GROUP_ID,
        parentId = AppCategory.FOOD.key,
        name = "Churrasco",
        icon = null,
        color = null,
        createdByMemberId = ANA,
        isDeleted = false,
        isPending = false,
    )

    private val bill = Expense(
        id = BILL_ID,
        groupId = GROUP_ID,
        description = "Luz",
        currency = GroupCurrency.BRL,
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

    @Test
    fun `GIVEN pizzaria added to the map WHEN typing it THEN the bill is Restaurante by that word`() = runTest {
        // Given
        prepareScenario(
            termChanges = listOf(
                CategoryTermChange(
                    key = "pizzaria",
                    term = "pizzaria",
                    categoryId = AppCategory.FOOD_RESTAURANT.key,
                    isRestored = false,
                    isPending = true,
                ),
            ),
        )

        // When
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Pizzaria do centro"))

        // Then
        val state = viewModel.uiState.value
        assertEquals(AppCategory.FOOD_RESTAURANT.key, state.category.id)
        assertEquals(CategoryHint.BY_TERM, state.categoryHint)
    }

    @Test
    fun `GIVEN a category picked by hand the map would not pick WHEN saving THEN asks to teach the map`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("180,00"))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Aluguel da prancha"))
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(SplitBillUiEvent.OnCategoryPicked(AppCategory.LEISURE_SPORTS.key))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        val offer = teach.observeOffer().first()
        assertEquals(AppCategory.LEISURE_SPORTS.key, offer?.categoryId)
        assertEquals(AppCategory.HOME_RENT.key, offer?.mapCategoryId)
        assertEquals("Aluguel da prancha", offer?.title)
        assertEquals(GROUP_ID, offer?.groupId)
    }

    @Test
    fun `GIVEN a bill split from the group stats WHEN saving a hand-picked category THEN nothing asks to teach`() =
        runTest {
            // Given
            prepareScenario(below = Route.GroupStats(GROUP_ID))
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("180,00"))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Aluguel da prancha"))
            viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
            viewModel.onEvent(SplitBillUiEvent.OnCategoryPicked(AppCategory.LEISURE_SPORTS.key))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(Route.GroupStats(GROUP_ID), navigator.backStack.value.last())
            assertNull(teach.observeOffer().first())
        }

    @Test
    fun `GIVEN the category the map picked WHEN saving THEN nothing asks to teach`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("312,00"))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // Then
        assertNull(teach.observeOffer().first())
    }

    @Test
    fun `GIVEN the picker WHEN tapping the map card THEN opens the category map over the bill`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryMapClicked)

        // Then
        assertEquals(Route.CategoryMap, navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN the categories read WHEN tapping the category THEN sends the category clicked`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)

        // Then
        assertEquals(listOf("bill_category_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the picker WHEN opening Transporte THEN sends the main toggled as expanded`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryMainToggled(AppCategory.TRANSPORT.key))

        // Then
        assertEquals(
            listOf("bill_category_main_toggled" to mapOf<String, Any>("is_expanded" to true)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN Transporte open in the picker WHEN tapping it again THEN sends the main toggled as hidden`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(SplitBillUiEvent.OnCategoryMainToggled(AppCategory.TRANSPORT.key))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryMainToggled(AppCategory.TRANSPORT.key))

        // Then
        assertEquals(
            listOf("bill_category_main_toggled" to mapOf<String, Any>("is_expanded" to false)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the picker WHEN tapping Outros in Todas as categorias THEN sends the category picked and no toggle`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCategoryMainToggled(AppCategory.OTHER.key))

            // Then
            assertNull(viewModel.uiState.value.categoryPicker)
            assertEquals(listOf("bill_category_picked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the picker WHEN picking Mercado THEN sends the category picked without it`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryPicked(AppCategory.FOOD_MARKET.key))

        // Then
        assertEquals(listOf("bill_category_picked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN a group category a sync deleted with the picker open WHEN picking it THEN sends nothing`() = runTest {
        // Given
        prepareScenario(groupCategories = listOf(churrasco))
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        categories.groupCategories.value = listOf(churrasco.copy(isDeleted = true))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryPicked("churrasco"))

        // Then
        assertFalse(viewModel.uiState.value.isCategoryPicked)
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the picker WHEN searching a category THEN sends nothing`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryQueryChanged("alug"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the picker WHEN closing it THEN sends nothing`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the picker WHEN tapping the map card THEN sends the category map clicked from Rachar conta`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryMapClicked)

        // Then
        assertEquals(
            listOf("category_map_clicked" to mapOf<String, Any>("source" to "split_bill")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the picker WHEN tapping Nova THEN sends the create clicked as a main category`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(
            SplitBillUiEvent.OnNewCategoryClicked(
                parentId = null,
                name = "Pet",
            ),
        )

        // Then
        assertEquals(
            listOf(
                "category_create_clicked" to mapOf<String, Any>(
                    "is_subcategory" to false,
                    "source" to "split_bill",
                ),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the picker WHEN tapping Nova em Alimentacao THEN sends the create clicked as a subcategory`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(
            SplitBillUiEvent.OnNewCategoryClicked(
                parentId = AppCategory.FOOD.key,
                name = "",
            ),
        )

        // Then
        assertEquals(
            listOf(
                "category_create_clicked" to mapOf<String, Any>(
                    "is_subcategory" to true,
                    "source" to "split_bill",
                ),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a group category in the picker WHEN editing it THEN sends the edit clicked without it`() = runTest {
        // Given
        prepareScenario(groupCategories = listOf(churrasco))
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnEditCategoryClicked("churrasco"))

        // Then
        assertEquals(
            listOf("category_edit_clicked" to mapOf<String, Any>("source" to "split_bill")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the picker WHEN editing a category no longer in the group THEN sends nothing`() = runTest {
        // Given
        prepareScenario(groupCategories = listOf(churrasco))
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnEditCategoryClicked("gone"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a new category WHEN typing its name THEN sends nothing`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(
            SplitBillUiEvent.OnNewCategoryClicked(
                parentId = null,
                name = "",
            ),
        )
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryNameChanged("Pet"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a new category WHEN picking Casa in Dentro de THEN sends the parent picked without it`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(
            SplitBillUiEvent.OnNewCategoryClicked(
                parentId = null,
                name = "Pet",
            ),
        )
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryParentPicked(AppCategory.HOME.key))

        // Then
        assertEquals(
            listOf("category_parent_picked" to mapOf<String, Any>("source" to "split_bill")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a new main category WHEN picking the paw icon THEN sends the icon picked`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(
            SplitBillUiEvent.OnNewCategoryClicked(
                parentId = null,
                name = "Pet",
            ),
        )
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryIconPicked(CategoryIcon.PAW))

        // Then
        assertEquals(
            listOf(
                "category_icon_picked" to mapOf<String, Any>(
                    "icon" to CategoryIcon.PAW,
                    "source" to "split_bill",
                ),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a new main category WHEN tapping Salvar THEN sends the tap then the new category saved`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(
            SplitBillUiEvent.OnNewCategoryClicked(
                parentId = null,
                name = "Pet",
            ),
        )
        viewModel.onEvent(SplitBillUiEvent.OnCategoryIconPicked(CategoryIcon.PAW))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryFormSaveClicked)

        // Then
        assertEquals(
            listOf(
                "category_save_clicked" to mapOf<String, Any>("source" to "split_bill"),
                "category_saved" to mapOf<String, Any>(
                    "is_new" to true,
                    "is_subcategory" to false,
                    "source" to "split_bill",
                ),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a group subcategory renamed WHEN tapping Salvar THEN sends the tap then the category saved as edited`() =
        runTest {
            // Given
            prepareScenario(groupCategories = listOf(churrasco))
            viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
            viewModel.onEvent(SplitBillUiEvent.OnEditCategoryClicked("churrasco"))
            viewModel.onEvent(SplitBillUiEvent.OnCategoryNameChanged("Churrasquinho"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCategoryFormSaveClicked)

            // Then
            assertEquals(
                listOf(
                    "category_save_clicked" to mapOf<String, Any>("source" to "split_bill"),
                    "category_saved" to mapOf<String, Any>(
                        "is_new" to false,
                        "is_subcategory" to true,
                        "source" to "split_bill",
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a name already in the category WHEN tapping Salvar THEN sends only the tap`() = runTest {
        // Given
        prepareScenario(groupCategories = listOf(churrasco))
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(
            SplitBillUiEvent.OnNewCategoryClicked(
                parentId = AppCategory.FOOD.key,
                name = "churrasco",
            ),
        )
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryFormSaveClicked)

        // Then
        assertEquals(
            listOf("category_save_clicked" to mapOf<String, Any>("source" to "split_bill")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a new category WHEN closing its form THEN sends nothing`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(
            SplitBillUiEvent.OnNewCategoryClicked(
                parentId = null,
                name = "Pet",
            ),
        )
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryFormDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a group category being edited WHEN tapping Apagar THEN sends the delete clicked`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(bill.copy(categoryId = "churrasco")),
            groupCategories = listOf(churrasco),
        )
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(SplitBillUiEvent.OnEditCategoryClicked("churrasco"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryDeleteClicked)

        // Then
        assertEquals(
            listOf("category_delete_clicked" to mapOf<String, Any>("source" to "split_bill")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN Apagar asking about a group category WHEN confirming THEN sends the delete confirmed`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(bill.copy(categoryId = "churrasco")),
            groupCategories = listOf(churrasco),
        )
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(SplitBillUiEvent.OnEditCategoryClicked("churrasco"))
        viewModel.onEvent(SplitBillUiEvent.OnCategoryDeleteClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryDeleteConfirmed)

        // Then
        assertEquals(
            listOf("category_delete_confirmed" to mapOf<String, Any>("source" to "split_bill")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN Apagar asking about a group category WHEN cancelling THEN sends the delete cancelled`() = runTest {
        // Given
        prepareScenario(
            stored = listOf(bill.copy(categoryId = "churrasco")),
            groupCategories = listOf(churrasco),
        )
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(SplitBillUiEvent.OnEditCategoryClicked("churrasco"))
        viewModel.onEvent(SplitBillUiEvent.OnCategoryDeleteClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryDeleteDismissed)

        // Then
        assertEquals(
            listOf("category_delete_cancelled" to mapOf<String, Any>("source" to "split_bill")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN no category question open WHEN a confirm or a dismiss arrives THEN sends nothing`() = runTest {
        // Given
        prepareScenario(groupCategories = listOf(churrasco))
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(SplitBillUiEvent.OnEditCategoryClicked("churrasco"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryDeleteConfirmed)
        viewModel.onEvent(SplitBillUiEvent.OnCategoryDeleteDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a category picked by hand WHEN saving the bill THEN the bill saved says it was picked`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("180,00"))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Aluguel da prancha"))
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(SplitBillUiEvent.OnCategoryPicked(AppCategory.LEISURE_SPORTS.key))
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
                    "is_from_receipt" to false,
                    "is_category_picked" to true,
                    "is_split_as_default" to false,
                ),
            ),
            trackedEvents,
        )
    }

    private fun prepareScenario(
        storedGroup: Group = home,
        stored: List<Expense> = emptyList(),
        groupCategories: List<GroupCategory> = emptyList(),
        expenseId: String? = null,
        termChanges: List<CategoryTermChange> = emptyList(),
        below: Route = Route.GroupDetail(
            GROUP_ID,
            joinedAs = null,
            tab = GroupTab.EXPENSES,
        ),
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup))
        expenses = FakeExpenseRepository(initialExpenses = stored)
        categories = FakeCategoryRepository(initialGroupCategories = groupCategories)
        val route = Route.SplitBill(
            GROUP_ID,
            expenseId = expenseId,
            picksGroupFirst = false,
            scansReceipt = false,
            duplicatesExpenseId = null,
        )
        terms = FakeCategoryTermRepository(initialChanges = termChanges)
        teach = InMemoryCategoryTeachRepository()
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(below)
            it.navigate(route)
        }
        val deletedBills = InMemoryDeletedBillRepository()
        viewModel = SplitBillViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
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
            categoryUseCases = BillCategoryUseCases(
                observeCategoryTree = ObserveCategoryTreeUseCase(categories),
                observeCategoryMap = ObserveCategoryMapUseCase(terms),
                saveGroupCategory = SaveGroupCategoryUseCase(categories) { NOW },
                deleteGroupCategory = DeleteGroupCategoryUseCase(categories) { NOW },
            ),
            saveSplitAsDefault = { _, _, _, _ -> Result.success(Unit) },
            offerCategoryTeach = OfferCategoryTeachUseCase(teach),
            takePickedReceipt = { null },
            readReceipt = { _, _, _ ->
                ReceiptScan.Failed(ReceiptReadFailure.UNREADABLE)
            },
            attachBillImage = { _, _, _, _, _ -> Result.success(BillAttachmentAddition.ADDED) },
            observeBitcoinInSats = { flowOf(false) },
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
