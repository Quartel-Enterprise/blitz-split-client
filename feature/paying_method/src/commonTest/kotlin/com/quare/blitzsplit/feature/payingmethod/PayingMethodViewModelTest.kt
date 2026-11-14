package com.quare.blitzsplit.feature.payingmethod

import com.quare.blitzsplit.core.expenses.domain.model.CardColor
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType
import com.quare.blitzsplit.core.expenses.domain.usecase.SavePayingMethod
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeletePayingMethodUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObservePayingMethodsUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SavePayingMethodUseCase
import com.quare.blitzsplit.core.expenses.testing.FakePayingMethodRepository
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.payingmethod.presentation.model.PayingMethodUiEvent
import com.quare.blitzsplit.feature.payingmethod.presentation.viewmodel.PayingMethodViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PayingMethodViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private lateinit var repository: FakePayingMethodRepository
    private lateinit var showsPix: MutableStateFlow<Boolean>
    private lateinit var route: Route.PayingMethod
    private lateinit var navigator: BackStackNavigator
    private lateinit var heldSave: CompletableDeferred<Unit>
    private lateinit var viewModel: PayingMethodViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Opening

    @Test
    fun `GIVEN no method yet WHEN a new method opens THEN loads on Credit card with the default on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertFalse(state.isEditing)
            assertEquals(PayingMethodType.CREDIT_CARD, state.type)
            assertEquals("", state.name)
            assertTrue(state.isDefault)
        }

    @Test
    fun `GIVEN a method already saved WHEN a new method opens THEN the default starts off`() = runTest(testDispatcher) {
        // Given
        prepareScenario(methods = listOf(card))

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoaded)
        assertFalse(state.isDefault)
    }

    @Test
    fun `GIVEN a saved card WHEN it opens to edit THEN fills its type name color and default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                methods = listOf(card),
                methodId = card.id,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isLoaded)
            assertTrue(state.isEditing)
            assertEquals(PayingMethodType.CREDIT_CARD, state.type)
            assertEquals("Nubank", state.name)
            assertEquals(CardColor.BLUE, state.cardColor)
            assertTrue(state.isDefault)
        }

    @Test
    fun `GIVEN a card being edited WHEN a sync changes that card THEN keeps what is being typed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                methods = listOf(card),
                methodId = card.id,
            )
            viewModel.onEvent(PayingMethodUiEvent.OnNameChanged("Inter"))

            // When
            repository.methods.value = listOf(
                card.copy(
                    name = "Roxinho",
                    cardColor = CardColor.GREEN,
                    isDefault = false,
                ),
            )

            // Then
            val state = viewModel.uiState.value
            assertEquals("Inter", state.name)
            assertEquals(CardColor.BLUE, state.cardColor)
            assertTrue(state.isDefault)
        }

    @Test
    fun `GIVEN the method to edit not read yet WHEN the screen opens THEN waits without closing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                methods = emptyList(),
                methodId = card.id,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isLoaded)
            assertEquals(listOf(Route.Groups, Route.Profile, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the method to edit not read yet WHEN it arrives THEN fills the form`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            methods = emptyList(),
            methodId = card.id,
        )

        // When
        repository.methods.value = listOf(card)

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isLoaded)
        assertEquals("Nubank", state.name)
        assertEquals(CardColor.BLUE, state.cardColor)
    }

    @Test
    fun `GIVEN a card open to edit WHEN another device deletes it THEN closes the screen`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            methods = listOf(card),
            methodId = card.id,
        )

        // When
        repository.methods.value = emptyList()

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
    }

    // Types offered

    @Test
    fun `GIVEN the Pix rule off WHEN a new method opens THEN Pix is not offered`() = runTest(testDispatcher) {
        // Given
        prepareScenario(showsPix = false)

        // When
        val types = viewModel.uiState.value.types

        // Then
        assertFalse(PayingMethodType.PIX in types)
        assertTrue(PayingMethodType.CREDIT_CARD in types)
    }

    @Test
    fun `GIVEN the Pix rule on WHEN a new method opens THEN Pix is offered`() = runTest(testDispatcher) {
        // Given
        prepareScenario(showsPix = true)

        // When
        val types = viewModel.uiState.value.types

        // Then
        assertTrue(PayingMethodType.PIX in types)
    }

    @Test
    fun `GIVEN the Pix rule turned off WHEN a Pix method opens to edit THEN Pix is offered and picked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                methods = listOf(pix),
                methodId = pix.id,
                showsPix = false,
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(PayingMethodType.PIX in state.types)
            assertEquals(PayingMethodType.PIX, state.type)
        }

    @Test
    fun `GIVEN the Pix rule off on an open form WHEN the rule turns on THEN Pix is offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(showsPix = false)

            // When
            showsPix.value = true

            // Then
            assertTrue(PayingMethodType.PIX in viewModel.uiState.value.types)
        }

    @Test
    fun `GIVEN the person holds Cash WHEN a new method opens THEN Cash is not offered`() = runTest(testDispatcher) {
        // Given
        prepareScenario(methods = listOf(cash))

        // When
        val types = viewModel.uiState.value.types

        // Then
        assertFalse(PayingMethodType.CASH in types)
    }

    @Test
    fun `GIVEN no Cash yet WHEN a new method opens THEN Cash is offered`() = runTest(testDispatcher) {
        // Given
        prepareScenario(methods = listOf(card))

        // When
        val types = viewModel.uiState.value.types

        // Then
        assertTrue(PayingMethodType.CASH in types)
    }

    @Test
    fun `GIVEN the Cash method WHEN it opens to edit THEN Cash is offered and picked`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            methods = listOf(cash),
            methodId = cash.id,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(PayingMethodType.CASH in state.types)
        assertEquals(PayingMethodType.CASH, state.type)
    }

    @Test
    fun `GIVEN Cash picked on a new method WHEN a sync brings Cash THEN falls back to the first type`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(PayingMethodUiEvent.OnTypeSelected(PayingMethodType.CASH))

            // When
            repository.methods.value = listOf(cash)

            // Then
            val state = viewModel.uiState.value
            assertFalse(PayingMethodType.CASH in state.types)
            assertEquals(state.types.first(), state.type)
            assertEquals(PayingMethodType.CREDIT_CARD, state.type)
        }

    @Test
    fun `GIVEN Pix picked on a new method WHEN the Pix rule turns off THEN falls back to the first type offered`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(showsPix = true)
            viewModel.onEvent(PayingMethodUiEvent.OnTypeSelected(PayingMethodType.PIX))

            // When
            showsPix.value = false

            // Then
            val state = viewModel.uiState.value
            assertFalse(PayingMethodType.PIX in state.types)
            assertEquals(PayingMethodType.CREDIT_CARD, state.type)
        }

    // Picking a type, a color and the default

    @Test
    fun `GIVEN Credit card picked WHEN picking Debit card THEN picks it and tracks the type`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(PayingMethodUiEvent.OnTypeSelected(PayingMethodType.DEBIT_CARD))

            // Then
            assertEquals(PayingMethodType.DEBIT_CARD, viewModel.uiState.value.type)
            assertEquals(
                listOf("paying_method_type_selected" to mapOf<String, Any>("type" to "debit_card")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN Credit card picked WHEN picking Credit card again THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnTypeSelected(PayingMethodType.CREDIT_CARD))

        // Then
        assertEquals(PayingMethodType.CREDIT_CARD, viewModel.uiState.value.type)
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Pix not offered WHEN picking Pix THEN keeps the type and tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(showsPix = false)

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnTypeSelected(PayingMethodType.PIX))

        // Then
        assertEquals(PayingMethodType.CREDIT_CARD, viewModel.uiState.value.type)
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the purple card color WHEN picking green THEN picks it and tracks the color`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(PayingMethodUiEvent.OnCardColorSelected(CardColor.GREEN))

            // Then
            assertEquals(CardColor.GREEN, viewModel.uiState.value.cardColor)
            assertEquals(listOf("paying_method_card_color_selected"), trackedEvents.map { it.first })
        }

    @Test
    fun `GIVEN the blue card color WHEN picking blue again THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            methods = listOf(card),
            methodId = card.id,
        )

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnCardColorSelected(CardColor.BLUE))

        // Then
        assertEquals(CardColor.BLUE, viewModel.uiState.value.cardColor)
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the default on WHEN turning it off THEN turns it off and tracks it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnDefaultChanged(false))

        // Then
        assertFalse(viewModel.uiState.value.isDefault)
        assertEquals(
            listOf("paying_method_default_toggled" to mapOf<String, Any>("is_default" to false)),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the default on WHEN turning it on again THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnDefaultChanged(true))

        // Then
        assertTrue(viewModel.uiState.value.isDefault)
        assertTrue(trackedEvents.isEmpty())
    }

    // Can save

    @Test
    fun `GIVEN Other without a name WHEN reading the form THEN cannot save`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PayingMethodUiEvent.OnTypeSelected(PayingMethodType.OTHER))

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isNameRequired)
        assertFalse(state.canSave)
    }

    @Test
    fun `GIVEN Other named with spaces only WHEN reading the form THEN cannot save`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PayingMethodUiEvent.OnTypeSelected(PayingMethodType.OTHER))

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnNameChanged("   "))

        // Then
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN Other with a name WHEN reading the form THEN can save`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PayingMethodUiEvent.OnTypeSelected(PayingMethodType.OTHER))

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnNameChanged("Vale"))

        // Then
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN a card without a name WHEN reading the form THEN can save`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isNameRequired)
        assertTrue(state.canSave)
    }

    @Test
    fun `GIVEN a name of 31 characters WHEN reading the form THEN cannot save`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnNameChanged("a".repeat(31)))

        // Then
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN a name of 30 characters WHEN reading the form THEN can save`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnNameChanged("a".repeat(30)))

        // Then
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN Debit card WHEN picked THEN shows the card colors`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnTypeSelected(PayingMethodType.DEBIT_CARD))

        // Then
        assertTrue(viewModel.uiState.value.showsCardColors)
        assertEquals(8, viewModel.uiState.value.cardColors.size)
    }

    @Test
    fun `GIVEN Cash WHEN picked THEN hides the card colors`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnTypeSelected(PayingMethodType.CASH))

        // Then
        assertFalse(viewModel.uiState.value.showsCardColors)
    }

    // Save

    @Test
    fun `GIVEN Other without a name WHEN saving THEN tracks the tap and saves nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PayingMethodUiEvent.OnTypeSelected(PayingMethodType.OTHER))
        trackedEvents.clear()

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnSaveClicked)

        // Then
        assertEquals(listOf("paying_method_save_clicked"), trackedEvents.map { it.first })
        assertTrue(repository.savedWrites.isEmpty())
        assertEquals(listOf(Route.Groups, Route.Profile, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a new card WHEN saving THEN saves it with a new id tracks it and closes`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PayingMethodUiEvent.OnNameChanged("Nubank"))
        viewModel.onEvent(PayingMethodUiEvent.OnCardColorSelected(CardColor.GREEN))
        trackedEvents.clear()

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnSaveClicked)

        // Then
        val saved = repository.methods.value.single()
        assertTrue(saved.id.isNotBlank())
        assertEquals(PayingMethodType.CREDIT_CARD, saved.type)
        assertEquals("Nubank", saved.name)
        assertEquals(CardColor.GREEN, saved.cardColor)
        assertTrue(saved.isDefault)
        assertEquals(
            listOf(
                "paying_method_save_clicked" to emptyMap(),
                "paying_method_saved" to mapOf<String, Any>(
                    "type" to "credit_card",
                    "is_new" to true,
                    "is_default" to true,
                ),
            ),
            trackedEvents,
        )
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a card being edited WHEN saving THEN keeps its id and tracks it as not new`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            methods = listOf(card),
            methodId = card.id,
        )
        viewModel.onEvent(PayingMethodUiEvent.OnNameChanged("Inter"))

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnSaveClicked)

        // Then
        assertEquals(listOf(card.copy(name = "Inter")), repository.methods.value)
        assertEquals(
            mapOf<String, Any>("type" to "credit_card", "is_new" to false, "is_default" to true),
            trackedEvents.single { it.first == "paying_method_saved" }.second,
        )
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a default card WHEN a new default is saved THEN the new one takes over the default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(methods = listOf(card))
            viewModel.onEvent(PayingMethodUiEvent.OnTypeSelected(PayingMethodType.DEBIT_CARD))
            viewModel.onEvent(PayingMethodUiEvent.OnDefaultChanged(true))

            // When
            viewModel.onEvent(PayingMethodUiEvent.OnSaveClicked)

            // Then
            val methods = repository.methods.value
            assertEquals(2, methods.size)
            assertFalse(methods.single { it.id == card.id }.isDefault)
            assertTrue(methods.single { it.id != card.id }.isDefault)
            assertEquals(PayingMethodType.DEBIT_CARD, methods.single { it.id != card.id }.type)
        }

    @Test
    fun `GIVEN Cash picked after a card color WHEN saving THEN saves Cash without a color`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PayingMethodUiEvent.OnCardColorSelected(CardColor.GREEN))
        viewModel.onEvent(PayingMethodUiEvent.OnTypeSelected(PayingMethodType.CASH))

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnSaveClicked)

        // Then
        val saved = repository.methods.value.single()
        assertEquals(PayingMethodType.CASH, saved.type)
        assertNull(saved.cardColor)
    }

    @Test
    fun `GIVEN Cash saving while another device adds Cash WHEN the save ends THEN the form stays open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(holdsSave = true)
            viewModel.onEvent(PayingMethodUiEvent.OnTypeSelected(PayingMethodType.CASH))
            viewModel.onEvent(PayingMethodUiEvent.OnSaveClicked)
            repository.methods.value = listOf(cash)
            trackedEvents.clear()

            // When
            heldSave.complete(Unit)

            // Then
            assertEquals(listOf(cash), repository.methods.value)
            assertFalse(viewModel.uiState.value.isSaving)
            assertTrue(trackedEvents.none { it.first == "paying_method_saved" })
            assertEquals(listOf(Route.Groups, Route.Profile, route), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a save that fails WHEN saving THEN the form stays open and not saving`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        repository.saveError = IllegalStateException("offline disk full")

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnSaveClicked)

        // Then
        assertFalse(viewModel.uiState.value.isSaving)
        assertTrue(viewModel.uiState.value.canSave)
        assertEquals(listOf(Route.Groups, Route.Profile, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a save in progress WHEN saving again THEN tracks the tap and writes once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(holdsSave = true)
        viewModel.onEvent(PayingMethodUiEvent.OnSaveClicked)
        viewModel.onEvent(PayingMethodUiEvent.OnSaveClicked)

        // When
        heldSave.complete(Unit)

        // Then
        assertEquals(1, repository.savedWrites.size)
        assertEquals(2, trackedEvents.count { it.first == "paying_method_save_clicked" })
    }

    // Delete

    @Test
    fun `GIVEN a saved method WHEN tapping delete THEN asks and tracks it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            methods = listOf(card),
            methodId = card.id,
        )

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnDeleteClicked)

        // Then
        assertTrue(viewModel.uiState.value.isDeleteConfirmationShown)
        assertEquals(listOf("paying_method_delete_clicked"), trackedEvents.map { it.first })
    }

    @Test
    fun `GIVEN a new method WHEN tapping delete THEN asks nothing and tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnDeleteClicked)

        // Then
        assertFalse(viewModel.uiState.value.isDeleteConfirmationShown)
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a saved method being saved WHEN tapping delete THEN asks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            methods = listOf(card),
            methodId = card.id,
            holdsSave = true,
        )
        viewModel.onEvent(PayingMethodUiEvent.OnSaveClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnDeleteClicked)

        // Then
        assertFalse(viewModel.uiState.value.isDeleteConfirmationShown)
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the delete question open WHEN dismissing it THEN closes it and tracks the cancel`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                methods = listOf(card),
                methodId = card.id,
            )
            viewModel.onEvent(PayingMethodUiEvent.OnDeleteClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(PayingMethodUiEvent.OnDeleteDismissed)

            // Then
            assertFalse(viewModel.uiState.value.isDeleteConfirmationShown)
            assertEquals(listOf("paying_method_delete_cancelled"), trackedEvents.map { it.first })
            assertEquals(listOf(card), repository.methods.value)
        }

    @Test
    fun `GIVEN no delete question WHEN dismissing it THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            methods = listOf(card),
            methodId = card.id,
        )

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnDeleteDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the delete question open WHEN confirming THEN deletes the method tracks it and closes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                methods = listOf(card, cash),
                methodId = card.id,
            )
            viewModel.onEvent(PayingMethodUiEvent.OnDeleteClicked)
            trackedEvents.clear()

            // When
            viewModel.onEvent(PayingMethodUiEvent.OnDeleteConfirmed)

            // Then
            assertEquals(listOf(cash), repository.methods.value)
            assertEquals(listOf("paying_method_delete_confirmed"), trackedEvents.map { it.first })
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN no delete question WHEN confirming THEN keeps the method and the screen`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            methods = listOf(card),
            methodId = card.id,
        )

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnDeleteConfirmed)

        // Then
        assertEquals(listOf(card), repository.methods.value)
        assertTrue(trackedEvents.isEmpty())
        assertEquals(listOf(Route.Groups, Route.Profile, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a typed name and the delete question open WHEN confirming THEN leaves without asking to discard`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                methods = listOf(card),
                methodId = card.id,
            )
            viewModel.onEvent(PayingMethodUiEvent.OnNameChanged("Inter"))
            viewModel.onEvent(PayingMethodUiEvent.OnDeleteClicked)

            // When
            viewModel.onEvent(PayingMethodUiEvent.OnDeleteConfirmed)

            // Then
            assertFalse(navigator.pendingDiscard.value)
            assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
        }

    // Unsaved input

    @Test
    fun `GIVEN a card as it opened WHEN reading the unsaved input THEN there is none`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            methods = listOf(card),
            methodId = card.id,
        )

        // When
        val hasUnsavedInput = viewModel.uiState.value.hasUnsavedInput

        // Then
        assertFalse(hasUnsavedInput)
        assertFalse(navigator.hasUnsavedInput.value)
    }

    @Test
    fun `GIVEN a new method as it opened WHEN reading the unsaved input THEN there is none`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(methods = listOf(card))

            // When
            val hasUnsavedInput = viewModel.uiState.value.hasUnsavedInput

            // Then
            assertFalse(hasUnsavedInput)
            assertFalse(navigator.hasUnsavedInput.value)
        }

    @Test
    fun `GIVEN a card open to edit WHEN its name changes THEN there is unsaved input`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            methods = listOf(card),
            methodId = card.id,
        )

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnNameChanged("Inter"))

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
        assertTrue(navigator.hasUnsavedInput.value)
    }

    @Test
    fun `GIVEN a new method WHEN another type is picked THEN there is unsaved input`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnTypeSelected(PayingMethodType.BITCOIN))

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedInput)
        assertTrue(navigator.hasUnsavedInput.value)
    }

    @Test
    fun `GIVEN a card open to edit WHEN its name gets spaces around it THEN there is no unsaved input`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                methods = listOf(card),
                methodId = card.id,
            )

            // When
            viewModel.onEvent(PayingMethodUiEvent.OnNameChanged(" Nubank  "))

            // Then
            assertFalse(viewModel.uiState.value.hasUnsavedInput)
        }

    @Test
    fun `GIVEN a typed name WHEN going back THEN asks to discard the changes`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            methods = listOf(card),
            methodId = card.id,
        )
        viewModel.onEvent(PayingMethodUiEvent.OnNameChanged("Inter"))

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnBackClicked)

        // Then
        assertTrue(navigator.pendingDiscard.value)
        assertEquals(listOf(Route.Groups, Route.Profile, route), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a typed name WHEN saving THEN leaves without asking to discard`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            methods = listOf(card),
            methodId = card.id,
        )
        viewModel.onEvent(PayingMethodUiEvent.OnNameChanged("Inter"))

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnSaveClicked)

        // Then
        assertFalse(navigator.pendingDiscard.value)
        assertFalse(navigator.hasUnsavedInput.value)
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
    }

    // Back

    @Test
    fun `GIVEN the form on top WHEN going back THEN closes it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen over the form WHEN going back THEN keeps the back stack`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.EditProfile)

        // When
        viewModel.onEvent(PayingMethodUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile, route, Route.EditProfile), navigator.backStack.value)
    }

    private val card = PayingMethod(
        id = "card-1",
        type = PayingMethodType.CREDIT_CARD,
        name = "Nubank",
        cardColor = CardColor.BLUE,
        isDefault = true,
        addedAt = 1L,
    )

    private val cash = PayingMethod(
        id = "cash-1",
        type = PayingMethodType.CASH,
        name = "",
        cardColor = null,
        isDefault = false,
        addedAt = 2L,
    )

    private val pix = PayingMethod(
        id = "pix-1",
        type = PayingMethodType.PIX,
        name = "Itaú",
        cardColor = null,
        isDefault = false,
        addedAt = 3L,
    )

    private fun prepareScenario(
        methods: List<PayingMethod> = emptyList(),
        methodId: String? = null,
        showsPix: Boolean = true,
        holdsSave: Boolean = false,
    ) {
        repository = FakePayingMethodRepository(initialMethods = methods)
        this.showsPix = MutableStateFlow(showsPix)
        route = Route.PayingMethod(methodId)
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.Profile)
            it.navigate(route)
        }
        heldSave = CompletableDeferred()
        if (!holdsSave) heldSave.complete(Unit)
        val save = SavePayingMethodUseCase(repository, currentTimeMillis = { 42L })
        viewModel = PayingMethodViewModel(
            route = route,
            navigator = navigator,
            savePayingMethod = SavePayingMethod { id, type, name, cardColor, isDefault ->
                heldSave.await()
                save(id, type, name, cardColor, isDefault)
            },
            deletePayingMethod = DeletePayingMethodUseCase(repository),
            observePayingMethods = ObservePayingMethodsUseCase(repository),
            observeShowsPix = { this.showsPix },
            trackEvent = { name, params -> trackedEvents += name to params },
        )
    }
}
