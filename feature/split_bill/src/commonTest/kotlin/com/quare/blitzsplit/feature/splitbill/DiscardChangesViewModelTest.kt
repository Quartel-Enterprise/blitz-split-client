package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentAddition
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
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
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitAsDefault
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

// Why: "Rachar conta", "Editar conta" and "Duplicar" ask "Discard changes?" before closing a form the person changed.
class DiscardChangesViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val pendingReads = mutableListOf<CompletableDeferred<ReceiptScan>>()
    private lateinit var groups: FakeGroupRepository
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
    fun `GIVEN the form not loaded yet WHEN reading it THEN nothing is unsaved`() = runTest(testDispatcher) {
        // Given
        prepareScenario(isSatsUnitRead = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
        assertNull(state.openedInputs)
        assertFalse(state.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a new bill WHEN the form opens THEN it is loaded and nothing is unsaved`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoaded)
        assertNotNull(state.openedInputs)
        assertFalse(state.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a bill being edited WHEN the form opens THEN nothing is unsaved`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            screen = editRoute,
            underneath = editUnderneath,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isEditing)
        assertNotNull(state.openedInputs)
        assertFalse(state.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a saved bill WHEN Duplicar opens THEN what was copied is not unsaved`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            screen = duplicateRoute,
            underneath = editUnderneath,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals("Mercado", state.description)
        assertNotNull(state.openedInputs)
        assertFalse(state.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a new bill WHEN typing an amount THEN it is unsaved`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("90"))

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a new bill WHEN typing a description THEN it is unsaved`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado"))

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN an amount typed WHEN erasing it THEN nothing is unsaved again`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("90"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged(""))

        // Then
        assertFalse(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a description typed WHEN erasing it THEN nothing is unsaved again`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged(""))

        // Then
        assertFalse(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a new bill WHEN typing only spaces as the description THEN nothing is unsaved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("   "))

            // Then
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a bill being edited WHEN typing spaces after the description THEN nothing is unsaved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado   "))

            // Then
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a bill being edited WHEN typing the same amount another way THEN nothing is unsaved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("312"))

            // Then
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a bill being edited WHEN changing the description THEN it is unsaved`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            screen = editRoute,
            underneath = editUnderneath,
        )

        // When
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a bill being edited WHEN changing the amount THEN it is unsaved`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            screen = editRoute,
            underneath = editUnderneath,
        )

        // When
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("313"))

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN an edited description WHEN typing the original back THEN nothing is unsaved again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado"))

            // Then
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a new bill WHEN picking another currency THEN it is unsaved`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCurrencyPicked(GroupCurrency.USD))

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a new bill WHEN moving it to another group THEN it is unsaved`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        pickGroup(HOME_ID)

        // Then
        assertEquals(HOME_ID, viewModel.uiState.value.groupId)
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a bill moved to another group WHEN moving it back THEN nothing is unsaved again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            pickGroup(HOME_ID)

            // When
            pickGroup(TRIP_ID)

            // Then
            assertEquals(TRIP_ID, viewModel.uiState.value.groupId)
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a bill being edited WHEN picking another payer with Pronto THEN it is unsaved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )

            // When
            pickPayer(PIERRE)

            // Then
            assertTrue(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a payer picked in the open sheet WHEN dismissing it THEN nothing is unsaved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )
            viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(PIERRE))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnPayerDismissed)

            // Then
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN another payer picked WHEN picking the original payer back THEN nothing is unsaved again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )
            pickPayer(PIERRE)

            // When
            pickPayer(ANA)

            // Then
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a bill being edited WHEN changing Quando THEN it is unsaved`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            screen = editRoute,
            underneath = editUnderneath,
        )

        // When
        viewModel.pickSpentAt(earlier)

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN Quando changed WHEN picking the original moment back THEN nothing is unsaved again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )
            viewModel.pickSpentAt(earlier)

            // When
            viewModel.pickSpentAt(yesterday)

            // Then
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a bill being edited WHEN excluding someone from the split with Pronto THEN it is unsaved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )

            // When
            toggleInSplit(BRUNO)

            // Then
            assertTrue(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN someone toggled in the open split sheet WHEN dismissing it THEN nothing is unsaved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
            viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(BRUNO))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitDismissed)

            // Then
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN someone excluded from the split WHEN including them again THEN nothing is unsaved again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )
            toggleInSplit(BRUNO)

            // When
            toggleInSplit(BRUNO)

            // Then
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a bill being edited WHEN changing the split mode with Pronto THEN it is unsaved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )
            viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSplitModeClicked(SplitMode.SHARES))
            viewModel.onEvent(SplitBillUiEvent.OnSplitDone)

            // Then
            assertEquals(SplitMode.SHARES, viewModel.uiState.value.split.mode)
            assertTrue(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a bill being edited WHEN picking a category THEN it is unsaved`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            screen = editRoute,
            underneath = editUnderneath,
        )

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCategoryClicked)
        viewModel.onEvent(SplitBillUiEvent.OnCategoryPicked(AppCategory.FOOD_MARKET.key))

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a new bill WHEN a receipt photo is picked THEN it is unsaved`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnReceiptPhotoPicked(byteArrayOf(1, 2, 3)))

        // Then
        assertNotNull(viewModel.uiState.value.receipt)
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
    }

    @Test
    fun `GIVEN a bill being edited WHEN it is set to become the group default THEN it is unsaved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )
            val loaded = viewModel.uiState.value

            // When
            val state = loaded.copy(splitAsDefault = SplitAsDefault(exceptionCategoryId = null))

            // Then
            assertFalse(loaded.hasUnsavedInput)
            assertTrue(state.inputs.splitsAsDefault)
            assertTrue(state.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a description with spaces around it WHEN reading the inputs THEN it is trimmed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )
            val state = viewModel.uiState.value.copy(description = "  Mercado  ")

            // When
            val inputs = state.inputs

            // Then
            assertEquals("Mercado", inputs.description)
        }

    @Test
    fun `GIVEN a bill being edited WHEN a sync adds a member that is not in the split THEN nothing is unsaved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )

            // When
            groups.groups.value = listOf(trip.copy(members = trip.members + diego))

            // Then
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a new bill WHEN a sync adds a member who joins the equal split THEN it is unsaved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            groups.groups.value = listOf(trip.copy(members = trip.members + diego))

            // Then
            assertTrue(
                viewModel.uiState.value.inputs.splitEntries
                    .any { it.memberId == DIEGO && it.value > 0L },
            )
            assertTrue(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a changed form WHEN reading the navigator THEN it knows there is unsaved input`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("90"))

            // When
            val hasUnsavedInput = navigator.hasUnsavedInput.value

            // Then
            assertTrue(hasUnsavedInput)
        }

    @Test
    fun `GIVEN a changed form WHEN changing it back THEN the navigator knows there is none`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("90"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged(""))

            // Then
            assertFalse(navigator.hasUnsavedInput.value)
        }

    @Test
    fun `GIVEN a new bill with unsaved input WHEN tapping close THEN it asks and stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            fillIn()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCloseClicked)

            // Then
            assertTrue(navigator.pendingDiscard.value)
            assertEquals(listOf(Route.Groups, tripExpenses, newRoute), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the question open WHEN confirming the discard THEN the form closes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        fillIn()
        viewModel.onEvent(SplitBillUiEvent.OnCloseClicked)

        // When
        navigator.confirmDiscard()

        // Then
        assertFalse(navigator.pendingDiscard.value)
        assertEquals(listOf(Route.Groups, tripExpenses), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the question open WHEN choosing to keep editing THEN the form stays with what was typed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            fillIn()
            viewModel.onEvent(SplitBillUiEvent.OnCloseClicked)

            // When
            navigator.cancelDiscard()

            // Then
            assertFalse(navigator.pendingDiscard.value)
            assertEquals(newRoute, navigator.backStack.value.last())
            assertEquals("Mercado", viewModel.uiState.value.description)
        }

    @Test
    fun `GIVEN a new bill nobody touched WHEN tapping close THEN it closes at once`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCloseClicked)

        // Then
        assertFalse(navigator.pendingDiscard.value)
        assertEquals(listOf(Route.Groups, tripExpenses), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a form changed back to how it opened WHEN tapping close THEN it closes at once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            fillIn()
            viewModel.onEvent(SplitBillUiEvent.OnAmountChanged(""))
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged(""))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCloseClicked)

            // Then
            assertFalse(navigator.pendingDiscard.value)
            assertEquals(listOf(Route.Groups, tripExpenses), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a bill being edited with unsaved input WHEN tapping close THEN it asks and stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCloseClicked)

            // Then
            assertTrue(navigator.pendingDiscard.value)
            assertEquals(editUnderneath + editRoute, navigator.backStack.value)
        }

    @Test
    fun `GIVEN an edit with the question open WHEN confirming the discard THEN the form closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))
            viewModel.onEvent(SplitBillUiEvent.OnCloseClicked)

            // When
            navigator.confirmDiscard()

            // Then
            assertEquals(editUnderneath, navigator.backStack.value)
            assertFalse(navigator.pendingDiscard.value)
        }

    @Test
    fun `GIVEN a bill being edited nobody touched WHEN tapping close THEN it closes at once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )

            // When
            viewModel.onEvent(SplitBillUiEvent.OnCloseClicked)

            // Then
            assertFalse(navigator.pendingDiscard.value)
            assertEquals(editUnderneath, navigator.backStack.value)
        }

    @Test
    fun `GIVEN Duplicar nobody touched WHEN tapping close THEN it closes at once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            screen = duplicateRoute,
            underneath = editUnderneath,
        )

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCloseClicked)

        // Then
        assertFalse(navigator.pendingDiscard.value)
        assertEquals(editUnderneath, navigator.backStack.value)
    }

    @Test
    fun `GIVEN Duplicar with the description changed WHEN tapping close THEN it asks`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            screen = duplicateRoute,
            underneath = editUnderneath,
        )
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCloseClicked)

        // Then
        assertTrue(navigator.pendingDiscard.value)
        assertEquals(editUnderneath + duplicateRoute, navigator.backStack.value)
    }

    @Test
    fun `GIVEN a new bill with unsaved input WHEN saving it THEN it leaves to the group without asking`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            fillIn()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(1, expenses.createdExpenses.size)
            assertFalse(navigator.pendingDiscard.value)
            assertEquals(listOf(Route.Groups, tripExpenses), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a new bill from the home with unsaved input WHEN saving it THEN opens its group without asking`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = fromHomeRoute,
                underneath = listOf(Route.Groups),
            )
            viewModel.onEvent(SplitBillUiEvent.OnGroupDone)
            fillIn()

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(1, expenses.createdExpenses.size)
            assertFalse(navigator.pendingDiscard.value)
            assertEquals(listOf(Route.Groups, tripExpenses), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a bill being edited with unsaved input WHEN saving it THEN it leaves without asking`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))

            // When
            viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

            // Then
            assertEquals(1, expenses.updatedExpenses.size)
            assertFalse(navigator.pendingDiscard.value)
            assertEquals(editUnderneath, navigator.backStack.value)
        }

    @Test
    fun `GIVEN a save that fails WHEN tapping close afterwards THEN it still asks`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            screen = editRoute,
            underneath = editUnderneath,
            updateError = IllegalStateException("offline"),
        )
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))
        viewModel.onEvent(SplitBillUiEvent.OnSaveClicked)

        // When
        viewModel.onEvent(SplitBillUiEvent.OnCloseClicked)

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
        assertTrue(navigator.pendingDiscard.value)
        assertEquals(editUnderneath + editRoute, navigator.backStack.value)
    }

    @Test
    fun `GIVEN a bill edited and being deleted WHEN confirming Apagar THEN it leaves without asking`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screen = editRoute,
                underneath = editUnderneath,
            )
            viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))
            viewModel.onEvent(SplitBillUiEvent.OnDeleteClicked)

            // When
            viewModel.onEvent(SplitBillUiEvent.OnDeleteConfirmed)

            // Then
            assertEquals(listOf(EXPENSE_ID), expenses.deletedIds)
            assertFalse(navigator.pendingDiscard.value)
            assertFalse(editRoute in navigator.backStack.value)
        }

    @Test
    fun `GIVEN a bill edited WHEN its group is gone THEN it leaves without asking`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            screen = editRoute,
            underneath = editUnderneath,
        )
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))

        // When
        groups.groups.value = emptyList()

        // Then
        assertFalse(navigator.pendingDiscard.value)
        assertFalse(editRoute in navigator.backStack.value)
    }

    @Test
    fun `GIVEN a new bill with unsaved input WHEN its group is gone THEN it leaves without asking`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            fillIn()

            // When
            groups.groups.value = emptyList()

            // Then
            assertFalse(navigator.pendingDiscard.value)
            assertFalse(newRoute in navigator.backStack.value)
        }

    @Test
    fun `GIVEN a bill edited WHEN someone deletes the bill THEN it leaves without asking`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            screen = editRoute,
            underneath = editUnderneath,
        )
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Feira"))

        // When
        expenses.expenses.value = emptyList()

        // Then
        assertFalse(navigator.pendingDiscard.value)
        assertFalse(editRoute in navigator.backStack.value)
    }

    @Test
    fun `GIVEN a changed form with its group opened over it WHEN going back from the group THEN it does not ask`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            fillIn()
            viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)

            // When
            navigator.navigateBack()

            // Then
            assertFalse(navigator.pendingDiscard.value)
            assertEquals(listOf(Route.Groups, tripExpenses, newRoute), navigator.backStack.value)
            assertTrue(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a changed form with its group opened WHEN the shell leaves below the form THEN it asks`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            fillIn()
            viewModel.onEvent(SplitBillUiEvent.OnOpenGroupClicked)

            // When
            navigator.replaceAbove(1, tripExpenses)

            // Then
            assertTrue(navigator.pendingDiscard.value)
            assertEquals(listOf(Route.Groups, tripExpenses, newRoute, tripOverBill), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the shell asked WHEN confirming the discard THEN the held change is applied`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            fillIn()
            navigator.popTo(1)

            // When
            navigator.confirmDiscard()

            // Then
            assertFalse(navigator.pendingDiscard.value)
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a form nobody touched WHEN the shell leaves below the form THEN it applies at once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            navigator.replaceAbove(1, tripExpenses)

            // Then
            assertFalse(navigator.pendingDiscard.value)
            assertEquals(listOf(Route.Groups, tripExpenses), navigator.backStack.value)
        }

    private fun fillIn() {
        viewModel.onEvent(SplitBillUiEvent.OnAmountChanged("90"))
        viewModel.onEvent(SplitBillUiEvent.OnDescriptionChanged("Mercado"))
    }

    private fun pickGroup(groupId: String) {
        viewModel.onEvent(SplitBillUiEvent.OnGroupClicked)
        viewModel.onEvent(SplitBillUiEvent.OnGroupPicked(groupId))
        viewModel.onEvent(SplitBillUiEvent.OnGroupDone)
    }

    private fun pickPayer(memberId: String) {
        viewModel.onEvent(SplitBillUiEvent.OnPayerClicked)
        viewModel.onEvent(SplitBillUiEvent.OnPayerPicked(memberId))
        viewModel.onEvent(SplitBillUiEvent.OnPayerDone)
    }

    private fun toggleInSplit(memberId: String) {
        viewModel.onEvent(SplitBillUiEvent.OnSplitClicked)
        viewModel.onEvent(SplitBillUiEvent.OnPersonToggled(memberId))
        viewModel.onEvent(SplitBillUiEvent.OnSplitDone)
    }

    private val tripExpenses = Route.GroupDetail.expensesOf(TRIP_ID)

    private val tripOverBill = tripExpenses.copy(isOverBill = true)

    private val newRoute = Route.SplitBill(
        TRIP_ID,
        expenseId = null,
        picksGroupFirst = false,
        scansReceipt = false,
        duplicatesExpenseId = null,
    )

    private val fromHomeRoute = newRoute.copy(picksGroupFirst = true)

    private val editRoute = newRoute.copy(expenseId = EXPENSE_ID)

    private val duplicateRoute = newRoute.copy(duplicatesExpenseId = EXPENSE_ID)

    private val billDetail = Route.BillDetail(TRIP_ID, EXPENSE_ID, focusCommentId = null)

    private val editUnderneath = listOf(Route.Groups, tripExpenses, billDetail)

    private val yesterday = Instant.parse("2026-10-29T20:00:00Z").toEpochMilliseconds()

    private val earlier = Instant.parse("2026-10-28T10:00:00Z").toEpochMilliseconds()

    // Why: "Mercado", R$ 312,00 Ana paid yesterday, split equally among Ana, Pierre and Bruno.
    private val savedBill = Expense(
        id = EXPENSE_ID,
        groupId = TRIP_ID,
        description = "Mercado",
        currency = GroupCurrency.BRL,
        amountMinor = 31_200L,
        splitMode = SplitMode.EQUAL,
        payers = listOf(ExpensePayer(ANA, 31_200L)),
        shares = listOf(
            ExpenseShare(ANA, 10_400L, weight = 1L),
            ExpenseShare(PIERRE, 10_400L, weight = 1L),
            ExpenseShare(BRUNO, 10_400L, weight = 1L),
        ),
        items = null,
        categoryId = null,
        spentAt = yesterday,
        isPending = false,
        updatedAt = SAVED_VERSION,
        updatedByMemberId = ANA,
    )

    private val ana = member(ANA, "Ana")
    private val pierre = member(PIERRE, "Pierre", isMe = true)
    private val bruno = member(BRUNO, "Bruno")
    private val carla = member(CARLA, "Carla")
    private val diego = member(DIEGO, "Diego")

    private val trip = group(TRIP_ID, "Viagem Floripa", listOf(ana, pierre, bruno, carla))

    private val home = group(HOME_ID, "Apê 302", listOf(member(PIERRE_HOME, "Pierre", isMe = true), carla))

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
        screen: Route.SplitBill = newRoute,
        underneath: List<Route> = listOf(Route.Groups, tripExpenses),
        updateError: Throwable? = null,
        isSatsUnitRead: Boolean = true,
    ) {
        groups = FakeGroupRepository(initialGroups = listOf(trip, home))
        expenses = FakeExpenseRepository(initialExpenses = listOf(savedBill)).also { it.updateError = updateError }
        val deletedBills = InMemoryDeletedBillRepository()
        navigator = BackStackNavigator(startRoute = underneath.first()).also { navigator ->
            underneath.drop(1).forEach(navigator::navigate)
            navigator.navigate(screen)
        }
        viewModel = SplitBillViewModel(
            paidWithUseCases = paidWithUseCasesOf(),
            trackEvent = { _, _ -> },
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
            observeGroupIncomes = { flowOf(emptyMap()) },
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
            saveSplitAsDefault = { _, _, _, _, _ -> Result.success(Unit) },
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
        // Why: 2026-10-30T15:00:00Z.
        const val NOW = 1_793_372_400_000L
        const val SAVED_VERSION = 1_793_300_000_000L
        const val TRIP_ID = "trip"
        const val HOME_ID = "home"
        const val EXPENSE_ID = "expense-1"
        const val ANA = "ana"
        const val PIERRE = "pierre"
        const val PIERRE_HOME = "pierre-home"
        const val BRUNO = "bruno"
        const val CARLA = "carla"
        const val DIEGO = "diego"
    }
}
