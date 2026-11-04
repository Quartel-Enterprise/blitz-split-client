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
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultSplitViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
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
    fun `GIVEN no default WHEN the screen opens THEN everyone is in equally with nothing to save`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals(DefaultSplitMode.EQUAL, state.mode)
            assertTrue(state.people.all { it.isIncluded })
            assertEquals(10_000L, state.exampleTotalMinor)
            assertEquals(5_000L, state.exampleEachMinor)
            assertTrue(state.joinedPeople.isEmpty())
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN six people in Igual WHEN reading the example THEN it is a round bill split evenly`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(members = (1..6).map { member("member-$it", "Pessoa $it") }))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(12_000L, state.exampleTotalMinor)
            assertEquals(2_000L, state.exampleEachMinor)
        }

    @Test
    fun `GIVEN 3 to 1 WHEN the screen opens THEN each one has their parts and part of a bill`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))

            // When
            val rows = viewModel.uiState.value.rows

            // Then
            assertEquals(DefaultSplitMode.SHARES, viewModel.uiState.value.mode)
            assertEquals(listOf(3L, 1L), rows.map { it.weight })
            assertEquals(listOf(7_500L, 2_500L), rows.map { it.basisPoints })
            assertEquals(listOf(7_500L, 2_500L), rows.map { it.exampleMinor })
        }

    @Test
    fun `GIVEN percentages adding up to 90 WHEN reading the state THEN it says what is missing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = percent(75L, 25L)))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnPercentChanged(ANA, "15"))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPercentError)
            assertEquals(90L, state.percentSum)
            assertEquals(10L, state.remainingPercent)
            assertEquals(1_500L, state.rows.last().exampleMinor)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN Pela renda WHEN typing each income and saving THEN the group gets only the percentages`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(DefaultSplitUiEvent.OnModeSelected(DefaultSplitMode.INCOME))
            viewModel.onEvent(DefaultSplitUiEvent.OnIncomeChanged(PIERRE, "9.000,00"))
            viewModel.onEvent(DefaultSplitUiEvent.OnIncomeChanged(ANA, "3.000,00"))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                GROUP_ID to GroupDefaultSplit(DefaultSplitMode.INCOME, mapOf(PIERRE to 75L, ANA to 25L), emptyList()),
                groups.savedDefaultSplits.single(),
            )
            assertEquals(mapOf(PIERRE to 900_000L, ANA to 300_000L), groups.savedIncomes[GROUP_ID])
            assertEquals(editGroup, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN bitcoin in sats WHEN typing incomes THEN they are whole sats and the example is 100 sats`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(currency = GroupCurrency.BTC),
                showsSats = true,
            )
            viewModel.onEvent(DefaultSplitUiEvent.OnModeSelected(DefaultSplitMode.INCOME))
            viewModel.onEvent(DefaultSplitUiEvent.OnIncomeChanged(PIERRE, "9000"))
            viewModel.onEvent(DefaultSplitUiEvent.OnIncomeChanged(ANA, "3000"))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnSaveClicked)

            // Then
            assertEquals(100L, viewModel.uiState.value.exampleTotalMinor)
            assertEquals(mapOf(PIERRE to 9_000L, ANA to 3_000L), groups.savedIncomes[GROUP_ID])
        }

    @Test
    fun `GIVEN incomes typed in bitcoin WHEN sats turn on while open THEN each keeps its value in sats`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(currency = GroupCurrency.BTC))
            viewModel.onEvent(DefaultSplitUiEvent.OnModeSelected(DefaultSplitMode.INCOME))
            viewModel.onEvent(DefaultSplitUiEvent.OnIncomeChanged(PIERRE, "0,00009"))

            // When
            bitcoinInSats.value = true

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsSats)
            assertEquals("9.000", state.people.first { it.id == PIERRE }.incomeText)
        }

    @Test
    fun `GIVEN Pela renda from another device WHEN the screen opens THEN it shows the saved percentages`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(defaultSplit = percent(75L, 25L).copy(mode = DefaultSplitMode.INCOME)),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.people.all { it.incomeText.isEmpty() })
            assertEquals(mapOf(PIERRE to 75L, ANA to 25L), state.weights)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN incomes this device typed WHEN the screen opens THEN they fill the fields`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroup = home.copy(defaultSplit = percent(75L, 25L).copy(mode = DefaultSplitMode.INCOME)),
            storedIncomes = mapOf(PIERRE to 900_000L, ANA to 300_000L),
        )

        // When
        val people = viewModel.uiState.value.people

        // Then
        assertEquals(listOf("9.000,00", "3.000,00"), people.map { it.incomeText })
        assertFalse(viewModel.uiState.value.hasChanges)
    }

    @Test
    fun `GIVEN incomes another member replaced WHEN the screen opens THEN it shows the group percentages`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(defaultSplit = percent(60L, 40L).copy(mode = DefaultSplitMode.INCOME)),
                storedIncomes = mapOf(PIERRE to 900_000L, ANA to 300_000L),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.people.all { it.incomeText.isEmpty() })
            assertEquals(mapOf(PIERRE to 60L, ANA to 40L), state.weights)
            assertFalse(state.hasChanges)
        }

    @Test
    fun `GIVEN someone who joined since WHEN the screen opens THEN they start with 1 part until saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(
                    defaultSplit = threeToOne,
                    members =
                        home.members + member(CARLA, "Carla"),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(listOf(CARLA), state.joinedPeople.map { it.id })
            assertEquals(1L, state.weights[CARLA])
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN someone new WHEN saving THEN they are in the saved split and the notice goes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = home.copy(
                    defaultSplit = threeToOne,
                    members =
                        home.members + member(CARLA, "Carla"),
                ),
            )

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                mapOf(PIERRE to 3L, ANA to 1L, CARLA to 1L),
                groups.savedDefaultSplits
                    .single()
                    .second
                    ?.weights,
            )
            assertTrue(
                viewModel.uiState.value.joinedPeople
                    .isEmpty(),
            )
        }

    @Test
    fun `GIVEN Porcentagem WHEN someone joins while the screen is open THEN they come in at 0 percent as new`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = percent(75L, 25L)))

            // When
            groups.groups.value =
                listOf(
                    home.copy(
                        defaultSplit = percent(75L, 25L),
                        members = home.members + member(CARLA, "Carla"),
                    ),
                )

            // Then
            val carla = viewModel.uiState.value.people
                .last()
            assertTrue(carla.isNew)
            assertEquals(0L, viewModel.uiState.value.weights[CARLA])
            assertFalse(viewModel.uiState.value.showsPercentError)
        }

    @Test
    fun `GIVEN Igual WHEN leaving Ana out and saving THEN Ana stays out of the default`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(DefaultSplitUiEvent.OnPersonToggled(ANA))

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnSaveClicked)

        // Then
        assertEquals(
            GroupDefaultSplit(DefaultSplitMode.EQUAL, mapOf(PIERRE to 1L, ANA to 0L), emptyList()),
            groups.savedDefaultSplits.single().second,
        )
    }

    @Test
    fun `GIVEN a group splitting 3 to 1 WHEN going back to Igual and saving THEN the group has no default anymore`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            viewModel.onEvent(DefaultSplitUiEvent.OnModeSelected(DefaultSplitMode.EQUAL))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnSaveClicked)

            // Then
            assertNull(groups.savedDefaultSplits.single().second)
            assertNull(
                groups.groups.value
                    .single()
                    .defaultSplit,
            )
        }

    @Test
    fun `GIVEN Partes WHEN switching to Porcentagem THEN 100 is shared evenly among who was in`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnModeSelected(DefaultSplitMode.PERCENT))

            // Then
            assertEquals(
                listOf("50", "50"),
                viewModel.uiState.value.people
                    .map { it.percentText },
            )
            assertTrue(viewModel.uiState.value.canSave)
        }

    @Test
    fun `GIVEN one part WHEN taking it away and adding two THEN the parts never go under 0`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))
            viewModel.onEvent(DefaultSplitUiEvent.OnPartsDecreased(ANA))
            viewModel.onEvent(DefaultSplitUiEvent.OnPartsDecreased(ANA))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnPartsIncreased(ANA))

            // Then
            assertEquals(1L, viewModel.uiState.value.weights[ANA])
            assertFalse(viewModel.uiState.value.hasChanges)
        }

    @Test
    fun `GIVEN the device cannot save WHEN tapping Salvar THEN it says so and stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        groups.defaultSplitError = IllegalStateException("disk full")
        viewModel.onEvent(DefaultSplitUiEvent.OnPersonToggled(ANA))

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnSaveClicked)

        // Then
        assertTrue(viewModel.uiState.value.showsSaveError)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(defaultSplit, navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN a save in flight WHEN tapping back THEN it stays until the save ends`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        groups.defaultSplitGate = CompletableDeferred()
        viewModel.onEvent(DefaultSplitUiEvent.OnPersonToggled(ANA))
        viewModel.onEvent(DefaultSplitUiEvent.OnSaveClicked)

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnBackClicked)

        // Then
        assertEquals(defaultSplit, navigator.backStack.value.last())
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN the screen open WHEN tapping back THEN Editar grupo is on top with nothing saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(DefaultSplitUiEvent.OnPersonToggled(ANA))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnBackClicked)

            // Then
            assertEquals(editGroup, navigator.backStack.value.last())
            assertTrue(groups.savedDefaultSplits.isEmpty())
        }

    @Test
    fun `GIVEN the group deleted elsewhere WHEN a sync takes it off THEN the screen closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            groups.groups.value = emptyList()

            // Then
            assertEquals(editGroup, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN no internet WHEN reading the state THEN it says the split holds on this device for now`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.showsOfflineNotice)
        }

    private val editGroup = Route.EditGroup(GROUP_ID)

    private val defaultSplit = Route.DefaultSplit(GROUP_ID)

    private val threeToOne = GroupDefaultSplit(
        mode = DefaultSplitMode.SHARES,
        weights = mapOf(PIERRE to 3L, ANA to 1L),
        exceptions = emptyList(),
    )

    private fun percent(
        pierre: Long,
        ana: Long,
    ): GroupDefaultSplit = GroupDefaultSplit(
        mode = DefaultSplitMode.PERCENT,
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

    @Test
    fun `GIVEN Partes 3 to 1 WHEN adding Mercado split equally and saving THEN the group keeps the exception`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnAddExceptionClicked)
            val opened = viewModel.uiState.value
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionCategoryPicked(AppCategory.FOOD.key))
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionCategoryPicked(AppCategory.FOOD_MARKET.key))
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionSaveClicked)
            val added = viewModel.uiState.value
            viewModel.onEvent(DefaultSplitUiEvent.OnSaveClicked)

            // Then
            assertFalse(opened.canSaveException)
            assertEquals(
                listOf(
                    CategorySplit(AppCategory.FOOD_MARKET.key, DefaultSplitMode.EQUAL, mapOf(PIERRE to 1L, ANA to 1L)),
                ),
                added.exceptions,
            )
            assertTrue(added.hasChanges)
            assertEquals(
                AppCategory.FOOD_MARKET.key,
                added.exceptionRows
                    .single()
                    .category.id,
            )
            assertEquals(
                added.exceptions,
                groups.savedDefaultSplits
                    .single()
                    .second
                    ?.exceptions,
            )
        }

    @Test
    fun `GIVEN an exception WHEN changing it to percentages that do not add up THEN it waits until they do`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne.copy(exceptions = listOf(market))))
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionClicked(AppCategory.FOOD_MARKET.key))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionModeSelected(DefaultSplitMode.PERCENT))
            val even = viewModel.uiState.value
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionPercentChanged(PIERRE, "40"))
            val short = viewModel.uiState.value

            // Then
            assertEquals(mapOf(PIERRE to 50L, ANA to 50L), even.exceptionEditor?.figures?.weights)
            assertTrue(even.canSaveException)
            assertEquals(10L, short.exceptionEditor?.remainingPercent)
            assertFalse(short.canSaveException)
        }

    @Test
    fun `GIVEN an exception for Mercado WHEN adding another for Mercado THEN it is taken and removing empties it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne.copy(exceptions = listOf(market))))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnAddExceptionClicked)
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionCategoryPicked(AppCategory.FOOD_MARKET.key))
            val taken = viewModel.uiState.value
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionDismissed)
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionClicked(AppCategory.FOOD_MARKET.key))
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionRemoveClicked)

            // Then
            assertTrue(taken.isExceptionCategoryTaken)
            assertFalse(taken.canSaveException)
            val state = viewModel.uiState.value
            assertTrue(state.exceptions.isEmpty())
            assertTrue(state.hasChanges)
            assertNull(state.exceptionEditor)
        }

    @Test
    fun `GIVEN the screen open WHEN tapping back THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnBackClicked)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Igual WHEN picking Partes THEN sends the mode picked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnModeSelected(DefaultSplitMode.SHARES))

        // Then
        assertEquals(
            listOf("default_split_mode_selected" to mapOf<String, Any>("mode" to DefaultSplitMode.SHARES)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN Igual WHEN leaving Ana out THEN sends the person toggled without who`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnPersonToggled(ANA))

        // Then
        assertEquals(listOf("default_split_person_toggled" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN Partes WHEN adding a part to Ana THEN sends the parts changed up without who`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnPartsIncreased(ANA))

            // Then
            assertEquals(
                listOf("default_split_parts_changed" to mapOf<String, Any>("direction" to "up")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Partes WHEN taking a part from Pierre THEN sends the parts changed down without who`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnPartsDecreased(PIERRE))

            // Then
            assertEquals(
                listOf("default_split_parts_changed" to mapOf<String, Any>("direction" to "down")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Porcentagem WHEN typing a percentage THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = home.copy(defaultSplit = percent(75L, 25L)))

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnPercentChanged(ANA, "15"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Pela renda WHEN typing an income THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(DefaultSplitUiEvent.OnModeSelected(DefaultSplitMode.INCOME))
        trackedEvents.clear()

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnIncomeChanged(PIERRE, "9.000,00"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Ana left out WHEN tapping Salvar THEN sends the save clicked event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(DefaultSplitUiEvent.OnPersonToggled(ANA))
        trackedEvents.clear()

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnSaveClicked)

        // Then
        assertEquals(listOf("default_split_save_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the screen open WHEN tapping Adicionar excecao THEN sends the exception add clicked event`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnAddExceptionClicked)

            // Then
            assertEquals(listOf("default_split_exception_add_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN an exception for Mercado WHEN opening it THEN sends the exception clicked without its category`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne.copy(exceptions = listOf(market))))

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionClicked(AppCategory.FOOD_MARKET.key))

            // Then
            assertEquals(listOf("default_split_exception_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN a new exception WHEN picking Mercado THEN sends the category picked without the category`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(DefaultSplitUiEvent.OnAddExceptionClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionCategoryPicked(AppCategory.FOOD_MARKET.key))

            // Then
            assertEquals(
                listOf("default_split_exception_category_picked" to emptyMap<String, Any>()),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the exception for Mercado open WHEN picking Porcentagem THEN sends the exception mode picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne.copy(exceptions = listOf(market))))
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionClicked(AppCategory.FOOD_MARKET.key))
            trackedEvents.clear()

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionModeSelected(DefaultSplitMode.PERCENT))

            // Then
            assertEquals(
                listOf(
                    "default_split_exception_mode_selected" to mapOf<String, Any>("mode" to DefaultSplitMode.PERCENT),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the Mercado exception open WHEN leaving Ana out THEN sends the exception person toggled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne.copy(exceptions = listOf(market))))
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionClicked(AppCategory.FOOD_MARKET.key))
            trackedEvents.clear()

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionPersonToggled(ANA))

            // Then
            assertEquals(
                listOf("default_split_exception_person_toggled" to emptyMap<String, Any>()),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the exception in Partes WHEN adding a part to Ana THEN sends the exception parts changed up`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne.copy(exceptions = listOf(market))))
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionClicked(AppCategory.FOOD_MARKET.key))
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionModeSelected(DefaultSplitMode.SHARES))
            trackedEvents.clear()

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionPartsIncreased(ANA))

            // Then
            assertEquals(
                listOf("default_split_exception_parts_changed" to mapOf<String, Any>("direction" to "up")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the exception in Partes WHEN taking a part from Pierre THEN sends the exception parts changed down`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne.copy(exceptions = listOf(market))))
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionClicked(AppCategory.FOOD_MARKET.key))
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionModeSelected(DefaultSplitMode.SHARES))
            trackedEvents.clear()

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionPartsDecreased(PIERRE))

            // Then
            assertEquals(
                listOf("default_split_exception_parts_changed" to mapOf<String, Any>("direction" to "down")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the exception in Porcentagem WHEN typing a percentage THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne.copy(exceptions = listOf(market))))
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionClicked(AppCategory.FOOD_MARKET.key))
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionModeSelected(DefaultSplitMode.PERCENT))
        trackedEvents.clear()

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionPercentChanged(PIERRE, "40"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a new exception for Mercado WHEN tapping Salvar excecao THEN sends the exception save clicked event`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(DefaultSplitUiEvent.OnAddExceptionClicked)
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionCategoryPicked(AppCategory.FOOD_MARKET.key))
            trackedEvents.clear()

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionSaveClicked)

            // Then
            assertEquals(listOf("default_split_exception_save_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN the exception for Mercado open WHEN removing it THEN sends the exception remove clicked event`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = home.copy(defaultSplit = threeToOne.copy(exceptions = listOf(market))))
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionClicked(AppCategory.FOOD_MARKET.key))
            trackedEvents.clear()

            // When
            viewModel.onEvent(DefaultSplitUiEvent.OnExceptionRemoveClicked)

            // Then
            assertEquals(listOf("default_split_exception_remove_clicked" to emptyMap<String, Any>()), trackedEvents)
        }

    @Test
    fun `GIVEN a new exception open WHEN dismissing it THEN sends nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(DefaultSplitUiEvent.OnAddExceptionClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(DefaultSplitUiEvent.OnExceptionDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    private val market = CategorySplit(
        AppCategory.FOOD_MARKET.key,
        DefaultSplitMode.EQUAL,
        mapOf(
            PIERRE to 1L,
            ANA to 1L,
        ),
    )

    private val bitcoinInSats = MutableStateFlow(false)

    private fun prepareScenario(
        storedGroup: Group = home,
        storedIncomes: Map<String, Long> = emptyMap(),
        online: Boolean = true,
        showsSats: Boolean = false,
    ) {
        bitcoinInSats.value = showsSats
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup)).also {
            if (storedIncomes.isNotEmpty()) it.savedIncomes[GROUP_ID] = storedIncomes
        }
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(
                Route.GroupDetail(
                    GROUP_ID,
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            )
            it.navigate(editGroup)
            it.navigate(defaultSplit)
        }
        viewModel = DefaultSplitViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            route = defaultSplit,
            navigator = navigator,
            updateDefaultSplit = UpdateDefaultSplitUseCase(groups) { 1_000L },
            getSplitIncomes = GetSplitIncomesUseCase(groups),
            decimalSeparator = { ',' },
            observeGroup = ObserveGroupUseCase(groups),
            observeCategoryTree = ObserveCategoryTreeUseCase(FakeCategoryRepository()),
            observeBitcoinInSats = { bitcoinInSats },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(online) },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val PIERRE = "member-1"
        const val ANA = "member-2"
        const val CARLA = "member-3"
    }
}
