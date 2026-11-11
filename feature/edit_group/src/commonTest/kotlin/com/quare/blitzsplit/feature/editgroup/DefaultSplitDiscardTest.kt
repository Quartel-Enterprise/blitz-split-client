package com.quare.blitzsplit.feature.editgroup

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.groups.domain.model.CategorySplit
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.GetSplitIncomesUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.UpdateDefaultSplitUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.editgroup.presentation.model.DefaultSplitUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.DefaultSplitViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertTrue

class DefaultSplitDiscardTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: DefaultSplitViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the split as it opened WHEN reading the unsaved input THEN there is none`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.hasUnsavedInput)
        assertFalse(state.hasEdits)
        assertFalse(navigator.hasUnsavedInput.value)
    }

    @Test
    fun `GIVEN another way to split WHEN reading the unsaved input THEN there is some`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnModeSelected(DefaultSplitMode.SHARES))

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
        assertTrue(navigator.hasUnsavedInput.value)
    }

    @Test
    fun `GIVEN a person with one more part WHEN reading the unsaved input THEN there is some`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnPartsIncreased(ANA))

            // Then
            assertTrue(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a person with a part more and then less WHEN reading the unsaved input THEN there is none`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            viewModel.onEvent(DefaultSplitUiEvent.OnPartsIncreased(ANA))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnPartsDecreased(ANA))

            // Then
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
            assertFalse(navigator.hasUnsavedInput.value)
        }

    @Test
    fun `GIVEN Igual WHEN leaving a person out THEN there is unsaved input`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnPersonToggled(ANA))

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a new exception WHEN reading the unsaved input THEN there is some`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
        viewModel.onEvent(DefaultSplitUiEvent.OnAddExceptionClicked)
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionCategoryPicked(AppCategory.FOOD_MARKET.key))

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionSaveClicked)

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN an exception removed WHEN reading the unsaved input THEN there is some`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne.copy(exceptions = listOf(market))))
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionClicked(AppCategory.FOOD_MARKET.key))

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionRemoveClicked)

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a percentage changed WHEN reading the unsaved input THEN there is some`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = home.copy(defaultSplit = percent(75L, 25L)))

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnPercentChanged(ANA, "15"))

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN Pela renda with no income kept WHEN typing the incomes THEN there is unsaved input`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = income(75L, 25L)))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnIncomeChanged(PIERRE, "9.000,00"))
            viewModel.onEvent(DefaultSplitUiEvent.OnIncomeChanged(ANA, "3.000,00"))

            // Then
            assertTrue(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN Pela renda with the incomes kept WHEN reading the unsaved input THEN there is none`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(defaultSplit = income(75L, 25L)),
                storedIncomes = mapOf(PIERRE to 900_000L, ANA to 300_000L),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.hasUnsavedInput)
            assertFalse(navigator.hasUnsavedInput.value)
        }

    @Test
    fun `GIVEN Pela renda with the incomes kept WHEN an income changes THEN there is unsaved input`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(defaultSplit = income(75L, 25L)),
                storedIncomes = mapOf(PIERRE to 900_000L, ANA to 300_000L),
            )

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnIncomeChanged(ANA, "6.000,00"))

            // Then
            assertTrue(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN someone who joined meanwhile WHEN reading the state THEN there is no unsaved input but Salvar is on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = percent(75L, 25L)))

            // When
            groups.groups.value = listOf(
                home.copy(
                    defaultSplit = percent(75L, 25L),
                    members = home.members + member(CARLA, "Carla"),
                ),
            )

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.people.last().isNew)
            assertFalse(state.hasEdits)
            assertFalse(state.hasUnsavedInput)
            assertTrue(state.hasChanges)
            assertTrue(state.canSave)
            assertFalse(navigator.hasUnsavedInput.value)
        }

    @Test
    fun `GIVEN someone who joined before opening WHEN reading the state THEN only Salvar is on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(
                    defaultSplit = threeToOne,
                    members = home.members + member(CARLA, "Carla"),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.hasUnsavedInput)
            assertTrue(state.hasChanges)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN someone who joined and an edit WHEN reading the unsaved input THEN there is some`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(
                    defaultSplit = threeToOne,
                    members = home.members + member(CARLA, "Carla"),
                ),
            )

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnPartsIncreased(PIERRE))

            // Then
            assertTrue(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN someone who joined WHEN closing THEN closes at once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroup = home.copy(
                defaultSplit = threeToOne,
                members = home.members + member(CARLA, "Carla"),
            ),
        )

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN an edited split WHEN closing THEN asks to discard and the screen stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(DefaultSplitUiEvent.OnPersonToggled(ANA))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnBackClicked)

            // Then
            assertTrue(navigator.pendingDiscard.value)
            assertEquals(listOf(Route.Groups, groupDetail, editGroup, defaultSplit), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the discard question WHEN confirming THEN back on Editar grupo without saving`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(DefaultSplitUiEvent.OnPersonToggled(ANA))
            viewModel.onEvent(DefaultSplitUiEvent.OnBackClicked)

            // When
            navigator.confirmDiscard()

            // Then
            assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
            assertFalse(navigator.pendingDiscard.value)
            assertTrue(groups.savedDefaultSplits.isEmpty())
        }

    @Test
    fun `GIVEN the discard question WHEN cancelling THEN the screen stays open with the edit`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(DefaultSplitUiEvent.OnPersonToggled(ANA))
            viewModel.onEvent(DefaultSplitUiEvent.OnBackClicked)

            // When
            navigator.cancelDiscard()

            // Then
            assertEquals(listOf(Route.Groups, groupDetail, editGroup, defaultSplit), navigator.backStack.value)
            assertFalse(navigator.pendingDiscard.value)
            assertTrue(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN the split as it opened WHEN closing THEN closes at once`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN an edit changed back WHEN closing THEN closes at once`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(DefaultSplitUiEvent.OnPersonToggled(ANA))
        viewModel.onEvent(DefaultSplitUiEvent.OnPersonToggled(ANA))

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN an edited split WHEN saving THEN leaves without asking`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(DefaultSplitUiEvent.OnPersonToggled(ANA))

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnSaveClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
        assertFalse(navigator.hasUnsavedInput.value)
        assertEquals(1, groups.savedDefaultSplits.size)
    }

    @Test
    fun `GIVEN an edit and someone who joined WHEN saving THEN leaves without asking`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroup = home.copy(
                defaultSplit = threeToOne,
                members = home.members + member(CARLA, "Carla"),
            ),
        )
        viewModel.onEvent(DefaultSplitUiEvent.OnPartsIncreased(PIERRE))

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnSaveClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    @Test
    fun `GIVEN an edit and a save that fails WHEN saving THEN stays open and still has unsaved input`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.defaultSplitError = IllegalStateException("no disk")
            viewModel.onEvent(DefaultSplitUiEvent.OnPersonToggled(ANA))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnSaveClicked)

            // Then
            assertEquals(listOf(Route.Groups, groupDetail, editGroup, defaultSplit), navigator.backStack.value)
            assertTrue(viewModel.uiState.value.showsSaveError)
            assertTrue(viewModel.uiState.value.hasUnsavedInput)
            assertTrue(navigator.hasUnsavedInput.value)
            assertFalse(navigator.pendingDiscard.value)
        }

    @Test
    fun `GIVEN an edited split WHEN the group is deleted THEN leaves without asking`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(DefaultSplitUiEvent.OnPersonToggled(ANA))

        // When
        groups.groups.value = emptyList()

        // Then
        assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
        assertFalse(navigator.pendingDiscard.value)
    }

    private val editGroup = Route.EditGroup(GROUP_ID)

    private val defaultSplit = Route.DefaultSplit(GROUP_ID)

    private val groupDetail = Route.GroupDetail(
        GROUP_ID,
        joinedAs = null,
        tab = GroupTab.EXPENSES,
        isOverBill = false,
    )

    private val threeToOne = GroupDefaultSplit(
        mode = DefaultSplitMode.SHARES,
        weights = mapOf(PIERRE to 3L, ANA to 1L),
        exceptions = emptyList(),
    )

    private val market = CategorySplit(
        AppCategory.FOOD_MARKET.key,
        DefaultSplitMode.EQUAL,
        mapOf(
            PIERRE to 1L,
            ANA to 1L,
        ),
    )

    private val home = Group(
        id = GROUP_ID,
        name = "Apê 302",
        type = GroupType.HOME,
        color = AvatarColor.BLUE,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(
            member(PIERRE, "Pierre").copy(
                isMe = true,
                hasJoined = true,
            ),
            member(ANA, "Ana"),
        ),
        isPending = false,
        inviteCode = "AB12CD",
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    private fun percent(
        pierre: Long,
        ana: Long,
    ): GroupDefaultSplit = GroupDefaultSplit(
        mode = DefaultSplitMode.PERCENT,
        weights = mapOf(PIERRE to pierre, ANA to ana),
        exceptions = emptyList(),
    )

    private fun income(
        pierre: Long,
        ana: Long,
    ): GroupDefaultSplit = GroupDefaultSplit(
        mode = DefaultSplitMode.INCOME,
        weights = mapOf(PIERRE to pierre, ANA to ana),
        exceptions = emptyList(),
    )

    private fun member(
        id: String,
        name: String,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.GREEN,
        isMe = false,
        hasJoined = false,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun prepareScenario(
        storedGroup: Group = home,
        storedIncomes: Map<String, Long> = emptyMap(),
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup)).also {
            if (storedIncomes.isNotEmpty()) it.savedIncomes[GROUP_ID] = storedIncomes
        }
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(groupDetail)
            it.navigate(editGroup)
            it.navigate(defaultSplit)
        }
        viewModel = DefaultSplitViewModel(
            trackEvent = { _, _ -> },
            route = defaultSplit,
            navigator = navigator,
            updateDefaultSplit = UpdateDefaultSplitUseCase(groups) { 1_000L },
            getSplitIncomes = GetSplitIncomesUseCase(groups),
            decimalSeparator = { ',' },
            observeGroup = ObserveGroupUseCase(groups),
            observeCategoryTree = ObserveCategoryTreeUseCase(FakeCategoryRepository()),
            observeBitcoinInSats = { MutableStateFlow(false) },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val PIERRE = "member-1"
        const val ANA = "member-2"
        const val CARLA = "member-3"
    }
}
