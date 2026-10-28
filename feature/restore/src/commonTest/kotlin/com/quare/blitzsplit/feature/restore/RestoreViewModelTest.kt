package com.quare.blitzsplit.feature.restore

import com.quare.blitzsplit.core.account.domain.model.ConnectResult
import com.quare.blitzsplit.core.account.domain.model.InvalidRecoveryCodeException
import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.account.domain.model.TooManyRecoveryAttemptsException
import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithBrowserUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithIdTokenUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.RestoreWithRecoveryCodeUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreError
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiAction
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiEvent
import com.quare.blitzsplit.feature.restore.presentation.viewmodel.RestoreViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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

class RestoreViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: RestoreViewModel
    private val actions = mutableListOf<RestoreUiAction>()
    private val profilePull = CompletableDeferred<Unit>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN half a code WHEN typing THEN restoring stays blocked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RestoreUiEvent.OnCodeChanged("K7QM-4XPA"))

        // Then
        assertFalse(viewModel.uiState.value.canRestore)
    }

    @Test
    fun `GIVEN a complete code WHEN restoring THEN restores and opens the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario(outcome = SignInOutcome.PROFILE_RESTORED)
        viewModel.onEvent(RestoreUiEvent.OnCodeChanged("K7QM-4XPA-9TRE"))

        // When
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

        // Then
        assertEquals(listOf("K7QM-4XPA-9TRE"), account.restoredCodes)
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN an unknown code WHEN restoring THEN shows the invalid code error`() = runTest(testDispatcher) {
        // Given
        prepareScenario(restoreError = InvalidRecoveryCodeException())
        viewModel.onEvent(RestoreUiEvent.OnCodeChanged("K7QM-4XPA-9TRE"))

        // When
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

        // Then
        assertEquals(RestoreError.INVALID_CODE, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isRestoring)
    }

    @Test
    fun `GIVEN too many attempts WHEN restoring THEN says so`() = runTest(testDispatcher) {
        // Given
        prepareScenario(restoreError = TooManyRecoveryAttemptsException())
        viewModel.onEvent(RestoreUiEvent.OnCodeChanged("K7QM-4XPA-9TRE"))

        // When
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

        // Then
        assertEquals(RestoreError.TOO_MANY_ATTEMPTS, viewModel.uiState.value.error)
    }

    @Test
    fun `GIVEN no internet WHEN restoring THEN keeps the code and calls nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)
        viewModel.onEvent(RestoreUiEvent.OnCodeChanged("K7QM-4XPA-9TRE"))

        // When
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

        // Then
        assertTrue(account.restoredCodes.isEmpty())
        assertEquals("K7QM-4XPA-9TRE", viewModel.uiState.value.code)
        assertTrue(viewModel.uiState.value.showsOfflineNotice)
    }

    @Test
    fun `GIVEN the old device QR WHEN scanned THEN restores with its code`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RestoreUiEvent.OnQrScanned("borarachar://recover?code=K7QM4XPA9TRE"))

        // Then
        assertEquals(listOf("K7QM4XPA9TRE"), account.restoredCodes)
    }

    @Test
    fun `GIVEN the restore screen WHEN scanning THEN opens the scanner`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RestoreUiEvent.OnScanQrClicked)

        // Then
        assertEquals(listOf<RestoreUiAction>(RestoreUiAction.OpenQrScanner), actions)
    }

    @Test
    fun `GIVEN no internet WHEN starting fresh THEN opens the profile setup`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        viewModel.onEvent(RestoreUiEvent.OnStartFreshClicked)

        // Then
        assertEquals(Route.ProfileSetup, navigator.backStack.value.last())
    }

    @Test
    fun `GIVEN Google is native here WHEN clicking it THEN asks the platform for an ID token`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(native = setOf(AccountProvider.GOOGLE))

            // When
            viewModel.onEvent(RestoreUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // Then
            assertEquals<List<RestoreUiAction>>(
                listOf(RestoreUiAction.LaunchNativeSignIn(AccountProvider.GOOGLE)),
                actions,
            )
            assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.connectingProvider)
        }

    @Test
    fun `GIVEN Apple is not native here WHEN clicking it THEN starts the browser sign in`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RestoreUiEvent.OnProviderClicked(AccountProvider.APPLE))

        // Then
        assertEquals(listOf(AccountProvider.APPLE), account.browserConnections)
    }

    @Test
    fun `GIVEN the browser sign in cannot start WHEN clicking a provider THEN shows the sign in error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(connectResult = Result.failure(IllegalStateException("no browser")))

            // When
            viewModel.onEvent(RestoreUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // Then
            assertEquals(RestoreError.SIGN_IN_FAILED, viewModel.uiState.value.error)
            assertNull(viewModel.uiState.value.connectingProvider)
        }

    @Test
    fun `GIVEN the first Apple sign in WHEN the ID token arrives with the name THEN hands the name on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(outcome = SignInOutcome.NEEDS_PROFILE)

            // When
            viewModel.onEvent(
                RestoreUiEvent.OnIdTokenReceived(AccountProvider.APPLE, "token", "nonce", "Pierre Vieira"),
            )

            // Then
            assertEquals(listOf<String?>("Pierre Vieira"), account.idTokenFullNames)
        }

    @Test
    fun `GIVEN an account with a profile WHEN the ID token arrives THEN opens the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario(outcome = SignInOutcome.PROFILE_RESTORED)

        // When
        viewModel.onEvent(RestoreUiEvent.OnIdTokenReceived(AccountProvider.GOOGLE, "token", "nonce", null))

        // Then
        assertEquals(listOf(AccountProvider.GOOGLE to "token"), account.idTokenConnections)
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN an account without a profile WHEN the ID token arrives THEN opens the profile setup`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(outcome = SignInOutcome.NEEDS_PROFILE)

            // When
            viewModel.onEvent(RestoreUiEvent.OnIdTokenReceived(AccountProvider.APPLE, "token", null, null))

            // Then
            assertEquals(Route.ProfileSetup, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN the profile pull fails WHEN restoring THEN shows the error and stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario(profilePullError = IllegalStateException("offline"))
        viewModel.onEvent(RestoreUiEvent.OnCodeChanged("K7QM-4XPA-9TRE"))

        // When
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

        // Then
        assertEquals(RestoreError.SIGN_IN_FAILED, viewModel.uiState.value.error)
        assertEquals(listOf(Route.Welcome, Route.Restore), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the connection fails WHEN the ID token arrives THEN shows the error and stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(connectResult = Result.failure(IllegalStateException("rejected")))

            // When
            viewModel.onEvent(RestoreUiEvent.OnIdTokenReceived(AccountProvider.GOOGLE, "token", null, null))

            // Then
            assertEquals(RestoreError.SIGN_IN_FAILED, viewModel.uiState.value.error)
            assertEquals(Route.Restore, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN a native sign in in progress WHEN it fails THEN shows the error and enables them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(native = setOf(AccountProvider.GOOGLE))
            viewModel.onEvent(RestoreUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // When
            viewModel.onEvent(RestoreUiEvent.OnNativeSignInFailed)

            // Then
            assertEquals(RestoreError.SIGN_IN_FAILED, viewModel.uiState.value.error)
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN a native sign in in progress WHEN the person cancels THEN enables the buttons without an error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(native = setOf(AccountProvider.GOOGLE))
            viewModel.onEvent(RestoreUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // When
            viewModel.onEvent(RestoreUiEvent.OnNativeSignInCancelled)

            // Then
            assertNull(viewModel.uiState.value.error)
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN an error on screen WHEN dismissing it THEN clears it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(restoreError = InvalidRecoveryCodeException())
        viewModel.onEvent(RestoreUiEvent.OnCodeChanged("K7QM-4XPA-9TRE"))
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

        // When
        viewModel.onEvent(RestoreUiEvent.OnErrorDismissed)

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `GIVEN the restore screen WHEN going back THEN returns to the welcome`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RestoreUiEvent.OnBackClicked)

        // Then
        assertEquals(listOf(Route.Welcome), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a browser sign in WHEN the account connects THEN finishes the sign in`() = runTest(testDispatcher) {
        // Given
        prepareScenario(outcome = SignInOutcome.PROFILE_RESTORED)

        // When
        account.status.value = AccountStatus.Connected("user", AccountProvider.APPLE)

        // Then
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the page reloaded after the Google sign in WHEN connected THEN shows the spinner on Google`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isProfilePullPending = true)

            // When
            account.status.value = AccountStatus.Connected("user", AccountProvider.GOOGLE)

            // Then
            assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.connectingProvider)
            assertFalse(viewModel.uiState.value.areAccountButtonsEnabled)
            assertFalse(viewModel.uiState.value.canRestore)
        }

    @Test
    fun `GIVEN a browser sign in waiting on the profile pull WHEN it fails THEN shows the error and enables them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profilePullError = IllegalStateException("offline"), isProfilePullPending = true)
            account.status.value = AccountStatus.Connected("user", AccountProvider.APPLE)

            // When
            profilePull.complete(Unit)

            // Then
            assertEquals(RestoreError.SIGN_IN_FAILED, viewModel.uiState.value.error)
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    private fun TestScope.prepareScenario(
        online: Boolean = true,
        outcome: SignInOutcome = SignInOutcome.PROFILE_RESTORED,
        profilePullError: Throwable? = null,
        restoreError: Throwable? = null,
        native: Set<AccountProvider> = emptySet(),
        connectResult: Result<ConnectResult> = Result.success(ConnectResult.LINKED),
        isProfilePullPending: Boolean = false,
    ) {
        if (!isProfilePullPending) profilePull.complete(Unit)
        account = FakeAccountRepository().also {
            it.restoreError = restoreError
            it.connectResult = connectResult
        }
        navigator = BackStackNavigator(startRoute = Route.Welcome).also { it.navigate(Route.Restore) }
        viewModel = RestoreViewModel(
            nativeSignIn = NativeSignIn(native),
            restoreWithRecoveryCode = RestoreWithRecoveryCodeUseCase(account),
            connectWithIdToken = ConnectWithIdTokenUseCase(account),
            connectWithBrowser = ConnectWithBrowserUseCase(account),
            observeAccountStatus = ObserveAccountStatusUseCase(account),
            completeSignIn = {
                profilePull.await()
                profilePullError?.let { Result.failure(it) } ?: Result.success(outcome)
            },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(online) },
            navigator = navigator,
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
