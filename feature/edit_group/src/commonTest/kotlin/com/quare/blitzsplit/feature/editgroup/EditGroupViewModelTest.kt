package com.quare.blitzsplit.feature.editgroup

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupUseCase
import com.quare.blitzsplit.core.groups.domain.usecase.impl.UpdateGroupUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiEvent
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
import kotlin.test.assertTrue

class EditGroupViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var groups: FakeGroupRepository
    private lateinit var expenses: FakeExpenseRepository
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: EditGroupViewModel

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
            assertEquals("CH", state.initials)
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
    fun `GIVEN a green group WHEN tapping its avatar THEN takes the next color of the palette`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(EditGroupUiEvent.OnColorClicked)

            // Then
            assertEquals(AvatarColor.LILAC, viewModel.uiState.value.color)
        }

    @Test
    fun `GIVEN a group of the last color WHEN tapping its avatar THEN wraps around to the first color`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedGroup = trip.copy(color = AvatarColor.entries.last()))

            // When
            viewModel.onEvent(EditGroupUiEvent.OnColorClicked)

            // Then
            assertEquals(AvatarColor.entries.first(), viewModel.uiState.value.color)
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
        viewModel.onEvent(EditGroupUiEvent.OnColorClicked)

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
            viewModel.onEvent(EditGroupUiEvent.OnColorClicked)
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
            viewModel.onEvent(EditGroupUiEvent.OnColorClicked)

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
            viewModel.onEvent(EditGroupUiEvent.OnColorClicked)
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
            viewModel.onEvent(EditGroupUiEvent.OnColorClicked)
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
        viewModel.onEvent(EditGroupUiEvent.OnColorClicked)
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
        viewModel.onEvent(EditGroupUiEvent.OnColorClicked)
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
            viewModel.onEvent(EditGroupUiEvent.OnColorClicked)
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
            viewModel.onEvent(EditGroupUiEvent.OnColorClicked)
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
            viewModel.onEvent(EditGroupUiEvent.OnColorClicked)

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
            viewModel.onEvent(EditGroupUiEvent.OnColorClicked)

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
            viewModel.onEvent(EditGroupUiEvent.OnColorClicked)

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
            viewModel.onEvent(EditGroupUiEvent.OnColorClicked)

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
            viewModel.onEvent(EditGroupUiEvent.OnColorClicked)

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
            viewModel.onEvent(EditGroupUiEvent.OnColorClicked)

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
            viewModel.onEvent(EditGroupUiEvent.OnColorClicked)

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
    fun `GIVEN the avatar tapped through the palette WHEN back at its first color THEN Salvar is disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            repeat(AvatarColor.entries.size - 1) { viewModel.onEvent(EditGroupUiEvent.OnColorClicked) }

            // When
            viewModel.onEvent(EditGroupUiEvent.OnColorClicked)

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

    private val editGroup = Route.EditGroup(GROUP_ID)

    private val groupDetail = Route.GroupDetail(GROUP_ID)

    private val trip = Group(
        id = GROUP_ID,
        name = "Viagem Floripa",
        type = GroupType.TRIP,
        color = AvatarColor.GREEN,
        currency = GroupCurrency.BRL,
        members = listOf(
            GroupMember(
                id = PIERRE,
                name = "Pierre",
                avatarColor = AvatarColor.BLUE,
                isMe = true,
                hasJoined = true,
                inviteCode = null,
            ),
            GroupMember(
                id = ANA,
                name = "Ana",
                avatarColor = AvatarColor.PEACH,
                isMe = false,
                hasJoined = false,
                inviteCode = null,
            ),
        ),
        isPending = false,
        inviteCode = "AB12CD",
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
        spentAt = 1_000L,
        isPending = false,
        updatedAt = 1_000L,
        updatedByMemberId = null,
    )

    private fun prepareScenario(
        storedGroup: Group = trip,
        storedExpenses: List<Expense> = emptyList(),
        isRoomRead: Boolean = true,
        online: Boolean = true,
        updateError: Throwable? = null,
        isSaveHeld: Boolean = false,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(storedGroup)).also {
            it.updateGroupError = updateError
            it.updateGroupGate = if (isSaveHeld) CompletableDeferred() else null
        }
        expenses = FakeExpenseRepository(initialExpenses = storedExpenses)
        isOnline = MutableStateFlow(online)
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(groupDetail)
            it.navigate(editGroup)
        }
        val roomNeverAnswers = MutableSharedFlow<Group?>()
        viewModel = EditGroupViewModel(
            route = editGroup,
            navigator = navigator,
            updateGroup = UpdateGroupUseCase(groups) { 1_000L },
            observeGroup = if (isRoomRead) ObserveGroupUseCase(groups) else ObserveGroup { roomNeverAnswers },
            observeExpenses = ObserveExpensesUseCase(expenses),
            connectivityObserver = NetworkConnectivityObserver { isOnline },
        )
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val PIERRE = "member-1"
        const val ANA = "member-2"
    }
}
