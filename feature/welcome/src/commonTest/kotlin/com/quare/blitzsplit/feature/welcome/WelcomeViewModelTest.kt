package com.quare.blitzsplit.feature.welcome

import com.quare.blitzsplit.core.account.domain.model.AbandonedBrowserSignIn
import com.quare.blitzsplit.core.account.domain.model.ConnectResult
import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithBrowserUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithIdTokenUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAbandonedBrowserSignInUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveReturningBrowserSignInUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.core.sync.signin.ProviderSignInImpl
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeError
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiAction
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiEvent
import com.quare.blitzsplit.feature.welcome.presentation.viewmodel.WelcomeViewModel
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

class WelcomeViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var viewModel: WelcomeViewModel
    private val actions = mutableListOf<WelcomeUiAction>()
    private val profilePull = CompletableDeferred<Unit>()
    private var profilePullCount = 0

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN no internet WHEN the screen opens THEN disables both account buttons and shows the notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            val state = viewModel.uiState.value

            // Then
            assertFalse(state.areAccountButtonsEnabled)
            assertTrue(state.showsOfflineNotice)
        }

    @Test
    fun `GIVEN no internet WHEN clicking Google THEN does nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        viewModel.onEvent(WelcomeUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

        // Then
        assertTrue(actions.isEmpty())
        assertTrue(account.browserConnections.isEmpty())
    }

    @Test
    fun `GIVEN Google is native here WHEN clicking it THEN asks the platform for an ID token`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(native = setOf(AccountProvider.GOOGLE))

            // When
            viewModel.onEvent(WelcomeUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // Then
            assertEquals<List<WelcomeUiAction>>(
                listOf(WelcomeUiAction.LaunchNativeSignIn(AccountProvider.GOOGLE)),
                actions,
            )
            assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.connectingProvider)
        }

    @Test
    fun `GIVEN Apple is not native here WHEN clicking it THEN starts the browser sign in`() = runTest(testDispatcher) {
        // Given
        prepareScenario(native = setOf(AccountProvider.GOOGLE))

        // When
        viewModel.onEvent(WelcomeUiEvent.OnProviderClicked(AccountProvider.APPLE))

        // Then
        assertEquals(listOf(AccountProvider.APPLE), account.browserConnections)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `GIVEN a browser sign in WHEN its callback comes back failed THEN shows the error and enables them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(WelcomeUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // When
            account.abandonedBrowserSignIns.emit(AbandonedBrowserSignIn.FAILED)

            // Then
            assertEquals(WelcomeError.SIGN_IN_FAILED, viewModel.uiState.value.error)
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN a browser sign in WHEN the browser closes without finishing THEN enables the buttons`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(WelcomeUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // When
            account.abandonedBrowserSignIns.emit(AbandonedBrowserSignIn.CANCELLED)

            // Then
            assertNull(viewModel.uiState.value.connectingProvider)
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `GIVEN the restore screen on top WHEN its browser sign in fails THEN leaves the welcome without an error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Restore)

            // When
            account.abandonedBrowserSignIns.emit(AbandonedBrowserSignIn.FAILED)

            // Then
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `GIVEN the first Apple sign in WHEN the ID token arrives with the name THEN hands the name on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(outcome = SignInOutcome.NEEDS_PROFILE)

            // When
            viewModel.onEvent(
                WelcomeUiEvent.OnIdTokenReceived(AccountProvider.APPLE, "token", "nonce", "Pierre Vieira", null),
            )

            // Then
            assertEquals(listOf<String?>("Pierre Vieira"), account.idTokenFullNames)
        }

    @Test
    fun `GIVEN a new account WHEN the ID token arrives THEN connects and opens the profile setup`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(outcome = SignInOutcome.NEEDS_PROFILE)

            // When
            viewModel.onEvent(WelcomeUiEvent.OnIdTokenReceived(AccountProvider.GOOGLE, "token", "nonce", null, null))

            // Then
            assertEquals(listOf(AccountProvider.GOOGLE to "token"), account.idTokenConnections)
            assertEquals(listOf(Route.Welcome, Route.ProfileSetup), navigator.backStack.value)
            assertNull(viewModel.uiState.value.connectingProvider)
        }

    @Test
    fun `GIVEN a returning account WHEN the ID token arrives THEN goes straight to the groups`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(outcome = SignInOutcome.PROFILE_RESTORED)

            // When
            viewModel.onEvent(WelcomeUiEvent.OnIdTokenReceived(AccountProvider.APPLE, "token", null, null, null))

            // Then
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the profile pull fails WHEN the ID token arrives THEN shows the error and stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profilePullError = IllegalStateException("offline"))

            // When
            viewModel.onEvent(WelcomeUiEvent.OnIdTokenReceived(AccountProvider.GOOGLE, "token", "nonce", null, null))

            // Then
            assertEquals(WelcomeError.SIGN_IN_FAILED, viewModel.uiState.value.error)
            assertEquals(listOf(Route.Welcome), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the connection fails WHEN the ID token arrives THEN shows the error and stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(connectResult = Result.failure(IllegalStateException("rejected")))

            // When
            viewModel.onEvent(WelcomeUiEvent.OnIdTokenReceived(AccountProvider.GOOGLE, "token", null, null, null))

            // Then
            assertEquals(WelcomeError.SIGN_IN_FAILED, viewModel.uiState.value.error)
            assertEquals(listOf(Route.Welcome), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a native sign in in progress WHEN the person cancels THEN enables the buttons again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(native = setOf(AccountProvider.GOOGLE))
            viewModel.onEvent(WelcomeUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // When
            viewModel.onEvent(WelcomeUiEvent.OnNativeSignInCancelled)

            // Then
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `GIVEN the welcome screen WHEN using without an account THEN opens the profile setup`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(WelcomeUiEvent.OnUseWithoutAccountClicked)

            // Then
            assertEquals(Route.ProfileSetup, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN the welcome screen WHEN choosing another device THEN opens the restore screen`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(WelcomeUiEvent.OnUsedOnAnotherDeviceClicked)

            // Then
            assertEquals(Route.Restore, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN the page reloaded after the Apple sign in WHEN the account connects THEN shows the spinner on Apple`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isProfilePullPending = true)

            // When
            account.status = AccountStatus.Connected("user", AccountProvider.APPLE)

            // Then
            assertEquals(AccountProvider.APPLE, viewModel.uiState.value.connectingProvider)
            assertFalse(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN a browser sign in waiting on the profile pull WHEN it completes THEN opens the profile setup`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                outcome = SignInOutcome.NEEDS_PROFILE,
                isProfilePullPending = true,
            )
            account.status = AccountStatus.Connected("user", AccountProvider.GOOGLE)

            // When
            profilePull.complete(Unit)

            // Then
            assertEquals(listOf(Route.Welcome, Route.ProfileSetup), navigator.backStack.value)
            assertNull(viewModel.uiState.value.connectingProvider)
        }

    @Test
    fun `GIVEN Apple clicked for the browser sign in WHEN connected as Google THEN keeps the spinner on Apple`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isProfilePullPending = true)
            viewModel.onEvent(WelcomeUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // When
            account.status = AccountStatus.Connected("user", AccountProvider.GOOGLE)

            // Then
            assertEquals(AccountProvider.APPLE, viewModel.uiState.value.connectingProvider)
        }

    @Test
    fun `GIVEN an ID token sign in whose profile pull failed WHEN the status arrives late THEN keeps the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profilePullError = IllegalStateException("offline"))
            account.defersStatusUpdates = true
            viewModel.onEvent(WelcomeUiEvent.OnIdTokenReceived(AccountProvider.GOOGLE, "token", "nonce", null, null))

            // When
            account.requeryStatus()

            // Then
            assertEquals(WelcomeError.SIGN_IN_FAILED, viewModel.uiState.value.error)
            assertNull(viewModel.uiState.value.connectingProvider)
            assertEquals(1, profilePullCount)
        }

    @Test
    fun `GIVEN the page reloaded on an Apple callback WHEN not mirrored yet THEN shows the spinner on Apple`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            account.returningBrowserSignIn.value = AccountProvider.APPLE

            // Then
            assertEquals(AccountProvider.APPLE, viewModel.uiState.value.connectingProvider)
            assertFalse(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN a returning Google sign in WHEN it ends without connecting THEN shows the sign in error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            account.returningBrowserSignIn.value = AccountProvider.GOOGLE

            // When
            account.returningBrowserSignIn.value = null

            // Then
            assertEquals(WelcomeError.SIGN_IN_FAILED, viewModel.uiState.value.error)
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN another screen on top WHEN a browser sign in returns THEN leaves the buttons idle`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigate(Route.Restore)

            // When
            account.returningBrowserSignIn.value = AccountProvider.GOOGLE

            // Then
            assertNull(viewModel.uiState.value.connectingProvider)
        }

    private fun TestScope.prepareScenario(
        online: Boolean = true,
        native: Set<AccountProvider> = emptySet(),
        outcome: SignInOutcome = SignInOutcome.NEEDS_PROFILE,
        profilePullError: Throwable? = null,
        connectResult: Result<ConnectResult> = Result.success(ConnectResult.LINKED),
        isProfilePullPending: Boolean = false,
    ) {
        if (!isProfilePullPending) profilePull.complete(Unit)
        account = FakeAccountRepository().also { it.connectResult = connectResult }
        navigator = BackStackNavigator(startRoute = Route.Welcome)
        isOnline = MutableStateFlow(online)
        viewModel = WelcomeViewModel(
            providerSignIn = ProviderSignInImpl(
                nativeSignIn = NativeSignIn(native),
                connectWithIdToken = ConnectWithIdTokenUseCase(account),
                connectWithBrowser = ConnectWithBrowserUseCase(account, navigator),
                completeSignIn = {
                    profilePullCount++
                    profilePull.await()
                    profilePullError?.let { Result.failure(it) } ?: Result.success(outcome)
                },
                observeAccountStatus = ObserveAccountStatusUseCase(account),
                observeReturningBrowserSignIn = ObserveReturningBrowserSignInUseCase(account),
                observeAbandonedBrowserSignIn = ObserveAbandonedBrowserSignInUseCase(account),
            ),
            navigator = navigator,
            connectivityObserver = NetworkConnectivityObserver { isOnline },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
