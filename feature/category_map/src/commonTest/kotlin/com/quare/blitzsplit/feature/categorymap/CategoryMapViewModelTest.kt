package com.quare.blitzsplit.feature.categorymap

import com.quare.blitzsplit.core.account.domain.model.AccountSnapshot
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTermChange
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryMapUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveMapCategoriesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RemoveCategoryTermUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RestoreCategoryMapUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SaveCategoryTermUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiEvent
import com.quare.blitzsplit.feature.categorymap.presentation.model.TermError
import com.quare.blitzsplit.feature.categorymap.presentation.viewmodel.CategoryMapViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryMapViewModelTest {
    private lateinit var terms: FakeCategoryTermRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: CategoryMapViewModel
    private val isOnline = MutableStateFlow(true)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the app map WHEN it opens THEN Transporte lists uber under Corrida por app`() = runTest {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        val transport = state.sections.first { it.category.id == AppCategory.TRANSPORT.key }
        val ride = transport.rows.first { it.category.id == AppCategory.TRANSPORT_RIDE.key }
        assertTrue(ride.terms.any { it.term == "uber" })
        assertEquals(60, state.sections.sumOf { it.termCount })
        assertTrue(state.showsDeviceOnlyNotice)
    }

    @Test
    fun `GIVEN uber typed WHEN searching THEN finds uber and uber eats marking the word`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnQueryChanged("Úber"))

        // Then
        val results = viewModel.uiState.value.searchResults
        assertEquals(listOf("uber eats", "uber"), results.map { it.term.term }.sorted().reversed())
        assertTrue(results.all { it.matchEnd - it.matchStart == 4 })
    }

    @Test
    fun `GIVEN a new term WHEN typed one letter or a word another category has THEN says why and Mover`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnAddTermClicked(AppCategory.LEISURE_PARTIES.key, ""))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermTextChanged("a"))
        val short = viewModel.uiState.value
        viewModel.onEvent(CategoryMapUiEvent.OnTermTextChanged("Uber"))
        val duplicate = viewModel.uiState.value

        // Then
        assertEquals(TermError.TOO_SHORT, short.termError)
        assertFalse(short.canSaveTerm)
        assertEquals("uber", duplicate.duplicateTerm?.term)
        assertEquals(AppCategory.TRANSPORT_RIDE.key, duplicate.duplicateCategory?.id)
        assertTrue(duplicate.canSaveTerm)
    }

    @Test
    fun `GIVEN uber moved to Festas WHEN saving THEN the map sends it there`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnAddTermClicked(AppCategory.LEISURE_PARTIES.key, "uber"))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermSaveClicked)

        // Then
        val state = viewModel.uiState.value
        assertNull(state.termForm)
        assertEquals(AppCategory.LEISURE_PARTIES.key, state.map.findOrNull("uber")?.categoryId)
        assertEquals(1, state.map.pendingCount)
    }

    @Test
    fun `GIVEN a term renamed WHEN saving THEN the old word leaves the map`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnTermClicked("feira"))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnTermTextChanged("hortifruti"))
        viewModel.onEvent(CategoryMapUiEvent.OnTermSaveClicked)

        // Then
        val map = viewModel.uiState.value.map
        assertNull(map.findOrNull("feira"))
        assertEquals(AppCategory.FOOD_MARKET.key, map.findOrNull("hortifruti")?.categoryId)
    }

    @Test
    fun `GIVEN a term removed WHEN undoing THEN it is back where it was`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnTermClicked("cafe"))
        viewModel.onEvent(CategoryMapUiEvent.OnTermRemoveClicked)
        val removed = viewModel.uiState.value

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnUndoRemoveClicked)

        // Then
        assertNull(removed.map.findOrNull("cafe"))
        assertEquals("café", removed.removedTerm?.term)
        val state = viewModel.uiState.value
        assertNull(state.removedTerm)
        assertEquals(AppCategory.FOOD_BAKERY.key, state.map.findOrNull("cafe")?.categoryId)
    }

    @Test
    fun `GIVEN changes and an own term WHEN restoring THEN the app terms come back and the own one stays`() = runTest {
        // Given
        prepareScenario(
            changes = listOf(
                changeOf("uber", AppCategory.LEISURE_PARTIES.key),
                changeOf("picanha", GROUP_CATEGORY_ID),
            ),
        )

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreClicked)
        val asking = viewModel.uiState.value
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreConfirmed)

        // Then
        assertTrue(asking.isRestoreConfirmOpen)
        assertEquals(listOf("picanha"), asking.ownTerms.map { it.term })
        val state = viewModel.uiState.value
        assertFalse(state.isRestoreConfirmOpen)
        assertEquals(AppCategory.TRANSPORT_RIDE.key, state.map.findOrNull("uber")?.categoryId)
        assertEquals(GROUP_CATEGORY_ID, state.map.findOrNull("picanha")?.categoryId)
    }

    @Test
    fun `GIVEN a group category WHEN the map opens THEN its terms say only in its group`() = runTest {
        // Given
        prepareScenario(changes = listOf(changeOf("picanha", GROUP_CATEGORY_ID)))

        // When
        val state = viewModel.uiState.value

        // Then
        val food = state.sections.first { it.category.id == AppCategory.FOOD.key }
        val barbecue = food.rows.first { it.category.id == GROUP_CATEGORY_ID }
        assertEquals("Viagem Floripa", barbecue.groupName)
        assertEquals(listOf("picanha"), barbecue.terms.map { it.term })
    }

    @Test
    fun `GIVEN offline with a change queued WHEN the map shows THEN the queue notice shows`() = runTest {
        // Given
        prepareScenario(changes = listOf(changeOf("pizzaria", AppCategory.FOOD_RESTAURANT.key, isPending = true)))

        // When
        isOnline.value = false

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showsOfflineNotice)
        assertTrue(state.map.findOrNull("pizzaria")?.isPending ?: false)
    }

    @Test
    fun `GIVEN opened from the profile WHEN Abrir Perfil THEN goes back to it`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnProfileClicked)

        // Then
        assertEquals(Route.Profile, navigator.backStack.value.last())
    }

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.BLUE,
        currency = GroupCurrency.BRL,
        members = emptyList(),
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
    )

    private fun changeOf(
        term: String,
        categoryId: String?,
        isPending: Boolean = false,
    ): CategoryTermChange = CategoryTermChange(
        key = com.quare.blitzsplit.core.expenses.domain.model.CategoryMatcher
            .keyOf(term),
        term = term,
        categoryId = categoryId,
        isRestored = false,
        isPending = isPending,
    )

    private fun prepareScenario(changes: List<CategoryTermChange> = emptyList()) {
        terms = FakeCategoryTermRepository(initialChanges = changes)
        val categories = FakeCategoryRepository(
            initialGroupCategories = listOf(
                GroupCategory(
                    id = GROUP_CATEGORY_ID,
                    groupId = GROUP_ID,
                    name = "Churrasco",
                    parentId = AppCategory.FOOD.key,
                    icon = null,
                    createdByMemberId = null,
                    isDeleted = false,
                    isPending = false,
                ),
            ),
        )
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.Profile)
            it.navigate(Route.CategoryMap)
        }
        viewModel = CategoryMapViewModel(
            navigator = navigator,
            saveCategoryTerm = SaveCategoryTermUseCase(terms) { NOW },
            removeCategoryTerm = RemoveCategoryTermUseCase(terms) { NOW },
            restoreCategoryMap = RestoreCategoryMapUseCase(terms) { NOW },
            observeCategoryMap = ObserveCategoryMapUseCase(terms),
            observeMapCategories = ObserveMapCategoriesUseCase(categories),
            observeGroups = { flowOf(listOf(trip)) },
            observeAccount = {
                flowOf(
                    AccountSnapshot(
                        status = AccountStatus.LocalOnly,
                        recoveryCode = null,
                        recoveryCodePrompt = RecoveryCodePrompt.NOT_DUE,
                        email = null,
                        signOutWhenSynced = false,
                    ),
                )
            },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
        )
    }

    private companion object {
        const val NOW = 1_000L
        const val GROUP_ID = "group-1"
        const val GROUP_CATEGORY_ID = "11111111-1111-1111-1111-111111111111"
    }
}
