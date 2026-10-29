package com.quare.blitzsplit.feature.recoverycode

import com.quare.blitzsplit.core.account.domain.model.AbandonedBrowserSignIn
import com.quare.blitzsplit.core.account.domain.model.ConnectResult
import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithBrowserUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithIdTokenUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAbandonedBrowserSignInUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveReturningBrowserSignInUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.UpdateRecoveryCodePromptUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.connect.ProviderConnectorImpl
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.feature.recoverycode.presentation.model.RecoveryCodeUiAction
import com.quare.blitzsplit.feature.recoverycode.presentation.model.RecoveryCodeUiEvent
import com.quare.blitzsplit.feature.recoverycode.presentation.viewmodel.RecoveryCodeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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

class RecoveryCodeViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var viewModel: RecoveryCodeViewModel
    private val actions = mutableListOf<RecoveryCodeUiAction>()
    private var completedSignIns = 0

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN an issued code WHEN observing THEN shows it grouped and the prompt as due`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            code = "K7QM4XPA9TRE",
            prompt = RecoveryCodePrompt.DUE,
        )

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals("K7QM-4XPA-9TRE", state.formattedCode)
        assertTrue(state.isPromptDue)
    }

    @Test
    fun `GIVEN the code WHEN copying THEN asks the platform to copy the grouped code`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            code = "K7QM4XPA9TRE",
            prompt = RecoveryCodePrompt.DUE,
        )

        // When
        viewModel.onEvent(RecoveryCodeUiEvent.OnCopyClicked)

        // Then
        assertEquals(listOf<RecoveryCodeUiAction>(RecoveryCodeUiAction.CopyToClipboard("K7QM-4XPA-9TRE")), actions)
    }

    @Test
    fun `GIVEN the sheet WHEN reminding later THEN snoozes the prompt`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            code = "K7QM4XPA9TRE",
            prompt = RecoveryCodePrompt.DUE,
        )

        // When
        viewModel.onEvent(RecoveryCodeUiEvent.OnRemindLaterClicked)

        // Then
        assertEquals(RecoveryCodePrompt.SNOOZED, account.recoveryCodePrompt)
    }

    @Test
    fun `GIVEN a saved code opened for review WHEN reminding later THEN keeps it saved`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            code = "K7QM4XPA9TRE",
            prompt = RecoveryCodePrompt.REVIEW,
        )

        // When
        viewModel.onEvent(RecoveryCodeUiEvent.OnRemindLaterClicked)

        // Then
        assertEquals(RecoveryCodePrompt.SAVED, account.recoveryCodePrompt)
    }

    @Test
    fun `GIVEN the sheet WHEN confirming it was saved THEN settles the prompt`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            code = "K7QM4XPA9TRE",
            prompt = RecoveryCodePrompt.DUE,
        )

        // When
        viewModel.onEvent(RecoveryCodeUiEvent.OnSavedClicked)

        // Then
        assertEquals(RecoveryCodePrompt.SAVED, account.recoveryCodePrompt)
    }

    @Test
    fun `GIVEN Apple is native WHEN choosing to connect it instead THEN asks the platform for a token`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                code = "K7QM4XPA9TRE",
                prompt = RecoveryCodePrompt.DUE,
                native = setOf(AccountProvider.APPLE),
            )

            // When
            viewModel.onEvent(RecoveryCodeUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // Then
            assertEquals(
                listOf<RecoveryCodeUiAction>(RecoveryCodeUiAction.LaunchNativeSignIn(AccountProvider.APPLE)),
                actions,
            )
        }

    @Test
    fun `GIVEN the code WHEN saving to the password manager THEN hands the grouped code to the platform`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                code = "K7QM4XPA9TRE",
                prompt = RecoveryCodePrompt.DUE,
            )

            // When
            viewModel.onEvent(RecoveryCodeUiEvent.OnSaveToPasswordManagerClicked)

            // Then
            assertEquals(
                listOf<RecoveryCodeUiAction>(RecoveryCodeUiAction.SaveToPasswordManager("K7QM-4XPA-9TRE")),
                actions,
            )
        }

    @Test
    fun `GIVEN the sheet WHEN choosing to connect an account instead THEN shows one button per provider`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                code = "K7QM4XPA9TRE",
                prompt = RecoveryCodePrompt.DUE,
            )

            // When
            viewModel.onEvent(RecoveryCodeUiEvent.OnConnectInsteadClicked)

            // Then
            assertTrue(viewModel.uiState.value.isConnectExpanded)
        }

    @Test
    fun `GIVEN no code yet WHEN copying THEN does nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            code = null,
            prompt = RecoveryCodePrompt.NOT_DUE,
        )

        // When
        viewModel.onEvent(RecoveryCodeUiEvent.OnCopyClicked)

        // Then
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN Google is not native WHEN choosing to connect it THEN starts the browser sign in`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                code = "K7QM4XPA9TRE",
                prompt = RecoveryCodePrompt.DUE,
            )

            // When
            viewModel.onEvent(RecoveryCodeUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // Then
            assertEquals(listOf(AccountProvider.GOOGLE), account.browserConnections)
        }

    @Test
    fun `GIVEN a browser sign in WHEN its callback comes back failed THEN shows the error and enables them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                code = "K7QM4XPA9TRE",
                prompt = RecoveryCodePrompt.DUE,
            )
            viewModel.onEvent(RecoveryCodeUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // When
            account.abandonedBrowserSignIns.emit(AbandonedBrowserSignIn.FAILED)

            // Then
            assertTrue(viewModel.uiState.value.hasSignInFailed)
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN a browser sign in WHEN the browser closes without finishing THEN enables the buttons`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                code = "K7QM4XPA9TRE",
                prompt = RecoveryCodePrompt.DUE,
            )
            viewModel.onEvent(RecoveryCodeUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // When
            account.abandonedBrowserSignIns.emit(AbandonedBrowserSignIn.CANCELLED)

            // Then
            assertNull(viewModel.uiState.value.connectingProvider)
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
            assertFalse(viewModel.uiState.value.hasSignInFailed)
        }

    @Test
    fun `GIVEN the browser sign in cannot start WHEN choosing a provider THEN shows the failure`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                code = "K7QM4XPA9TRE",
                prompt = RecoveryCodePrompt.DUE,
                connectResult = Result.failure(IllegalStateException("no browser")),
            )

            // When
            viewModel.onEvent(RecoveryCodeUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // Then
            assertTrue(viewModel.uiState.value.hasSignInFailed)
            assertNull(viewModel.uiState.value.connectingProvider)
        }

    @Test
    fun `GIVEN the first Apple sign in WHEN the ID token arrives with the name THEN hands the name on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                code = "K7QM4XPA9TRE",
                prompt = RecoveryCodePrompt.DUE,
                native = setOf(AccountProvider.APPLE),
            )
            viewModel.onEvent(RecoveryCodeUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // When
            viewModel.onEvent(
                RecoveryCodeUiEvent.OnIdTokenReceived(AccountProvider.APPLE, "token", "nonce", "Pierre Vieira", null),
            )

            // Then
            assertEquals(listOf<String?>("Pierre Vieira"), account.idTokenFullNames)
        }

    @Test
    fun `GIVEN the ID token arrives WHEN it connects THEN completes the sign in and settles the buttons`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                code = "K7QM4XPA9TRE",
                prompt = RecoveryCodePrompt.DUE,
                native = setOf(AccountProvider.APPLE),
            )
            viewModel.onEvent(RecoveryCodeUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // When
            viewModel.onEvent(
                RecoveryCodeUiEvent.OnIdTokenReceived(AccountProvider.APPLE, "token", "nonce", null, null),
            )

            // Then
            assertEquals(listOf(AccountProvider.APPLE to "token"), account.idTokenConnections)
            assertEquals(1, completedSignIns)
            assertFalse(viewModel.uiState.value.hasSignInFailed)
            assertNull(viewModel.uiState.value.connectingProvider)
        }

    @Test
    fun `GIVEN the ID token arrives WHEN the connection fails THEN shows the failure`() = runTest(testDispatcher) {
        // Given
        prepareScenario(
            code = "K7QM4XPA9TRE",
            prompt = RecoveryCodePrompt.DUE,
            connectResult = Result.failure(IllegalStateException("rejected")),
        )

        // When
        viewModel.onEvent(RecoveryCodeUiEvent.OnIdTokenReceived(AccountProvider.GOOGLE, "token", null, null, null))

        // Then
        assertTrue(viewModel.uiState.value.hasSignInFailed)
        assertEquals(0, completedSignIns)
    }

    @Test
    fun `GIVEN a native sign in in progress WHEN it fails or is cancelled THEN reports only the failure`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                code = "K7QM4XPA9TRE",
                prompt = RecoveryCodePrompt.DUE,
                native = setOf(AccountProvider.APPLE),
            )
            viewModel.onEvent(RecoveryCodeUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // When
            viewModel.onEvent(RecoveryCodeUiEvent.OnNativeSignInFailed)
            val afterFailure = viewModel.uiState.value.hasSignInFailed
            viewModel.onEvent(RecoveryCodeUiEvent.OnNativeSignInCancelled)

            // Then
            assertTrue(afterFailure)
            assertFalse(viewModel.uiState.value.hasSignInFailed)
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN the page reloaded on a Google link callback WHEN the sheet shows THEN shows the spinner on Google`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                code = "K7QM4XPA9TRE",
                prompt = RecoveryCodePrompt.DUE,
            )

            // When
            account.returningBrowserSignIn.value = AccountProvider.GOOGLE

            // Then
            assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.connectingProvider)
            assertTrue(viewModel.uiState.value.isConnectExpanded)
            assertFalse(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN a returning Google link WHEN it ends with the prompt still due THEN shows the failure`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                code = "K7QM4XPA9TRE",
                prompt = RecoveryCodePrompt.DUE,
            )
            account.returningBrowserSignIn.value = AccountProvider.GOOGLE

            // When
            account.returningBrowserSignIn.value = null

            // Then
            assertTrue(viewModel.uiState.value.hasSignInFailed)
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN a returning Apple link WHEN it ends with the prompt settled THEN shows no failure`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                code = "K7QM4XPA9TRE",
                prompt = RecoveryCodePrompt.DUE,
            )
            account.returningBrowserSignIn.value = AccountProvider.APPLE

            // When
            account.recoveryCodePrompt = RecoveryCodePrompt.SAVED
            account.returningBrowserSignIn.value = null

            // Then
            assertFalse(viewModel.uiState.value.hasSignInFailed)
            assertNull(viewModel.uiState.value.connectingProvider)
        }

    private fun TestScope.prepareScenario(
        code: String?,
        prompt: RecoveryCodePrompt,
        native: Set<AccountProvider> = emptySet(),
        connectResult: Result<ConnectResult> = Result.success(ConnectResult.LINKED),
    ) {
        completedSignIns = 0
        account = FakeAccountRepository().also {
            it.recoveryCode = code
            it.recoveryCodePrompt = prompt
            it.connectResult = connectResult
        }
        viewModel = RecoveryCodeViewModel(
            updateRecoveryCodePrompt = UpdateRecoveryCodePromptUseCase(account),
            providerConnector = ProviderConnectorImpl(
                nativeSignIn = NativeSignIn(native),
                connectWithIdToken = ConnectWithIdTokenUseCase(account),
                connectWithBrowser = ConnectWithBrowserUseCase(account, BackStackNavigator(startRoute = Route.Groups)),
                completeSignIn = {
                    completedSignIns++
                    Result.success(SignInOutcome.PROFILE_RESTORED)
                },
                observeAccount = ObserveAccountUseCase(account),
                observeReturningBrowserSignIn = ObserveReturningBrowserSignInUseCase(account),
                observeAbandonedBrowserSignIn = ObserveAbandonedBrowserSignInUseCase(account),
            ),
            observeAccount = ObserveAccountUseCase(account),
            connectivityObserver = NetworkConnectivityObserver { flowOf(true) },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
