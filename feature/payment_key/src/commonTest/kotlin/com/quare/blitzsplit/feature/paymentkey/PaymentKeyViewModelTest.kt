package com.quare.blitzsplit.feature.paymentkey

import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyError
import com.quare.blitzsplit.core.model.PaymentKeyFormat
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.model.PaymentMethod
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePaymentKeys
import com.quare.blitzsplit.core.profile.domain.usecase.SavePaymentKey
import com.quare.blitzsplit.core.profile.domain.usecase.impl.DeletePaymentKeyUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.DetectPaymentKeyTypeUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.FormatPaymentKeyInputUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.FormatPaymentKeyUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObservePaymentKeysUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.SavePaymentKeyUseCase
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ValidatePaymentKeyUseCase
import com.quare.blitzsplit.core.profile.testing.FakePaymentKeyRepository
import com.quare.blitzsplit.feature.paymentkey.presentation.model.PaymentKeyUiEvent
import com.quare.blitzsplit.feature.paymentkey.presentation.viewmodel.PaymentKeyViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaymentKeyViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakePaymentKeyRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: PaymentKeyViewModel
    private var saveCalls = 0

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a new Pix key WHEN the screen opens THEN starts on CPF and offers only the Pix types`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(method = PaymentMethod.PIX)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(PaymentKeyType.CPF, state.type)
            assertEquals(
                listOf(PaymentKeyType.CPF, PaymentKeyType.PHONE, PaymentKeyType.EMAIL, PaymentKeyType.RANDOM),
                state.types,
            )
            assertFalse(state.isEditing)
            assertEquals("", state.input)
        }

    @Test
    fun `GIVEN a new Bitcoin key WHEN the screen opens THEN starts on Lightning and offers only the Bitcoin types`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(method = PaymentMethod.BITCOIN)

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(PaymentKeyType.LIGHTNING, state.type)
            assertEquals(listOf(PaymentKeyType.LIGHTNING, PaymentKeyType.ONCHAIN), state.types)
        }

    @Test
    fun `GIVEN the keys not read yet WHEN the screen opens THEN is not loaded`() = runTest(testDispatcher) {
        // Given
        prepareScenario(keysRead = false)

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoaded)
    }

    @Test
    fun `GIVEN the keys not read yet WHEN typing a valid CPF THEN can not save`() = runTest(testDispatcher) {
        // Given
        prepareScenario(keysRead = false)

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(VALID_CPF))

        // Then
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `GIVEN the keys read WHEN the screen opens THEN is loaded`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isLoaded)
    }

    @Test
    fun `GIVEN a new Pix key WHEN selecting a Bitcoin type THEN keeps the type`() = runTest(testDispatcher) {
        // Given
        prepareScenario(method = PaymentMethod.PIX)

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnTypeSelected(PaymentKeyType.ONCHAIN))

        // Then
        assertEquals(PaymentKeyType.CPF, viewModel.uiState.value.type)
    }

    @Test
    fun `GIVEN a saved CPF WHEN editing it THEN fills the form with the CPF as people read it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                keyId = "key-1",
                storedKeys = listOf(
                    key(
                        id = "key-1",
                        value = VALID_CPF,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isEditing)
            assertEquals(PaymentKeyType.CPF, state.type)
            assertEquals("123.456.789-09", state.input)
            assertTrue(state.isDefault)
            assertEquals(PaymentKeyFormat.CPF, state.format)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN a saved phone WHEN editing it THEN fills the input with the masked phone`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            keyId = "key-1",
            storedKeys = listOf(
                key(
                    id = "key-1",
                    type = PaymentKeyType.PHONE,
                    value = "+5511912345678",
                ),
            ),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(PaymentKeyType.PHONE, state.type)
        assertEquals("+55 (11) 91234-5678", state.input)
    }

    @Test
    fun `GIVEN a saved key no longer valid WHEN editing it THEN shows its error right away`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                keyId = "key-1",
                storedKeys = listOf(
                    key(
                        id = "key-1",
                        value = WRONG_CPF,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(PaymentKeyError.INVALID_CPF, state.error)
            assertFalse(state.canSave)
        }

    @Test
    fun `GIVEN a saved key not the default WHEN editing it THEN fills the default switch off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                keyId = "key-1",
                storedKeys = listOf(
                    key(
                        id = "key-1",
                        value = VALID_CPF,
                        isDefault = false,
                    ),
                    key(
                        id = "key-2",
                        type = PaymentKeyType.PHONE,
                        value = "+5511912345678",
                        isDefault = true,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isDefault)
        }

    @Test
    fun `GIVEN a key being edited WHEN a sync brings the keys again THEN keeps what was typed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                keyId = "key-1",
                storedKeys = listOf(
                    key(
                        id = "key-1",
                        value = VALID_CPF,
                    ),
                ),
            )
            viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(OTHER_VALID_CPF))

            // When
            repository.keys.value = listOf(
                key(
                    id = "key-1",
                    type = PaymentKeyType.EMAIL,
                    value = "pierre@email.com",
                ),
                key(
                    id = "key-2",
                    type = PaymentKeyType.PHONE,
                    value = "+5511912345678",
                    isDefault = false,
                ),
            )

            // Then
            assertEquals(PaymentKeyType.CPF, viewModel.uiState.value.type)
            assertEquals("529.982.247-25", viewModel.uiState.value.input)
        }

    @Test
    fun `GIVEN a key that does not exist WHEN editing it THEN navigates back`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            keyId = "key-1",
            storedKeys = listOf(key(id = "key-2")),
        )

        // When
        val backStack = navigator.backStack.value

        // Then
        assertEquals(profileStack, backStack)
    }

    @Test
    fun `GIVEN a key being edited WHEN it is deleted on another device THEN navigates back`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                keyId = "key-1",
                storedKeys = listOf(key(id = "key-1")),
            )

            // When
            repository.keys.value = emptyList()

            // Then
            assertEquals(profileStack, navigator.backStack.value)
        }

    @Test
    fun `GIVEN the edited key gone WHEN the keys come again THEN navigates back only once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            keyId = "key-1",
            storedKeys = listOf(key(id = "key-1")),
        )
        repository.keys.value = emptyList()

        // When
        repository.keys.value = listOf(key(id = "key-2"))

        // Then
        assertEquals(profileStack, navigator.backStack.value)
    }

    @Test
    fun `GIVEN CPF WHEN typing 11 digits THEN masks them as a CPF`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("12345678909"))

        // Then
        assertEquals("123.456.789-09", viewModel.uiState.value.input)
    }

    @Test
    fun `GIVEN phone WHEN typing its digits THEN masks them as a phone`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PaymentKeyUiEvent.OnTypeSelected(PaymentKeyType.PHONE))

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("11912345678"))

        // Then
        assertEquals("+55 (11) 91234-5678", viewModel.uiState.value.input)
        assertEquals(PaymentKeyFormat.PHONE, viewModel.uiState.value.format)
    }

    @Test
    fun `GIVEN a complete CPF WHEN erasing the dash THEN leaves one digit less`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("12345678909"))

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("123.456.78909"))

        // Then
        val input = viewModel.uiState.value.input
        assertNotEquals("123.456.789-09", input)
        assertEquals(10, input.count(Char::isDigit))
    }

    @Test
    fun `GIVEN a complete CPF WHEN erasing the dash THEN removes the digit before it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("12345678909"))

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("123.456.78909"))

        // Then
        assertEquals("123.456.780-9", viewModel.uiState.value.input)
    }

    @Test
    fun `GIVEN CPF WHEN typing an email THEN switches to email`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("pierre@email.com"))

        // Then
        val state = viewModel.uiState.value
        assertEquals(PaymentKeyType.EMAIL, state.type)
        assertEquals("pierre@email.com", state.input)
        assertEquals(PaymentKeyFormat.EMAIL, state.format)
        assertFalse(state.hasSwitchedNetwork)
    }

    @Test
    fun `GIVEN CPF WHEN pasting an email THEN switches to email`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnPasted("pierre@email.com"))

        // Then
        assertEquals(PaymentKeyType.EMAIL, viewModel.uiState.value.type)
        assertEquals("pierre@email.com", viewModel.uiState.value.input)
        assertFalse(viewModel.uiState.value.hasSwitchedNetwork)
    }

    @Test
    fun `GIVEN Lightning WHEN typing a bc1 address THEN switches to on-chain and says the network changed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(method = PaymentMethod.BITCOIN)

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(SEGWIT))

            // Then
            val state = viewModel.uiState.value
            assertEquals(PaymentKeyType.ONCHAIN, state.type)
            assertEquals(PaymentKeyFormat.SEGWIT, state.format)
            assertTrue(state.hasSwitchedNetwork)
        }

    @Test
    fun `GIVEN on-chain WHEN pasting a Lightning Address THEN switches to Lightning and says the network changed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(method = PaymentMethod.BITCOIN)
            viewModel.onEvent(PaymentKeyUiEvent.OnTypeSelected(PaymentKeyType.ONCHAIN))

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnPasted("pierre@getalby.com"))

            // Then
            val state = viewModel.uiState.value
            assertEquals(PaymentKeyType.LIGHTNING, state.type)
            assertEquals(PaymentKeyFormat.LIGHTNING_ADDRESS, state.format)
            assertTrue(state.hasSwitchedNetwork)
        }

    @Test
    fun `GIVEN on-chain WHEN pasting a Taproot address THEN shows its format and stays on on-chain`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(method = PaymentMethod.BITCOIN)
            viewModel.onEvent(PaymentKeyUiEvent.OnTypeSelected(PaymentKeyType.ONCHAIN))

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnPasted(TAPROOT))

            // Then
            assertEquals(PaymentKeyType.ONCHAIN, viewModel.uiState.value.type)
            assertEquals(PaymentKeyFormat.TAPROOT, viewModel.uiState.value.format)
            assertFalse(viewModel.uiState.value.hasSwitchedNetwork)
        }

    @Test
    fun `GIVEN the network switched WHEN picking a type THEN no longer says the network changed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(method = PaymentMethod.BITCOIN)
            viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(SEGWIT))

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnTypeSelected(PaymentKeyType.LIGHTNING))

            // Then
            assertEquals(PaymentKeyType.LIGHTNING, viewModel.uiState.value.type)
            assertFalse(viewModel.uiState.value.hasSwitchedNetwork)
        }

    @Test
    fun `GIVEN phone WHEN typing 11 digits that are also a valid CPF THEN stays on phone`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PaymentKeyUiEvent.OnTypeSelected(PaymentKeyType.PHONE))

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(PHONE_AND_CPF))

        // Then
        assertEquals(PaymentKeyType.PHONE, viewModel.uiState.value.type)
        assertEquals(PaymentKeyFormat.PHONE, viewModel.uiState.value.format)
    }

    @Test
    fun `GIVEN CPF WHEN typing 11 digits that are also a valid phone THEN stays on CPF`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(PHONE_AND_CPF))

        // Then
        assertEquals(PaymentKeyType.CPF, viewModel.uiState.value.type)
        assertEquals(PaymentKeyFormat.CPF, viewModel.uiState.value.format)
    }

    @Test
    fun `GIVEN CPF WHEN typing half a CPF THEN shows no error yet`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("123456"))

        // Then
        assertEquals("123.456", viewModel.uiState.value.input)
        assertNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.format)
    }

    @Test
    fun `GIVEN CPF WHEN pasting half a CPF THEN shows the CPF error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnPasted("123456"))

        // Then
        assertEquals(PaymentKeyError.INVALID_CPF, viewModel.uiState.value.error)
    }

    @Test
    fun `GIVEN half a CPF typed WHEN leaving the field THEN shows the CPF error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("123456"))

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputFocusLost)

        // Then
        assertEquals(PaymentKeyError.INVALID_CPF, viewModel.uiState.value.error)
    }

    @Test
    fun `GIVEN the field left blank WHEN typing half a CPF THEN shows no error yet`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PaymentKeyUiEvent.OnInputFocusLost)

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("123456"))

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `GIVEN CPF WHEN typing every digit of a wrong CPF THEN shows the CPF error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(WRONG_CPF))

        // Then
        assertEquals(PaymentKeyError.INVALID_CPF, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.format)
    }

    @Test
    fun `GIVEN phone WHEN typing every digit of a wrong phone THEN shows the phone error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PaymentKeyUiEvent.OnTypeSelected(PaymentKeyType.PHONE))

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("11812345678"))

        // Then
        assertEquals(PaymentKeyError.INVALID_PHONE, viewModel.uiState.value.error)
    }

    @Test
    fun `GIVEN random key WHEN typing 31 characters THEN shows no error yet`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PaymentKeyUiEvent.OnTypeSelected(PaymentKeyType.RANDOM))

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("123e4567e89b12d3a45642661417400"))

        // Then
        assertEquals(PaymentKeyType.RANDOM, viewModel.uiState.value.type)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `GIVEN random key WHEN typing 32 characters of a wrong key THEN shows the random key error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(PaymentKeyUiEvent.OnTypeSelected(PaymentKeyType.RANDOM))

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("123e4567e89b12d3a45642661417400g"))

            // Then
            assertEquals(PaymentKeyError.INVALID_RANDOM, viewModel.uiState.value.error)
        }

    @Test
    fun `GIVEN on-chain WHEN typing the start of a testnet address THEN shows the testnet error right away`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(method = PaymentMethod.BITCOIN)
            viewModel.onEvent(PaymentKeyUiEvent.OnTypeSelected(PaymentKeyType.ONCHAIN))

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("tb1qw508"))

            // Then
            assertEquals(PaymentKeyError.TESTNET_ADDRESS, viewModel.uiState.value.error)
        }

    @Test
    fun `GIVEN Lightning WHEN typing an invoice THEN shows the invoice error right away`() = runTest(testDispatcher) {
        // Given
        prepareScenario(method = PaymentMethod.BITCOIN)

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(INVOICE))

        // Then
        assertEquals(PaymentKeyType.LIGHTNING, viewModel.uiState.value.type)
        assertEquals(PaymentKeyError.LIGHTNING_INVOICE, viewModel.uiState.value.error)
    }

    @Test
    fun `GIVEN Lightning WHEN typing half a Lightning Address THEN shows no error yet`() = runTest(testDispatcher) {
        // Given
        prepareScenario(method = PaymentMethod.BITCOIN)

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("pierre@get"))

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `GIVEN a saved email WHEN typing the same email for a new key THEN shows the already saved error right away`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedKeys = listOf(
                    key(
                        id = "key-2",
                        type = PaymentKeyType.EMAIL,
                        value = EMAIL,
                    ),
                ),
            )
            viewModel.onEvent(PaymentKeyUiEvent.OnTypeSelected(PaymentKeyType.EMAIL))

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("Pierre@Email.com"))

            // Then
            assertEquals(PaymentKeyError.ALREADY_SAVED, viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.canSave)
        }

    @Test
    fun `GIVEN a saved Lightning Address WHEN typing the same email as a Pix key THEN shows no error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                storedKeys = listOf(
                    key(
                        id = "key-2",
                        type = PaymentKeyType.LIGHTNING,
                        value = EMAIL,
                    ),
                ),
            )
            viewModel.onEvent(PaymentKeyUiEvent.OnTypeSelected(PaymentKeyType.EMAIL))

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnPasted(EMAIL))

            // Then
            assertNull(viewModel.uiState.value.error)
            assertEquals(PaymentKeyFormat.EMAIL, viewModel.uiState.value.format)
        }

    @Test
    fun `GIVEN a saved email being edited WHEN the screen opens THEN does not count it as already saved`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                keyId = "key-1",
                storedKeys = listOf(
                    key(
                        id = "key-1",
                        type = PaymentKeyType.EMAIL,
                        value = EMAIL,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.error)
            assertEquals(PaymentKeyFormat.EMAIL, state.format)
            assertTrue(state.canSave)
        }

    @Test
    fun `GIVEN an error shown WHEN clearing the field THEN hides the error`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("123456"))
        viewModel.onEvent(PaymentKeyUiEvent.OnInputFocusLost)

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(""))

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `GIVEN the field cleared after an error WHEN typing half a CPF again THEN shows no error yet`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(PaymentKeyUiEvent.OnPasted("123456"))
            viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(""))

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("123"))

            // Then
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `GIVEN CPF WHEN typing a valid CPF THEN shows its format and can save`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(VALID_CPF))

        // Then
        val state = viewModel.uiState.value
        assertEquals(PaymentKeyFormat.CPF, state.format)
        assertNull(state.error)
        assertTrue(state.canSave)
    }

    @Test
    fun `GIVEN CPF with no error WHEN the screen opens THEN shows the CPF notice`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showsCpfNotice)
    }

    @Test
    fun `GIVEN a wrong CPF typed WHEN reading the state THEN hides the CPF notice`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(WRONG_CPF))

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.showsCpfNotice)
    }

    @Test
    fun `GIVEN email selected WHEN reading the state THEN hides the CPF notice`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PaymentKeyUiEvent.OnTypeSelected(PaymentKeyType.EMAIL))

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.showsCpfNotice)
    }

    @Test
    fun `GIVEN no Pix key yet WHEN adding one THEN it is the locked default`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isDefault)
        assertTrue(state.isDefaultLocked)
        assertNull(state.currentDefaultType)
    }

    @Test
    fun `GIVEN the first Pix key WHEN turning the default off THEN stays default`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnDefaultChanged(false))

        // Then
        assertTrue(viewModel.uiState.value.isDefault)
    }

    @Test
    fun `GIVEN another Pix key is the default WHEN adding one THEN starts off and shows the default type`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedKeys = listOf(phoneDefault))

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isDefault)
            assertFalse(state.isDefaultLocked)
            assertEquals(PaymentKeyType.PHONE, state.currentDefaultType)
        }

    @Test
    fun `GIVEN another Pix key is the default WHEN turning the default on THEN makes it default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedKeys = listOf(phoneDefault))

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnDefaultChanged(true))

            // Then
            assertTrue(viewModel.uiState.value.isDefault)
        }

    @Test
    fun `GIVEN the default turned on WHEN a sync brings the keys again THEN keeps it on`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storedKeys = listOf(phoneDefault))
        viewModel.onEvent(PaymentKeyUiEvent.OnDefaultChanged(true))

        // When
        repository.keys.value =
            listOf(
                phoneDefault,
                key(
                    id = "key-3",
                    type = PaymentKeyType.EMAIL,
                    value = EMAIL,
                    isDefault = false,
                ),
            )

        // Then
        assertTrue(viewModel.uiState.value.isDefault)
    }

    @Test
    fun `GIVEN the default key WHEN editing it THEN the default is locked on with no current default type`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                keyId = "key-1",
                storedKeys = listOf(
                    key(
                        id = "key-1",
                        value = VALID_CPF,
                        isDefault = true,
                    ),
                    key(
                        id = "key-2",
                        type = PaymentKeyType.PHONE,
                        value = "+5511912345678",
                        isDefault = false,
                    ),
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertTrue(state.isDefault)
            assertTrue(state.isDefaultLocked)
            assertNull(state.currentDefaultType)
        }

    @Test
    fun `GIVEN the default key being edited WHEN turning the default off THEN stays default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                keyId = "key-1",
                storedKeys = listOf(
                    key(
                        id = "key-1",
                        value = VALID_CPF,
                        isDefault = true,
                    ),
                    key(
                        id = "key-2",
                        type = PaymentKeyType.PHONE,
                        value = "+5511912345678",
                        isDefault = false,
                    ),
                ),
            )

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnDefaultChanged(false))

            // Then
            assertTrue(viewModel.uiState.value.isDefault)
        }

    @Test
    fun `GIVEN a key that is not the default WHEN editing it THEN shows the type of the current default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                keyId = "key-1",
                storedKeys = listOf(
                    key(
                        id = "key-1",
                        value = VALID_CPF,
                        isDefault = false,
                    ),
                    phoneDefault,
                ),
            )

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.isDefault)
            assertFalse(state.isDefaultLocked)
            assertEquals(PaymentKeyType.PHONE, state.currentDefaultType)
        }

    @Test
    fun `GIVEN only Bitcoin keys WHEN adding a Pix key THEN it is the locked default`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            storedKeys = listOf(
                key(
                    id = "key-2",
                    type = PaymentKeyType.LIGHTNING,
                    value = "pierre@getalby.com",
                ),
            ),
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isDefault)
        assertTrue(state.isDefaultLocked)
        assertNull(state.currentDefaultType)
    }

    @Test
    fun `GIVEN a valid new CPF WHEN saving THEN stores the normalized CPF as a new default key`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("123.456.789-09"))

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnSaveClicked)

            // Then
            val saved = repository.savedKeys.single().single()
            assertTrue(saved.id.isNotBlank())
            assertEquals(PaymentKeyType.CPF, saved.type)
            assertEquals(VALID_CPF, saved.value)
            assertTrue(saved.isDefault)
        }

    @Test
    fun `GIVEN a valid new key WHEN saving THEN navigates back`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(VALID_CPF))

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnSaveClicked)

        // Then
        assertEquals(profileStack, navigator.backStack.value)
    }

    @Test
    fun `GIVEN another default and the default left off WHEN saving THEN stores the key not default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedKeys = listOf(phoneDefault))
            viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(VALID_CPF))

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnSaveClicked)

            // Then
            val saved = repository.savedKeys.single().single()
            assertNotEquals(phoneDefault.id, saved.id)
            assertFalse(saved.isDefault)
        }

    @Test
    fun `GIVEN another default and the default turned on WHEN saving THEN stores the key as default`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storedKeys = listOf(phoneDefault))
            viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(VALID_CPF))
            viewModel.onEvent(PaymentKeyUiEvent.OnDefaultChanged(true))

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnSaveClicked)

            // Then
            val saved = repository.savedKeys.single().first { it.type == PaymentKeyType.CPF }
            assertTrue(saved.isDefault)
        }

    @Test
    fun `GIVEN a key being edited WHEN saving THEN stores it under the same id`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            keyId = "key-1",
            storedKeys = listOf(
                key(
                    id = "key-1",
                    value = VALID_CPF,
                ),
            ),
        )
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(OTHER_VALID_CPF))

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnSaveClicked)

        // Then
        val saved = repository.savedKeys.single().single()
        assertEquals("key-1", saved.id)
        assertEquals(OTHER_VALID_CPF, saved.value)
        assertEquals(profileStack, navigator.backStack.value)
    }

    @Test
    fun `GIVEN half a CPF WHEN saving THEN stores nothing and stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged("123456"))

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnSaveClicked)

        // Then
        assertTrue(repository.savedKeys.isEmpty())
        assertEquals(profileStack + paymentKeyRoute(), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a save in progress WHEN reading the state THEN is saving and can not save`() = runTest(testDispatcher) {
        // Given
        prepareScenario(saveGate = CompletableDeferred())
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(VALID_CPF))
        viewModel.onEvent(PaymentKeyUiEvent.OnSaveClicked)

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.isSaving)
        assertFalse(state.canSave)
    }

    @Test
    fun `GIVEN a save in progress WHEN saving again THEN saves the key once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(saveGate = CompletableDeferred())
        viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(VALID_CPF))
        viewModel.onEvent(PaymentKeyUiEvent.OnSaveClicked)

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnSaveClicked)

        // Then
        assertEquals(1, saveCalls)
    }

    @Test
    fun `GIVEN a new key WHEN clicking delete THEN shows no confirmation`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnDeleteClicked)

        // Then
        assertFalse(viewModel.uiState.value.isDeleteConfirmationShown)
    }

    @Test
    fun `GIVEN a key being edited WHEN clicking delete THEN asks for confirmation`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            keyId = "key-1",
            storedKeys = listOf(key(id = "key-1")),
        )

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnDeleteClicked)

        // Then
        assertTrue(viewModel.uiState.value.isDeleteConfirmationShown)
        assertTrue(repository.deletedIds.isEmpty())
    }

    @Test
    fun `GIVEN the confirmation shown WHEN dismissing it THEN hides it and deletes nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                keyId = "key-1",
                storedKeys = listOf(key(id = "key-1")),
            )
            viewModel.onEvent(PaymentKeyUiEvent.OnDeleteClicked)

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnDeleteDismissed)

            // Then
            assertFalse(viewModel.uiState.value.isDeleteConfirmationShown)
            assertTrue(repository.deletedIds.isEmpty())
        }

    @Test
    fun `GIVEN no confirmation shown WHEN confirming the deletion THEN deletes nothing and stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                keyId = "key-1",
                storedKeys = listOf(key(id = "key-1")),
            )

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnDeleteConfirmed)

            // Then
            assertTrue(repository.deletedIds.isEmpty())
            assertEquals(profileStack + paymentKeyRoute(keyId = "key-1"), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the confirmation shown WHEN confirming the deletion THEN deletes the key and navigates back once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                keyId = "key-1",
                storedKeys = listOf(key(id = "key-1"), key(id = "key-2")),
            )
            viewModel.onEvent(PaymentKeyUiEvent.OnDeleteClicked)

            // When
            viewModel.onEvent(PaymentKeyUiEvent.OnDeleteConfirmed)

            // Then
            assertEquals(listOf("key-1"), repository.deletedIds)
            assertEquals(profileStack, navigator.backStack.value)
        }

    @Test
    fun `GIVEN the screen open WHEN clicking back THEN navigates back`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(PaymentKeyUiEvent.OnBackClicked)

        // Then
        assertEquals(profileStack, navigator.backStack.value)
    }

    @Test
    fun `GIVEN back clicked WHEN the edited key is deleted on another device THEN does not navigate back again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                keyId = "key-1",
                storedKeys = listOf(key(id = "key-1")),
            )
            viewModel.onEvent(PaymentKeyUiEvent.OnBackClicked)

            // When
            repository.keys.value = emptyList()

            // Then
            assertEquals(profileStack, navigator.backStack.value)
        }

    @Test
    fun `GIVEN a save under way WHEN a native back closes the screen before it ends THEN keeps the profile open`() =
        runTest(testDispatcher) {
            // Given
            val gate = CompletableDeferred<Unit>()
            prepareScenario(saveGate = gate)
            viewModel.onEvent(PaymentKeyUiEvent.OnInputChanged(VALID_CPF))
            viewModel.onEvent(PaymentKeyUiEvent.OnSaveClicked)
            navigator.navigateBack()

            // When
            gate.complete(Unit)
            runCurrent()

            // Then
            assertEquals(profileStack, navigator.backStack.value)
        }

    private val profileStack: List<Route> = listOf(Route.Groups, Route.Profile)

    private val phoneDefault = PaymentKey(
        id = "key-2",
        type = PaymentKeyType.PHONE,
        value = "+5511912345678",
        isDefault = true,
        addedAt = 1L,
    )

    private fun key(
        id: String,
        type: PaymentKeyType = PaymentKeyType.CPF,
        value: String = VALID_CPF,
        isDefault: Boolean = true,
    ): PaymentKey = PaymentKey(
        id = id,
        type = type,
        value = value,
        isDefault = isDefault,
        addedAt = 1L,
    )

    private fun paymentKeyRoute(
        method: PaymentMethod = PaymentMethod.PIX,
        keyId: String? = null,
    ): Route = Route.PaymentKey(
        method = method,
        keyId = keyId,
    )

    private fun prepareScenario(
        method: PaymentMethod = PaymentMethod.PIX,
        keyId: String? = null,
        storedKeys: List<PaymentKey> = emptyList(),
        keysRead: Boolean = true,
        saveGate: CompletableDeferred<Unit>? = null,
    ) {
        val route = Route.PaymentKey(
            method = method,
            keyId = keyId,
        )
        repository = FakePaymentKeyRepository(storedKeys)
        navigator = BackStackNavigator(startRoute = Route.Groups).also {
            it.navigate(Route.Profile)
            it.navigate(route)
        }
        val save = SavePaymentKeyUseCase(repository) { NOW }
        viewModel = PaymentKeyViewModel(
            route = route,
            navigator = navigator,
            validatePaymentKey = ValidatePaymentKeyUseCase(),
            detectPaymentKeyType = DetectPaymentKeyTypeUseCase(),
            formatPaymentKeyInput = FormatPaymentKeyInputUseCase(),
            formatPaymentKey = FormatPaymentKeyUseCase(),
            savePaymentKey = SavePaymentKey { id, type, value, isDefault ->
                saveCalls++
                saveGate?.await()
                save(id, type, value, isDefault)
            },
            deletePaymentKey = DeletePaymentKeyUseCase(repository),
            observePaymentKeys = if (keysRead) {
                ObservePaymentKeysUseCase(
                    repository,
                )
            } else {
                ObservePaymentKeys { emptyFlow() }
            },
        )
    }

    companion object {
        const val NOW = 1_000L
        const val VALID_CPF = "12345678909"
        const val OTHER_VALID_CPF = "52998224725"
        const val WRONG_CPF = "12345678900"

        // Why: a valid CPF that is also a valid phone (DDD 11, then a 9).
        const val PHONE_AND_CPF = "11912345684"
        const val EMAIL = "pierre@email.com"
        const val SEGWIT = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
        const val TAPROOT = "bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0"
        const val INVOICE = "lnbc2500u1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypq"
    }
}
