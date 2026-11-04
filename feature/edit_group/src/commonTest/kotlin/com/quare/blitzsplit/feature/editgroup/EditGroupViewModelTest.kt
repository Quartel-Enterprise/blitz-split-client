package com.quare.blitzsplit.feature.editgroup

import com.quare.blitzsplit.core.expenses.domain.model.DebtSimplificationSummary
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SimplifyDebtsConflictNotice
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettlementsUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SetSimplifiesDebtsUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.SimplifyDebtsChange
import com.quare.blitzsplit.core.groups.domain.model.SimplifyDebtsPair
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ConsumeSimplifyDebtsConflictUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.UpdateGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.CropPhoto
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.GroupIconEditor
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyAmount
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyDebtsStatus
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyName
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyQueuedPayment
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.EditGroupViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditGroupViewModelTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var settlements: FakeSettlementRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: EditGroupViewModel
    private lateinit var photoGate: CompletableDeferred<Unit>

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN Room not read yet WHEN the screen opens THEN nothing is loaded and Salvar is disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isRoomRead = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertFalse(state.canSave)
            assertFalse(state.showsNameError)
        }

    @Test
    fun `GIVEN Room not read yet WHEN tapping Salvar THEN saves nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

        // When
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

        // Then
        assertTrue(groups.updatedGroupIds.isEmpty())
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group on the device WHEN the screen opens THEN the form is filled from the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertEquals("Viagem Floripa", state.name)
            assertEquals("VF", state.initials)
            assertEquals(AvatarColor.GREEN, state.color)
            assertEquals(GroupType.TRIP, state.type)
            assertEquals(GroupCurrency.BRL, state.currency)
            assertFalse(state.isSaving)
            assertFalse(state.showsSaveError)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN a name being typed WHEN a sync changes the group THEN the form keeps what was typed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))
            viewModel.onEvent(EditGroupUiEvent.OnTypeSelected(GroupType.EVENT))

            // When
            groups.groups.value = listOf(
                trip.copy(
                    name = "Viagem Floripa 2026",
                    color = AvatarColor.SAND,
                    type = GroupType.HOME,
                ),
            )

            // Then
            val state = viewModel.uiState.value
            assertEquals("Churrasco", state.name)
            assertEquals("C", state.initials)
            assertEquals(AvatarColor.GREEN, state.color)
            assertEquals(GroupType.EVENT, state.type)
        }

    @Test
    fun `GIVEN a group with no bills WHEN a sync changes its currency THEN the form keeps its own`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            groups.groups.value = listOf(trip.copy(currency = GroupCurrency.USD))

            // Then
            assertEquals(GroupCurrency.BRL, viewModel.uiState.value.currency)
            assertFalse(viewModel.uiState.value.isCurrencyLocked)
        }

    @Test
    fun `GIVEN the form WHEN typing a new name THEN the initials follow it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val typed = "Churrasco de sábado"

        // When
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged(typed))

        // Then
        assertEquals("Churrasco de sábado", viewModel.uiState.value.name)
        assertEquals("CS", viewModel.uiState.value.initials)
    }

    @Test
    fun `GIVEN the form WHEN typing a name over 40 letters THEN it is capped at 40`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val typed = "a".repeat(45)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged(typed))

        // Then
        assertEquals("a".repeat(40), viewModel.uiState.value.name)
        assertEquals(40, viewModel.uiState.value.nameLength)
    }

    @Test
    fun `GIVEN the form WHEN clearing the name THEN shows the name error and disables Salvar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val typed = ""

            // When
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged(typed))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsNameError)
            assertFalse(state.canSave)
            assertEquals("", state.initials)
        }

    @Test
    fun `GIVEN the form WHEN typing a name of only spaces THEN shows the name error and disables Salvar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val typed = "   "

            // When
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged(typed))

            // Then
            assertTrue(viewModel.uiState.value.showsNameError)
            assertFalse(viewModel.uiState.value.canSave)
        }

    @Test
    fun `GIVEN a name of only spaces WHEN tapping Salvar THEN saves nothing and stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged("   "))

        // When
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

        // Then
        assertTrue(groups.updatedGroupIds.isEmpty())
        assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a green group WHEN picking lilac in the form THEN the form shows lilac`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))

        // Then
        assertEquals(AvatarColor.LILAC, viewModel.uiState.value.color)
    }

    @Test
    fun `GIVEN a trip WHEN picking the home type THEN the form shows home`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val home = GroupType.HOME

        // When
        viewModel.onEvent(EditGroupUiEvent.OnTypeSelected(home))

        // Then
        assertEquals(GroupType.HOME, viewModel.uiState.value.type)
    }

    @Test
    fun `GIVEN a group with no bills WHEN picking another currency THEN the form shows it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val euro = GroupCurrency.EUR

        // When
        viewModel.onEvent(EditGroupUiEvent.OnCurrencySelected(euro))

        // Then
        assertFalse(viewModel.uiState.value.isCurrencyLocked)
        assertEquals(GroupCurrency.EUR, viewModel.uiState.value.currency)
    }

    @Test
    fun `GIVEN a group with a bill WHEN the screen opens THEN the currency is locked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(dinner))

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isCurrencyLocked)
        assertEquals(GroupCurrency.BRL, state.currency)
    }

    @Test
    fun `GIVEN a group with a bill WHEN picking another currency THEN the currency stays the group one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            val euro = GroupCurrency.EUR

            // When
            viewModel.onEvent(EditGroupUiEvent.OnCurrencySelected(euro))

            // Then
            assertEquals(GroupCurrency.BRL, viewModel.uiState.value.currency)
        }

    @Test
    fun `GIVEN another currency picked WHEN the first bill arrives THEN locks it to the group one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // When
            expenses.expenses.value = listOf(dinner)

            // Then
            assertTrue(viewModel.uiState.value.isCurrencyLocked)
            assertEquals(GroupCurrency.BRL, viewModel.uiState.value.currency)
        }

    @Test
    fun `GIVEN a locked currency WHEN a sync changes the group currency THEN the form shows the new one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))

            // When
            groups.groups.value = listOf(trip.copy(currency = GroupCurrency.USD))

            // Then
            assertTrue(viewModel.uiState.value.isCurrencyLocked)
            assertEquals(GroupCurrency.USD, viewModel.uiState.value.currency)
        }

    @Test
    fun `GIVEN a locked currency WHEN the group bills are gone THEN the currency can change again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))

            // When
            expenses.expenses.value = emptyList()

            // Then
            assertFalse(viewModel.uiState.value.isCurrencyLocked)
        }

    @Test
    fun `GIVEN a locked currency a sync changed WHEN the group bills are gone THEN Salvar stays disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            groups.groups.value = listOf(trip.copy(currency = GroupCurrency.USD))

            // When
            expenses.expenses.value = emptyList()

            // Then
            assertEquals(GroupCurrency.USD, viewModel.uiState.value.currency)
            assertFalse(viewModel.uiState.value.canSave)
        }

    @Test
    fun `GIVEN a bill of another group WHEN the screen opens THEN the currency is not locked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner.copy(groupId = "group-2")))

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isCurrencyLocked)
        }

    @Test
    fun `GIVEN no internet WHEN the screen opens THEN shows the offline notice`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)
        viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsOfflineNotice)
        assertTrue(state.canSave)
    }

    @Test
    fun `GIVEN no internet WHEN it comes back THEN hides the offline notice`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        isOnline.value = true

        // Then
        assertFalse(viewModel.uiState.value.showsOfflineNotice)
    }

    @Test
    fun `GIVEN an edited form WHEN tapping Salvar THEN saves the group and goes back to it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))
            viewModel.onEvent(EditGroupUiEvent.OnTypeSelected(GroupType.EVENT))
            viewModel.onEvent(EditGroupUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            assertEquals(listOf(GROUP_ID), groups.updatedGroupIds)
            val saved = groups.groups.value.single()
            assertEquals("Churrasco", saved.name)
            assertEquals(AvatarColor.LILAC, saved.color)
            assertEquals(GroupType.EVENT, saved.type)
            assertEquals(GroupCurrency.EUR, saved.currency)
            assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no internet WHEN tapping Salvar THEN saves on the device and goes back to the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            assertEquals(listOf(GROUP_ID), groups.updatedGroupIds)
            assertEquals(
                "Churrasco",
                groups.groups.value
                    .single()
                    .name,
            )
            assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a group with a bill WHEN tapping Salvar THEN saves the group own currency`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedExpenses = listOf(dinner))
        viewModel.onEvent(EditGroupUiEvent.OnCurrencySelected(GroupCurrency.EUR))

        // When
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

        // Then
        assertEquals(
            GroupCurrency.BRL,
            groups.groups.value
                .single()
                .currency,
        )
    }

    @Test
    fun `GIVEN the device fails to save WHEN tapping Salvar THEN stays and shows the save error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateError = IllegalStateException("disk full"))
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsSaveError)
            assertFalse(state.isSaving)
            assertTrue(state.canSave)
            assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a failed save WHEN tapping Salvar again THEN clears the save error while saving`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(updateError = IllegalStateException("disk full"))
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)
            groups.updateGroupError = null
            groups.updateGroupGate = CompletableDeferred()

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            assertFalse(viewModel.uiState.value.showsSaveError)
            assertTrue(viewModel.uiState.value.isSaving)
        }

    @Test
    fun `GIVEN a save in flight WHEN reading the form THEN Salvar is disabled while it saves`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isSaveHeld = true)
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isSaving)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN a double tap on Salvar WHEN the save ends THEN saves only once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isSaveHeld = true)
        viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

        // When
        groups.updateGroupGate?.complete(Unit)

        // Then
        assertEquals(listOf(GROUP_ID), groups.updatedGroupIds)
        assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the form WHEN tapping Cancelar THEN goes back to the group`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

        // When
        viewModel.onEvent(EditGroupUiEvent.OnCloseClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        assertTrue(groups.updatedGroupIds.isEmpty())
        assertEquals(
            "Viagem Floripa",
            groups.groups.value
                .single()
                .name,
        )
    }

    @Test
    fun `GIVEN a save in flight WHEN tapping Cancelar THEN stays until the save ends`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isSaveHeld = true)
        viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnCloseClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen over the form WHEN tapping Cancelar THEN pops nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Profile)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnCloseClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupDetail, editGroup, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a save in flight WHEN a native back closed the form before it ends THEN the group stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isSaveHeld = true)
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)
            navigator.popTo(2)

            // When
            groups.updateGroupGate?.complete(Unit)

            // Then
            assertEquals(listOf(GROUP_ID), groups.updatedGroupIds)
            assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a save in flight and another screen over the form WHEN the save ends THEN that screen stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isSaveHeld = true)
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)
            navigator.navigate(Route.Profile)

            // When
            groups.updateGroupGate?.complete(Unit)

            // Then
            assertEquals(listOf(Route.Groups, groupDetail, editGroup, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the form on top WHEN the group is gone from the device THEN leaves on its own`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            groups.groups.value = emptyList()

            // Then
            assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a group not on the device WHEN the screen opens THEN leaves on its own`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = trip.copy(id = "group-2"))

        // When
        val backStack = navigator.backStack.value

        // Then
        assertEquals(listOf(Route.Groups, groupDetail), backStack)
    }

    @Test
    fun `GIVEN another screen over the form WHEN the group is gone THEN pops nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Profile)

        // When
        groups.groups.value = emptyList()

        // Then
        assertEquals(listOf(Route.Groups, groupDetail, editGroup, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the group gone under another screen WHEN that screen closes THEN the form leaves too`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Profile)
            groups.groups.value = emptyList()

            // When
            navigator.navigateBack()

            // Then
            assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the group gone and back under another screen WHEN that screen closes THEN the form stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Profile)
            groups.groups.value = emptyList()
            groups.groups.value = listOf(trip)

            // When
            navigator.navigateBack()

            // Then
            assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a sync renamed the group WHEN saving only a new color THEN saves the synced name and new color`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.groups.value = listOf(trip.copy(name = "Floripa 2027"))
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            val saved = groups.groups.value.single()
            assertEquals("Floripa 2027", saved.name)
            assertEquals(AvatarColor.LILAC, saved.color)
        }

    @Test
    fun `GIVEN a sync renamed and recolored the group WHEN saving a new name THEN keeps it and the synced color`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.groups.value = listOf(
                trip.copy(
                    name = "Floripa 2027",
                    color = AvatarColor.SAND,
                ),
            )
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            val saved = groups.groups.value.single()
            assertEquals("Churrasco", saved.name)
            assertEquals(AvatarColor.SAND, saved.color)
        }

    @Test
    fun `GIVEN a sync recolored the group WHEN saving a color picked here THEN the color picked here wins`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.groups.value = listOf(trip.copy(color = AvatarColor.SAND))
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                AvatarColor.LILAC,
                groups.groups.value
                    .single()
                    .color,
            )
        }

    @Test
    fun `GIVEN a sync changed the group type WHEN saving only a new color THEN saves the synced type`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.groups.value = listOf(trip.copy(type = GroupType.HOME))
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                GroupType.HOME,
                groups.groups.value
                    .single()
                    .type,
            )
        }

    @Test
    fun `GIVEN a sync changed the group type WHEN saving another type picked here THEN the type picked here wins`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.groups.value = listOf(trip.copy(type = GroupType.HOME))
            viewModel.onEvent(EditGroupUiEvent.OnTypeSelected(GroupType.EVENT))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                GroupType.EVENT,
                groups.groups.value
                    .single()
                    .type,
            )
        }

    @Test
    fun `GIVEN a sync changed an unlocked currency WHEN saving only a new color THEN saves the synced currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.groups.value = listOf(trip.copy(currency = GroupCurrency.USD))
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                GroupCurrency.USD,
                groups.groups.value
                    .single()
                    .currency,
            )
        }

    @Test
    fun `GIVEN a sync changed an unlocked currency WHEN saving one picked here THEN the one picked here wins`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.groups.value = listOf(trip.copy(currency = GroupCurrency.USD))
            viewModel.onEvent(EditGroupUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                GroupCurrency.EUR,
                groups.groups.value
                    .single()
                    .currency,
            )
        }

    @Test
    fun `GIVEN a sync rename and a name here that only gained spaces WHEN saving THEN saves the synced name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.groups.value = listOf(trip.copy(name = "Floripa 2027"))
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("  Viagem Floripa  "))
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                "Floripa 2027",
                groups.groups.value
                    .single()
                    .name,
            )
        }

    @Test
    fun `GIVEN a locked currency changed by a sync WHEN saving a new color THEN saves the synced currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))
            groups.groups.value = listOf(trip.copy(currency = GroupCurrency.USD))
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                GroupCurrency.USD,
                groups.groups.value
                    .single()
                    .currency,
            )
        }

    @Test
    fun `GIVEN no internet after a sync rename WHEN saving only a new color THEN saves the synced name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.groups.value = listOf(trip.copy(name = "Floripa 2027"))
            isOnline.value = false
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            val saved = groups.groups.value.single()
            assertEquals("Floripa 2027", saved.name)
            assertEquals(AvatarColor.LILAC, saved.color)
            assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        }

    @Test
    fun `GIVEN an untouched form WHEN a sync changes the group THEN Salvar stays disabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        groups.groups.value = listOf(
            trip.copy(
                name = "Floripa 2027",
                color = AvatarColor.SAND,
                type = GroupType.HOME,
                currency = GroupCurrency.USD,
            ),
        )

        // Then
        assertFalse(viewModel.uiState.value.hasChanges)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN the form as it opened WHEN tapping Salvar THEN saves nothing and stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

        // Then
        assertTrue(groups.updatedGroupIds.isEmpty())
        assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the form WHEN typing a new name THEN Salvar is enabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val typed = "Churrasco"

        // When
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged(typed))

        // Then
        assertTrue(viewModel.uiState.value.hasChanges)
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN the form WHEN picking another type THEN Salvar is enabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val event = GroupType.EVENT

        // When
        viewModel.onEvent(EditGroupUiEvent.OnTypeSelected(event))

        // Then
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN a group with no bills WHEN picking another currency THEN Salvar is enabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val euro = GroupCurrency.EUR

        // When
        viewModel.onEvent(EditGroupUiEvent.OnCurrencySelected(euro))

        // Then
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN a new name WHEN typing the name it opened with back THEN Salvar is disabled again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Viagem Floripa"))

            // Then
            assertFalse(viewModel.uiState.value.hasChanges)
            assertFalse(viewModel.uiState.value.canSave)
        }

    @Test
    fun `GIVEN another type picked WHEN picking the type it opened with back THEN Salvar is disabled again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnTypeSelected(GroupType.EVENT))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnTypeSelected(GroupType.TRIP))

            // Then
            assertFalse(viewModel.uiState.value.canSave)
        }

    @Test
    fun `GIVEN another currency picked WHEN picking the currency it opened with back THEN Salvar is disabled again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnCurrencySelected(GroupCurrency.BRL))

            // Then
            assertFalse(viewModel.uiState.value.canSave)
        }

    @Test
    fun `GIVEN another color picked WHEN back at its first color THEN Salvar is disabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))

        // When
        viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.GREEN))

        // Then
        assertEquals(AvatarColor.GREEN, viewModel.uiState.value.color)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN the form WHEN the name only gains surrounding spaces THEN Salvar stays disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val typed = "  Viagem Floripa "

            // When
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged(typed))

            // Then
            assertFalse(viewModel.uiState.value.hasChanges)
            assertFalse(viewModel.uiState.value.canSave)
        }

    @Test
    fun `GIVEN a locked currency WHEN a sync changes the group currency THEN Salvar stays disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))

            // When
            groups.groups.value = listOf(trip.copy(currency = GroupCurrency.USD))

            // Then
            assertEquals(GroupCurrency.USD, viewModel.uiState.value.currency)
            assertFalse(viewModel.uiState.value.canSave)
        }

    @Test
    fun `GIVEN only another currency picked WHEN the first bill arrives THEN Salvar is disabled again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnCurrencySelected(GroupCurrency.EUR))

            // When
            expenses.expenses.value = listOf(dinner)

            // Then
            assertFalse(viewModel.uiState.value.canSave)
        }

    @Test
    fun `GIVEN a save in flight under another screen WHEN the save ends THEN stops saving and disables Salvar`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isSaveHeld = true)
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)
            navigator.navigate(Route.Profile)

            // When
            groups.updateGroupGate?.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertEquals(listOf(GROUP_ID), groups.updatedGroupIds)
            assertFalse(state.isSaving)
            assertFalse(state.showsSaveError)
            assertFalse(state.hasChanges)
            assertFalse(state.canSave)
            assertEquals(listOf(Route.Groups, groupDetail, editGroup, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a save that ended under another screen now closed WHEN tapping Cancelar THEN goes back to the group`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isSaveHeld = true)
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)
            navigator.navigate(Route.Profile)
            groups.updateGroupGate?.complete(Unit)
            navigator.navigateBack()

            // When
            viewModel.onEvent(EditGroupUiEvent.OnCloseClicked)

            // Then
            assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a name saved under another screen WHEN typing the name the group had before THEN Salvar is enabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isSaveHeld = true)
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)
            navigator.navigate(Route.Profile)
            groups.updateGroupGate?.complete(Unit)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Viagem Floripa"))

            // Then
            assertTrue(viewModel.uiState.value.hasChanges)
            assertTrue(viewModel.uiState.value.canSave)
        }

    @Test
    fun `GIVEN a name with spaces saved under another screen WHEN reading the form THEN Salvar is disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isSaveHeld = true)
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged(" Churrasco "))
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)
            navigator.navigate(Route.Profile)
            groups.updateGroupGate?.complete(Unit)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                "Churrasco",
                groups.groups.value
                    .single()
                    .name,
            )
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN a group splitting by parts WHEN the form opens THEN the default split row says Partes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(defaultSplit = threeToOne))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(DefaultSplitMode.SHARES, state.defaultSplitMode)
        }

    @Test
    fun `GIVEN the form open WHEN the default split is saved under it THEN the row follows without a change to save`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            groups.groups.value = listOf(trip.copy(defaultSplit = threeToOne))

            // Then
            assertEquals(DefaultSplitMode.SHARES, viewModel.uiState.value.defaultSplitMode)
            assertFalse(viewModel.uiState.value.hasChanges)
        }

    @Test
    fun `GIVEN the form on top WHEN tapping Divisao padrao THEN it opens over the form`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(EditGroupUiEvent.OnDefaultSplitClicked)

        // Then
        assertEquals(Route.DefaultSplit(GROUP_ID), navigator.backStack.value.last())
        assertEquals(editGroup, navigator.backStack.value[navigator.backStack.value.size - 2])
    }

    @Test
    fun `GIVEN Divisao padrao already open WHEN tapping its row again THEN nothing stacks`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnDefaultSplitClicked)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnDefaultSplitClicked)

        // Then
        assertEquals(1, navigator.backStack.value.count { it is Route.DefaultSplit })
    }

    @Test
    fun `GIVEN a save in flight WHEN tapping Divisao padrao THEN it stays on the form`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isSaveHeld = true)
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnDefaultSplitClicked)

        // Then
        assertEquals(editGroup, navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN the form WHEN tapping the icon THEN Icone do grupo opens on the group color and shape`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(EditGroupUiEvent.OnIconClicked)

            // Then
            assertEquals(
                GroupIconEditor(
                    color = AvatarColor.GREEN,
                    shape = GroupShape.SQUARE,
                ),
                viewModel.uiState.value.iconEditor,
            )
        }

    @Test
    fun `GIVEN Room not read yet WHEN tapping the icon THEN Icone do grupo stays closed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnIconClicked)

        // Then
        assertNull(viewModel.uiState.value.iconEditor)
    }

    @Test
    fun `GIVEN a save in flight WHEN tapping the icon THEN Icone do grupo stays closed`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isSaveHeld = true)
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnIconClicked)

        // Then
        assertNull(viewModel.uiState.value.iconEditor)
    }

    @Test
    fun `GIVEN Icone do grupo open WHEN picking a color THEN only the editor changes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnIconClicked)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnIconColorPicked(AvatarColor.LILAC))

        // Then
        val state = viewModel.uiState.value
        assertEquals(AvatarColor.LILAC, state.iconEditor?.color)
        assertEquals(AvatarColor.GREEN, state.color)
        assertFalse(state.canSave)
    }

    @Test
    fun `GIVEN Icone do grupo open WHEN picking a shape THEN only the editor changes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnIconClicked)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnIconShapePicked(GroupShape.PETAL))

        // Then
        val state = viewModel.uiState.value
        assertEquals(GroupShape.PETAL, state.iconEditor?.shape)
        assertEquals(GroupShape.SQUARE, state.shape)
        assertFalse(state.canSave)
    }

    @Test
    fun `GIVEN a color and shape picked in Icone do grupo WHEN tapping Usar este icone THEN the form takes them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnIconClicked)
            viewModel.onEvent(EditGroupUiEvent.OnIconColorPicked(AvatarColor.LILAC))
            viewModel.onEvent(EditGroupUiEvent.OnIconShapePicked(GroupShape.PETAL))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnIconConfirmed)

            // Then
            val state = viewModel.uiState.value
            assertEquals(AvatarColor.LILAC, state.color)
            assertEquals(GroupShape.PETAL, state.shape)
            assertNull(state.iconEditor)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a group with a photo WHEN tapping Usar este icone THEN the photo is taken off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(photo = remotePhoto))
            viewModel.onEvent(EditGroupUiEvent.OnIconClicked)
            viewModel.onEvent(EditGroupUiEvent.OnIconShapePicked(GroupShape.CIRCLE))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnIconConfirmed)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photo)
            assertNull(state.photoUrl)
            assertEquals(GroupShape.CIRCLE, state.shape)
            assertEquals(AvatarColor.GREEN, state.selectedColor)
        }

    @Test
    fun `GIVEN a group with a photo WHEN only taking the photo off THEN Salvar is enabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = trip.copy(photo = remotePhoto))
        viewModel.onEvent(EditGroupUiEvent.OnIconClicked)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnIconConfirmed)

        // Then
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN a color and shape picked in Icone do grupo WHEN dismissing it THEN the form keeps its icon`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(photo = remotePhoto))
            viewModel.onEvent(EditGroupUiEvent.OnIconClicked)
            viewModel.onEvent(EditGroupUiEvent.OnIconColorPicked(AvatarColor.LILAC))
            viewModel.onEvent(EditGroupUiEvent.OnIconShapePicked(GroupShape.PETAL))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnIconDismissed)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.iconEditor)
            assertEquals(AvatarColor.GREEN, state.color)
            assertEquals(GroupShape.SQUARE, state.shape)
            assertEquals(remotePhoto, state.photo)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN Icone do grupo dismissed WHEN opening it again THEN it opens on the form icon`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnIconClicked)
            viewModel.onEvent(EditGroupUiEvent.OnIconColorPicked(AvatarColor.LILAC))
            viewModel.onEvent(EditGroupUiEvent.OnIconDismissed)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnIconClicked)

            // Then
            assertEquals(
                AvatarColor.GREEN,
                viewModel.uiState.value.iconEditor
                    ?.color,
            )
        }

    @Test
    fun `GIVEN the form WHEN only the shape changes THEN Salvar is enabled`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnIconClicked)
        viewModel.onEvent(EditGroupUiEvent.OnIconShapePicked(GroupShape.COOKIE))

        // When
        viewModel.onEvent(EditGroupUiEvent.OnIconConfirmed)

        // Then
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN another shape used WHEN using its first shape back THEN Salvar is disabled again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnIconClicked)
            viewModel.onEvent(EditGroupUiEvent.OnIconShapePicked(GroupShape.COOKIE))
            viewModel.onEvent(EditGroupUiEvent.OnIconConfirmed)
            viewModel.onEvent(EditGroupUiEvent.OnIconClicked)
            viewModel.onEvent(EditGroupUiEvent.OnIconShapePicked(GroupShape.SQUARE))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnIconConfirmed)

            // Then
            assertFalse(viewModel.uiState.value.canSave)
        }

    @Test
    fun `GIVEN a group with a photo WHEN the form opens THEN no color is marked and the photo url is its url`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(photo = remotePhoto))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(remotePhoto, state.photo)
            assertEquals(remotePhoto.url, state.photoUrl)
            assertNull(state.selectedColor)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN a group with a photo WHEN picking a color in the form THEN takes the photo off and keeps the shape`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(photo = remotePhoto))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.SAND))

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photo)
            assertEquals(AvatarColor.SAND, state.selectedColor)
            assertEquals(GroupShape.SQUARE, state.shape)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a group with a photo WHEN picking its own color in the form THEN Salvar is enabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(photo = remotePhoto))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.GREEN))

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photo)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a save in flight WHEN picking a color in the form THEN the form keeps its icon`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(photo = remotePhoto),
                isSaveHeld = true,
            )
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.SAND))

            // Then
            val state = viewModel.uiState.value
            assertEquals(AvatarColor.GREEN, state.color)
            assertEquals(remotePhoto, state.photo)
        }

    @Test
    fun `GIVEN a photo picked WHEN it is still being prepared THEN shows it preparing and Salvar is disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isPhotoHeld = true)
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

            // When
            pickAndConfirmPhoto(pickedFile)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isPreparingPhoto)
            assertFalse(state.canSave)
            assertNull(state.photo)
        }

    @Test
    fun `GIVEN a photo being prepared WHEN tapping Salvar THEN saves nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isPhotoHeld = true)
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))
        pickAndConfirmPhoto(pickedFile)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

        // Then
        assertTrue(groups.updatedGroupIds.isEmpty())
        assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Icone do grupo open WHEN a picked photo is prepared THEN the form shows it and the editor closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnIconClicked)

            // When
            pickAndConfirmPhoto(pickedFile)

            // Then
            val state = viewModel.uiState.value
            assertEquals(preparedPhoto, state.photo)
            assertEquals(preparedPhoto.dataUri, state.photoUrl)
            assertNull(state.selectedColor)
            assertNull(state.iconEditor)
            assertFalse(state.isPreparingPhoto)
            assertFalse(state.showsPhotoError)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a photo error WHEN a picked photo is prepared THEN hides the error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnPhotoUnreadable)

        // When
        pickAndConfirmPhoto(pickedFile)

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showsPhotoError)
        assertEquals(preparedPhoto, state.photo)
    }

    @Test
    fun `GIVEN a group with a photo WHEN the picked photo fails to prepare THEN keeps its photo and shows the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(photo = remotePhoto),
                photoError = IllegalArgumentException("not an image"),
            )

            // When
            pickAndConfirmPhoto(pickedFile)

            // Then
            val state = viewModel.uiState.value
            assertEquals(remotePhoto, state.photo)
            assertTrue(state.showsPhotoError)
            assertFalse(state.isPreparingPhoto)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN Icone do grupo open WHEN the picked file is unreadable THEN shows the photo error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnIconClicked)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnPhotoUnreadable)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsPhotoError)
            assertNull(state.photo)
            assertNull(state.iconEditor)
        }

    @Test
    fun `GIVEN a photo being prepared WHEN the next picked file is unreadable THEN the late photo never shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isPhotoHeld = true)
            pickAndConfirmPhoto(pickedFile)
            viewModel.onEvent(EditGroupUiEvent.OnPhotoUnreadable)

            // When
            photoGate.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photo)
            assertTrue(state.showsPhotoError)
            assertFalse(state.isPreparingPhoto)
        }

    @Test
    fun `GIVEN a photo error WHEN picking a color in the form THEN hides the error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnPhotoUnreadable)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))

        // Then
        assertFalse(viewModel.uiState.value.showsPhotoError)
    }

    @Test
    fun `GIVEN a photo being prepared WHEN picking a color in the form THEN the late photo never shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isPhotoHeld = true)
            pickAndConfirmPhoto(pickedFile)
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))

            // When
            photoGate.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photo)
            assertEquals(AvatarColor.LILAC, state.selectedColor)
            assertFalse(state.isPreparingPhoto)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a photo being prepared WHEN using a shape from Icone do grupo THEN the late photo never shows`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isPhotoHeld = true)
            pickAndConfirmPhoto(pickedFile)
            viewModel.onEvent(EditGroupUiEvent.OnIconClicked)
            viewModel.onEvent(EditGroupUiEvent.OnIconShapePicked(GroupShape.PETAL))
            viewModel.onEvent(EditGroupUiEvent.OnIconConfirmed)

            // When
            photoGate.complete(Unit)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.photo)
            assertEquals(GroupShape.PETAL, state.shape)
            assertFalse(state.isPreparingPhoto)
        }

    @Test
    fun `GIVEN a save in flight WHEN picking a photo THEN keeps the photo shown`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroup = trip.copy(photo = remotePhoto),
            isSaveHeld = true,
        )
        viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

        // When
        pickAndConfirmPhoto(pickedFile)

        // Then
        val state = viewModel.uiState.value
        assertEquals(remotePhoto, state.photo)
        assertFalse(state.isPreparingPhoto)
    }

    @Test
    fun `GIVEN no internet and a photo picked WHEN tapping Salvar THEN saves the prepared photo on the device`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)
            pickAndConfirmPhoto(pickedFile)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                preparedPhoto,
                groups.groups.value
                    .single()
                    .photo,
            )
            assertEquals(listOf(Route.Groups, groupDetail), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a shape used here WHEN tapping Salvar THEN saves the shape`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnIconClicked)
        viewModel.onEvent(EditGroupUiEvent.OnIconShapePicked(GroupShape.CIRCLE))
        viewModel.onEvent(EditGroupUiEvent.OnIconConfirmed)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

        // Then
        assertEquals(
            GroupShape.CIRCLE,
            groups.groups.value
                .single()
                .shape,
        )
    }

    @Test
    fun `GIVEN a sync changed the shape WHEN saving only a new name THEN saves the synced shape`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.groups.value = listOf(trip.copy(shape = GroupShape.PETAL))
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            val saved = groups.groups.value.single()
            assertEquals("Churrasco", saved.name)
            assertEquals(GroupShape.PETAL, saved.shape)
        }

    @Test
    fun `GIVEN a sync changed the shape WHEN saving a shape used here THEN the shape used here wins`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.groups.value = listOf(trip.copy(shape = GroupShape.PETAL))
            viewModel.onEvent(EditGroupUiEvent.OnIconClicked)
            viewModel.onEvent(EditGroupUiEvent.OnIconShapePicked(GroupShape.CIRCLE))
            viewModel.onEvent(EditGroupUiEvent.OnIconConfirmed)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                GroupShape.CIRCLE,
                groups.groups.value
                    .single()
                    .shape,
            )
        }

    @Test
    fun `GIVEN a sync added a photo WHEN saving only a new name THEN saves the synced photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            groups.groups.value = listOf(trip.copy(photo = remotePhoto))
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            val saved = groups.groups.value.single()
            assertEquals("Churrasco", saved.name)
            assertEquals(remotePhoto, saved.photo)
        }

    @Test
    fun `GIVEN a sync changed the photo WHEN saving a photo picked here THEN the photo picked here wins`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(photo = remotePhoto))
            groups.groups.value = listOf(trip.copy(photo = otherRemotePhoto))
            pickAndConfirmPhoto(pickedFile)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            assertEquals(
                preparedPhoto,
                groups.groups.value
                    .single()
                    .photo,
            )
        }

    @Test
    fun `GIVEN a sync changed the photo WHEN saving with the photo taken off here THEN saves no photo`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(photo = remotePhoto))
            groups.groups.value = listOf(trip.copy(photo = otherRemotePhoto))
            viewModel.onEvent(EditGroupUiEvent.OnColorSelected(AvatarColor.LILAC))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            val saved = groups.groups.value.single()
            assertNull(saved.photo)
            assertEquals(AvatarColor.LILAC, saved.color)
        }

    @Test
    fun `GIVEN a sync changed the photo WHEN saving only a new shape THEN saves the new shape`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(photo = remotePhoto))
            groups.groups.value = listOf(trip.copy(photo = otherRemotePhoto))
            viewModel.onEvent(EditGroupUiEvent.OnIconClicked)
            viewModel.onEvent(EditGroupUiEvent.OnIconShapePicked(GroupShape.COOKIE))
            viewModel.onEvent(EditGroupUiEvent.OnIconConfirmed)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            val saved = groups.groups.value.single()
            assertEquals(GroupShape.COOKIE, saved.shape)
            assertNull(saved.photo)
        }

    @Test
    fun `GIVEN a group of three simplifying WHEN the screen opens THEN shows the switch on with its payments`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople.copy(simplifiesDebts = true),
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.simplifiesDebts)
            assertEquals(SimplifyDebtsStatus.ON, state.simplifyStatus)
            assertEquals(3, state.memberCount)
            assertEquals(
                DebtSimplificationSummary(
                    paymentCount = 1,
                    unsimplifiedPaymentCount = 2,
                ),
                state.simplification,
            )
        }

    @Test
    fun `GIVEN a group of three not simplifying WHEN the screen opens THEN shows it off with no payments line`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.simplifiesDebts)
            assertEquals(SimplifyDebtsStatus.OFF, state.simplifyStatus)
            assertNull(state.simplification)
            assertTrue(state.canChangeSimplifyDebts)
        }

    @Test
    fun `GIVEN a group of two not simplifying WHEN the screen opens THEN asks for a third person to change it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(2, state.memberCount)
            assertEquals(SimplifyDebtsStatus.NEEDS_THIRD_PERSON, state.simplifyStatus)
            assertFalse(state.canChangeSimplifyDebts)
        }

    @Test
    fun `GIVEN no internet and a switch change not up yet WHEN the screen opens THEN says it is pending and offline`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople.copy(
                    simplifiesDebts = true,
                    isSimplifyDebtsPending = true,
                ),
                online = false,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(SimplifyDebtsStatus.PENDING, state.simplifyStatus)
            assertTrue(state.showsSimplifyOfflineNotice)
        }

    @Test
    fun `GIVEN the switch on WHEN a new bill arrives THEN the payments line follows it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroup = threePeople.copy(simplifiesDebts = true),
            storedExpenses = listOf(pierrePaidForBruno),
        )

        // When
        expenses.expenses.value = listOf(pierrePaidForBruno, brunoPaidForAna)

        // Then
        assertEquals(
            DebtSimplificationSummary(
                paymentCount = 1,
                unsimplifiedPaymentCount = 2,
            ),
            viewModel.uiState.value.simplification,
        )
    }

    @Test
    fun `GIVEN the switch on WHEN a payment is recorded THEN the payments line follows it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroup = threePeople.copy(simplifiesDebts = true),
            storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
        )

        // When
        settlements.settlements.value = listOf(
            payment(
                from = ANA,
                to = BRUNO,
                amountMinor = 3_000,
            ),
        )

        // Then
        assertEquals(
            DebtSimplificationSummary(
                paymentCount = 1,
                unsimplifiedPaymentCount = 1,
            ),
            viewModel.uiState.value.simplification,
        )
    }

    @Test
    fun `GIVEN the switch off WHEN a sync turns it on THEN shows it on with its payments line`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            )

            // When
            groups.groups.value = listOf(threePeople.copy(simplifiesDebts = true))

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.simplifiesDebts)
            assertEquals(
                DebtSimplificationSummary(
                    paymentCount = 1,
                    unsimplifiedPaymentCount = 2,
                ),
                state.simplification,
            )
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN the switch on WHEN a sync turns it off THEN shows it off with no payments line`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople.copy(simplifiesDebts = true),
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            )

            // When
            groups.groups.value = listOf(threePeople)

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.simplifiesDebts)
            assertNull(state.simplification)
        }

    @Test
    fun `GIVEN Room not read yet WHEN tapping the switch THEN no confirmation opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

        // Then
        assertNull(viewModel.uiState.value.simplifyConfirmation)
    }

    @Test
    fun `GIVEN a group of two not simplifying WHEN tapping the switch THEN no confirmation opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedExpenses = listOf(dinner))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

            // Then
            assertNull(viewModel.uiState.value.simplifyConfirmation)
        }

    @Test
    fun `GIVEN a group of two simplifying WHEN tapping the switch THEN asks to turn it off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = trip.copy(simplifiesDebts = true),
                storedExpenses = listOf(dinner),
            )

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

            // Then
            assertEquals(
                false,
                viewModel.uiState.value.simplifyConfirmation
                    ?.isTurningOn,
            )
        }

    @Test
    fun `GIVEN a group of three not simplifying WHEN tapping the switch THEN asks to turn it on with what changes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            )

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

            // Then
            val confirmation = assertNotNull(viewModel.uiState.value.simplifyConfirmation)
            assertTrue(confirmation.isTurningOn)
            assertEquals("Viagem Floripa", confirmation.groupName)
            assertEquals(2, confirmation.paymentsBefore)
            assertEquals(1, confirmation.paymentsAfter)
            assertFalse(confirmation.isAlreadyMinimal)
            assertEquals(0, confirmation.othersChangingCount)
            assertEquals(listOf(PIERRE, ANA, BRUNO), confirmation.people.map { it.memberId })
            assertEquals(listOf(false, true, false), confirmation.people.map { it.isGuest })
            val me = confirmation.people.first()
            assertEquals(
                SimplifyName(
                    name = "Pierre",
                    isMe = true,
                ),
                me.person,
            )
            assertEquals(
                listOf(
                    SimplifyName(
                        name = "Ana",
                        isMe = false,
                    ),
                ),
                me.receivesFrom,
            )
            assertEquals(
                listOf(
                    SimplifyName(
                        name = "Bruno",
                        isMe = false,
                    ),
                ),
                me.stopsReceivingFrom,
            )
            val ana = confirmation.people[1]
            assertEquals(
                listOf(
                    SimplifyAmount(
                        to = me.person,
                        amountMinor = 3_000,
                    ),
                ),
                ana.pays,
            )
            assertEquals(
                listOf(
                    SimplifyAmount(
                        to = SimplifyName(
                            name = "Bruno",
                            isMe = false,
                        ),
                        amountMinor = 3_000,
                    ),
                ),
                ana.stopsPaying,
            )
        }

    @Test
    fun `GIVEN a group of three not simplifying WHEN tapping the switch THEN the switch itself stays off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            )

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

            // Then
            assertFalse(viewModel.uiState.value.simplifiesDebts)
            assertTrue(groups.simplifyDebtsChanges.isEmpty())
        }

    @Test
    fun `GIVEN bills already at their fewest payments WHEN tapping the switch THEN says it is at the minimum`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(pierrePaidForEveryone),
            )

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

            // Then
            val confirmation = assertNotNull(viewModel.uiState.value.simplifyConfirmation)
            assertTrue(confirmation.isAlreadyMinimal)
            assertEquals(2, confirmation.paymentsBefore)
            assertEquals(2, confirmation.paymentsAfter)
            assertEquals(emptyList(), confirmation.people)
        }

    @Test
    fun `GIVEN a group of three simplifying WHEN tapping the switch THEN asks to turn it off naming only you`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople.copy(simplifiesDebts = true),
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            )

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

            // Then
            val confirmation = assertNotNull(viewModel.uiState.value.simplifyConfirmation)
            assertFalse(confirmation.isTurningOn)
            assertEquals(1, confirmation.paymentsBefore)
            assertEquals(2, confirmation.paymentsAfter)
            assertEquals(listOf(PIERRE), confirmation.people.map { it.memberId })
            assertEquals(2, confirmation.othersChangingCount)
            assertFalse(confirmation.isAlreadyMinimal)
        }

    @Test
    fun `GIVEN a payment in the queue WHEN tapping the switch THEN the confirmation lists only the queued one`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
                storedSettlements = listOf(
                    payment(
                        from = ANA,
                        to = BRUNO,
                        amountMinor = 1_000,
                        isPending = true,
                    ),
                    payment(
                        from = BRUNO,
                        to = PIERRE,
                        amountMinor = 500,
                        isPending = false,
                    ),
                ),
            )

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

            // Then
            assertEquals(
                listOf(
                    SimplifyQueuedPayment(
                        from = SimplifyName(
                            name = "Ana",
                            isMe = false,
                        ),
                        to = SimplifyName(
                            name = "Bruno",
                            isMe = false,
                        ),
                        amountMinor = 1_000,
                    ),
                ),
                viewModel.uiState.value.simplifyConfirmation
                    ?.queuedPayments,
            )
        }

    @Test
    fun `GIVEN the confirmation to turn it on WHEN a sync turns it on THEN the confirmation closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            )
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

            // When
            groups.groups.value = listOf(threePeople.copy(simplifiesDebts = true))

            // Then
            assertNull(viewModel.uiState.value.simplifyConfirmation)
        }

    @Test
    fun `GIVEN the confirmation open WHEN a new bill arrives THEN it counts the payments again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(pierrePaidForBruno),
            )
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

            // When
            expenses.expenses.value = listOf(pierrePaidForBruno, brunoPaidForAna)

            // Then
            val confirmation = assertNotNull(viewModel.uiState.value.simplifyConfirmation)
            assertEquals(2, confirmation.paymentsBefore)
            assertEquals(1, confirmation.paymentsAfter)
        }

    @Test
    fun `GIVEN the confirmation open WHEN a sync renames the group THEN it stays open with the new name`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            )
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

            // When
            groups.groups.value = listOf(threePeople.copy(name = "Futebol de quinta"))

            // Then
            assertEquals(
                "Futebol de quinta",
                viewModel.uiState.value.simplifyConfirmation
                    ?.groupName,
            )
        }

    @Test
    fun `GIVEN the confirmation open WHEN dismissing it THEN it closes and the switch stays as it was`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            )
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsDismissed)

            // Then
            val state = viewModel.uiState.value
            assertNull(state.simplifyConfirmation)
            assertFalse(state.simplifiesDebts)
            assertTrue(groups.simplifyDebtsChanges.isEmpty())
        }

    @Test
    fun `GIVEN the confirmation to turn it on WHEN confirming THEN turns it on with the pairs before and after`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            )
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsConfirmed)

            // Then
            assertEquals(
                listOf(
                    Triple(
                        GROUP_ID,
                        true,
                        SimplifyDebtsChange(
                            before = listOf(
                                SimplifyDebtsPair(
                                    fromMemberId = ANA,
                                    toMemberId = BRUNO,
                                    amountMinor = 3_000,
                                ),
                                SimplifyDebtsPair(
                                    fromMemberId = BRUNO,
                                    toMemberId = PIERRE,
                                    amountMinor = 3_000,
                                ),
                            ),
                            after = listOf(
                                SimplifyDebtsPair(
                                    fromMemberId = ANA,
                                    toMemberId = PIERRE,
                                    amountMinor = 3_000,
                                ),
                            ),
                        ),
                    ),
                ),
                groups.simplifyDebtsChanges,
            )
            val state = viewModel.uiState.value
            assertTrue(state.simplifiesDebts)
            assertEquals(SimplifyDebtsStatus.PENDING, state.simplifyStatus)
            assertNull(state.simplifyConfirmation)
            assertFalse(state.isChangingSimplifyDebts)
            assertFalse(state.showsSaveError)
        }

    @Test
    fun `GIVEN the confirmation to turn it on WHEN confirming THEN Salvar stays disabled and the form is not saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            )
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsConfirmed)

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.hasChanges)
            assertFalse(state.canSave)
            assertTrue(groups.updatedGroupIds.isEmpty())
            assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a new name typed WHEN confirming the switch THEN the name stays unsaved with Salvar enabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            )
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsConfirmed)

            // Then
            val state = viewModel.uiState.value
            assertEquals("Churrasco", state.name)
            assertTrue(state.canSave)
            assertTrue(state.simplifiesDebts)
            assertTrue(groups.updatedGroupIds.isEmpty())
            assertEquals(
                "Viagem Floripa",
                groups.groups.value
                    .single()
                    .name,
            )
        }

    @Test
    fun `GIVEN the switch confirmed WHEN tapping Salvar on a new name THEN the save sends no switch change`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            )
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsConfirmed)
            viewModel.onEvent(EditGroupUiEvent.OnNameChanged("Churrasco"))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSaveClicked)

            // Then
            assertEquals(1, groups.simplifyDebtsChanges.size)
            assertEquals(listOf(GROUP_ID), groups.updatedGroupIds)
            val saved = groups.groups.value.single()
            assertEquals("Churrasco", saved.name)
            assertTrue(saved.simplifiesDebts)
        }

    @Test
    fun `GIVEN no internet WHEN confirming the switch THEN turns it on on the device`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroup = threePeople,
            storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            online = false,
        )
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsConfirmed)

        // Then
        assertEquals(listOf(true), groups.simplifyDebtsChanges.map { it.second })
        val state = viewModel.uiState.value
        assertTrue(state.simplifiesDebts)
        assertTrue(state.showsSimplifyOfflineNotice)
    }

    @Test
    fun `GIVEN the confirmation to turn it off WHEN confirming THEN turns it off`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroup = threePeople.copy(simplifiesDebts = true),
            storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
        )
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsConfirmed)

        // Then
        assertEquals(listOf(false), groups.simplifyDebtsChanges.map { it.second })
        val state = viewModel.uiState.value
        assertFalse(state.simplifiesDebts)
        assertNull(state.simplification)
        assertNull(state.simplifyConfirmation)
    }

    @Test
    fun `GIVEN the device fails to change it WHEN confirming the switch THEN shows the save error and closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
                simplifyError = IllegalStateException("disk full"),
            )
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsConfirmed)

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.showsSaveError)
            assertNull(state.simplifyConfirmation)
            assertFalse(state.isChangingSimplifyDebts)
            assertFalse(state.simplifiesDebts)
        }

    @Test
    fun `GIVEN a switch change in flight WHEN reading the switch THEN it cannot change until it ends`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople,
                storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
                isSimplifyHeld = true,
            )
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsConfirmed)

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isChangingSimplifyDebts)
            assertFalse(state.canChangeSimplifyDebts)
        }

    @Test
    fun `GIVEN a switch change in flight WHEN confirming again THEN changes it only once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroup = threePeople,
            storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
            isSimplifyHeld = true,
        )
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsClicked)
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsConfirmed)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsConfirmed)
        groups.simplifyDebtsGate?.complete(Unit)

        // Then
        assertEquals(1, groups.simplifyDebtsChanges.size)
        assertNull(viewModel.uiState.value.simplifyConfirmation)
    }

    @Test
    fun `GIVEN no confirmation open WHEN confirming the switch THEN changes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedGroup = threePeople,
            storedExpenses = listOf(brunoPaidForAna, pierrePaidForBruno),
        )

        // When
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsConfirmed)

        // Then
        assertTrue(groups.simplifyDebtsChanges.isEmpty())
        assertFalse(viewModel.uiState.value.simplifiesDebts)
    }

    @Test
    fun `GIVEN the form on top WHEN tapping Como funciona THEN opens it in the group currency`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = threePeople)

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsInfoClicked)

            // Then
            assertEquals(
                listOf(Route.Groups, groupDetail, editGroup, Route.SimplifyDebtsInfo(GroupCurrency.BRL)),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN another currency picked in the form WHEN tapping Como funciona THEN opens it in the currency picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = threePeople)
            viewModel.onEvent(EditGroupUiEvent.OnCurrencySelected(GroupCurrency.USD))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsInfoClicked)

            // Then
            assertEquals(Route.SimplifyDebtsInfo(GroupCurrency.USD), navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN another screen over the form WHEN tapping Como funciona THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = threePeople)
        navigator.navigate(Route.DefaultSplit(GROUP_ID))

        // When
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsInfoClicked)

        // Then
        assertEquals(
            listOf(Route.Groups, groupDetail, editGroup, Route.DefaultSplit(GROUP_ID)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Room not read yet WHEN tapping Como funciona THEN nothing opens`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isRoomRead = false)

        // When
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyDebtsInfoClicked)

        // Then
        assertEquals(listOf(Route.Groups, groupDetail, editGroup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Bruno changed the switch after this device WHEN the form opens on top THEN says so once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = threePeople.copy(simplifyDebtsConflictBy = BRUNO_USER))

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                SimplifyDebtsConflictNotice(
                    changedByName = "Bruno",
                    simplifiesDebts = false,
                ),
                state.simplifyConflict,
            )
            assertEquals(listOf(GROUP_ID), groups.consumedSimplifyConflictGroupIds)
        }

    @Test
    fun `GIVEN a change after this device by someone no longer in the group WHEN the form opens THEN names nobody`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople.copy(
                    simplifiesDebts = true,
                    simplifyDebtsConflictBy = "user-gone",
                ),
            )

            // When
            val notice = viewModel.uiState.value.simplifyConflict

            // Then
            assertEquals(
                SimplifyDebtsConflictNotice(
                    changedByName = null,
                    simplifiesDebts = true,
                ),
                notice,
            )
        }

    @Test
    fun `GIVEN the form open WHEN a sync brings a later change by Bruno THEN says so once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = threePeople)

        // When
        groups.groups.value = listOf(threePeople.copy(simplifyDebtsConflictBy = BRUNO_USER))

        // Then
        assertEquals(
            "Bruno",
            viewModel.uiState.value.simplifyConflict
                ?.changedByName,
        )
        assertEquals(listOf(GROUP_ID), groups.consumedSimplifyConflictGroupIds)
    }

    @Test
    fun `GIVEN another screen over the form WHEN a sync brings a later change THEN the form says nothing yet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = threePeople)
            navigator.navigate(Route.DefaultSplit(GROUP_ID))

            // When
            groups.groups.value = listOf(threePeople.copy(simplifyDebtsConflictBy = BRUNO_USER))

            // Then
            assertNull(viewModel.uiState.value.simplifyConflict)
            assertTrue(groups.consumedSimplifyConflictGroupIds.isEmpty())
        }

    @Test
    fun `GIVEN a later change while another screen covered the form WHEN that screen closes THEN says so once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = threePeople)
            navigator.navigate(Route.DefaultSplit(GROUP_ID))
            groups.groups.value = listOf(threePeople.copy(simplifyDebtsConflictBy = BRUNO_USER))

            // When
            navigator.navigateBack()

            // Then
            assertEquals(
                "Bruno",
                viewModel.uiState.value.simplifyConflict
                    ?.changedByName,
            )
            assertEquals(listOf(GROUP_ID), groups.consumedSimplifyConflictGroupIds)
        }

    @Test
    fun `GIVEN the later change shown WHEN another screen opens and closes THEN it is not consumed again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = threePeople.copy(simplifyDebtsConflictBy = BRUNO_USER))
            navigator.navigate(Route.DefaultSplit(GROUP_ID))

            // When
            navigator.navigateBack()

            // Then
            assertEquals(listOf(GROUP_ID), groups.consumedSimplifyConflictGroupIds)
        }

    @Test
    fun `GIVEN the later change shown WHEN it is dismissed THEN it goes away`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedGroup = threePeople.copy(simplifyDebtsConflictBy = BRUNO_USER))

        // When
        viewModel.onEvent(EditGroupUiEvent.OnSimplifyConflictDismissed)

        // Then
        assertNull(viewModel.uiState.value.simplifyConflict)
    }

    @Test
    fun `GIVEN the later change shown WHEN tapping Ver na Atividade THEN opens Atividade right above Inicio`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople.copy(simplifyDebtsConflictBy = BRUNO_USER),
                isAboveHome = true,
            )

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyConflictActivityClicked)

            // Then
            assertEquals(listOf(Route.Home, Route.Activity(personKey = null)), navigator.backStack.value)
            assertNull(viewModel.uiState.value.simplifyConflict)
        }

    @Test
    fun `GIVEN the later change shown under another screen WHEN tapping Ver na Atividade THEN only hides it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedGroup = threePeople.copy(simplifyDebtsConflictBy = BRUNO_USER),
                isAboveHome = true,
            )
            navigator.navigate(Route.DefaultSplit(GROUP_ID))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnSimplifyConflictActivityClicked)

            // Then
            assertEquals(
                listOf(Route.Home, Route.Groups, groupDetail, editGroup, Route.DefaultSplit(GROUP_ID)),
                navigator.backStack.value,
            )
            assertNull(viewModel.uiState.value.simplifyConflict)
        }

    private val editGroup = Route.EditGroup(GROUP_ID)

    private val groupDetail = Route.GroupDetail(
        GROUP_ID,
        joinedAs = null,
        tab = GroupTab.EXPENSES,
    )

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = listOf(
            GroupMember(
                id = PIERRE,
                name = "Pierre",
                avatarColor = AvatarColor.BLUE,
                isMe = true,
                hasJoined = true,
                userId = null,
                inviteCode = null,
                photoUrl = null,
            ),
            GroupMember(
                id = ANA,
                name = "Ana",
                avatarColor = AvatarColor.PEACH,
                isMe = false,
                hasJoined = false,
                userId = null,
                inviteCode = null,
                photoUrl = null,
            ),
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

    private val remotePhoto = ProfilePhoto.Remote("https://example.com/group-photos/group-1/photo.jpg?v=1")

    private val otherRemotePhoto = ProfilePhoto.Remote("https://example.com/group-photos/group-1/photo.jpg?v=2")

    private val pickedFile = byteArrayOf(1, 2, 3)

    private val preparedPhoto = ProfilePhoto.Pending(byteArrayOf(9, 8, 7))

    private val threeToOne = GroupDefaultSplit(
        mode = DefaultSplitMode.SHARES,
        weights = mapOf(PIERRE to 3L, ANA to 1L),
        exceptions = emptyList(),
    )

    private val dinner = Expense(
        id = "expense-1",
        groupId = GROUP_ID,
        description = "Jantar",
        amountMinor = 12_000,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(PIERRE, 12_000)),
        shares = listOf(
            ExpenseShare(
                memberId = PIERRE,
                amountMinor = 6_000,
                weight = 1,
            ),
            ExpenseShare(
                memberId = ANA,
                amountMinor = 6_000,
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

    private val bruno = GroupMember(
        id = BRUNO,
        name = "Bruno",
        avatarColor = AvatarColor.LILAC,
        isMe = false,
        hasJoined = true,
        userId = BRUNO_USER,
        inviteCode = null,
        photoUrl = null,
    )

    // Why: three people, so "Simplificar dívidas" can be turned on.
    private val threePeople = trip.copy(members = trip.members + bruno)

    // Why: Ana owes Bruno R$ 30, and with pierrePaidForBruno Bruno owes Pierre R$ 30: one payment, Ana to Pierre.
    private val brunoPaidForAna = bill(
        id = "expense-2",
        payerId = BRUNO,
        shares = mapOf(ANA to 3_000L, BRUNO to 3_000L),
    )

    private val pierrePaidForBruno = bill(
        id = "expense-3",
        payerId = PIERRE,
        shares = mapOf(PIERRE to 3_000L, BRUNO to 3_000L),
    )

    // Why: Ana and Bruno each owe Pierre, already the fewest payments.
    private val pierrePaidForEveryone = bill(
        id = "expense-4",
        payerId = PIERRE,
        shares = mapOf(PIERRE to 3_000L, ANA to 3_000L, BRUNO to 3_000L),
    )

    private fun bill(
        id: String,
        payerId: String,
        shares: Map<String, Long>,
    ): Expense = dinner.copy(
        id = id,
        amountMinor = shares.values.sum(),
        payers = listOf(ExpensePayer(payerId, shares.values.sum())),
        shares = shares.map { (memberId, amountMinor) ->
            ExpenseShare(
                memberId = memberId,
                amountMinor = amountMinor,
                weight = 1,
            )
        },
    )

    private fun payment(
        from: String,
        to: String,
        amountMinor: Long,
        isPending: Boolean = false,
    ): Settlement = Settlement(
        id = "settlement-$from-$to",
        groupId = GROUP_ID,
        fromMemberId = from,
        toMemberId = to,
        amountMinor = amountMinor,
        method = SettlementMethod.PIX,
        paidAt = 2_000L,
        isPending = isPending,
        updatedAt = 2_000L,
    )

    @Test
    fun `GIVEN a photo picked WHEN turning it in Ajustar foto THEN frames it turned before the form shows it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(EditGroupUiEvent.OnPhotoPicked(pickedFile))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnTurnClicked))

            // Then
            val state = viewModel.uiState.value
            assertEquals(1, state.photoCrop?.crop?.quarterTurns)
            assertNull(state.photo)
        }

    @Test
    fun `GIVEN Ajustar foto WHEN dismissing it THEN the group keeps no photo`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(EditGroupUiEvent.OnPhotoPicked(pickedFile))

        // When
        viewModel.onEvent(EditGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnDismissed))

        // Then
        val state = viewModel.uiState.value
        assertNull(state.photoCrop)
        assertNull(state.photo)
        assertFalse(state.canSave)
    }

    // Why: a picked photo opens "Ajustar foto" first; Concluir there, as it opened, makes the avatar.
    private fun pickAndConfirmPhoto(source: ByteArray) {
        viewModel.onEvent(EditGroupUiEvent.OnPhotoPicked(source))
        viewModel.onEvent(EditGroupUiEvent.OnPhotoCrop(PhotoCropEvent.OnConfirmClicked))
    }

    private fun prepareScenario(
        storedGroup: Group = trip,
        storedExpenses: List<Expense> = emptyList(),
        isRoomRead: Boolean = true,
        online: Boolean = true,
        updateError: Throwable? = null,
        isSaveHeld: Boolean = false,
        isPhotoHeld: Boolean = false,
        photoError: Throwable? = null,
        storedSettlements: List<Settlement> = emptyList(),
        simplifyError: Throwable? = null,
        isSimplifyHeld: Boolean = false,
        isAboveHome: Boolean = false,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup)).also {
            it.updateGroupError = updateError
            it.updateGroupGate = if (isSaveHeld) CompletableDeferred() else null
            it.simplifyDebtsError = simplifyError
            it.simplifyDebtsGate = if (isSimplifyHeld) CompletableDeferred() else null
        }
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        settlements = FakeSettlementRepository(initialSettlements = storedSettlements)
        isOnline = MutableStateFlow(online)
        navigator = BackStackNavigator(startRoute = if (isAboveHome) Route.Home else Route.Groups).also {
            if (isAboveHome) it.navigate(Route.Groups)
            it.navigate(groupDetail)
            it.navigate(editGroup)
        }
        photoGate = CompletableDeferred<Unit>().also { if (!isPhotoHeld) it.complete(Unit) }
        val roomNeverAnswers = MutableSharedFlow<Group?>()
        viewModel = EditGroupViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            route = editGroup,
            navigator = navigator,
            updateGroup = UpdateGroupUseCase(groups) { 1_000L },
            setSimplifiesDebts = SetSimplifiesDebtsUseCase(groups, expenses, settlements) { 1_000L },
            consumeSimplifyDebtsConflict = ConsumeSimplifyDebtsConflictUseCase(groups),
            observeGroup = if (isRoomRead) ObserveGroupUseCase(groups) else ObserveGroup { roomNeverAnswers },
            observeExpenses = ObserveExpensesUseCase(expenses),
            observeSettlements = ObserveSettlementsUseCase(settlements),
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            loadCropPhoto = { source ->
                Result.success(
                    CropPhoto(
                        jpeg = source,
                        width = 1200,
                        height = 900,
                    ),
                )
            },
            prepareProfilePhoto = { _, _ ->
                photoGate.await()
                if (photoError != null) Result.failure(photoError) else Result.success(preparedPhoto)
            },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val PIERRE = "member-1"
        const val ANA = "member-2"
        const val BRUNO = "member-3"
        const val BRUNO_USER = "user-bruno"
    }
}
