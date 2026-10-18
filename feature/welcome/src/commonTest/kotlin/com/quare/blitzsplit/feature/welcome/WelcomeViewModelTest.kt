package com.quare.blitzsplit.feature.welcome

import com.quare.blitzsplit.core.account.domain.model.ConnectResult
import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithBrowserUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithIdTokenUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeError
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiAction
import com.quare.blitzsplit.feature.welcome.presentation.model.WelcomeUiEvent
import com.quare.blitzsplit.feature.welcome.presentation.viewmodel.WelcomeViewModel
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
    fun `GIVEN a new account WHEN the ID token arrives THEN connects and opens the profile setup`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(outcome = SignInOutcome.NEEDS_PROFILE)

            // When
            viewModel.onEvent(WelcomeUiEvent.OnIdTokenReceived(AccountProvider.GOOGLE, "token", "nonce"))

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
            viewModel.onEvent(WelcomeUiEvent.OnIdTokenReceived(AccountProvider.APPLE, "token", null))

            // Then
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the profile pull fails WHEN the ID token arrives THEN shows the error and stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profilePullError = IllegalStateException("offline"))

            // When
            viewModel.onEvent(WelcomeUiEvent.OnIdTokenReceived(AccountProvider.GOOGLE, "token", "nonce"))

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
            viewModel.onEvent(WelcomeUiEvent.OnIdTokenReceived(AccountProvider.GOOGLE, "token", null))

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

    private fun TestScope.prepareScenario(
        online: Boolean = true,
        native: Set<AccountProvider> = emptySet(),
        outcome: SignInOutcome = SignInOutcome.NEEDS_PROFILE,
        profilePullError: Throwable? = null,
        connectResult: Result<ConnectResult> = Result.success(ConnectResult.LINKED),
    ) {
        account = FakeAccountRepository().also { it.connectResult = connectResult }
        navigator = BackStackNavigator(startRoute = Route.Welcome)
        isOnline = MutableStateFlow(online)
        viewModel = WelcomeViewModel(
            nativeSignIn = NativeSignIn(native),
            connectWithIdToken = ConnectWithIdTokenUseCase(account),
            connectWithBrowser = ConnectWithBrowserUseCase(account),
            observeAccountStatus = ObserveAccountStatusUseCase(account),
            completeSignIn = { profilePullError?.let { Result.failure(it) } ?: Result.success(outcome) },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            navigator = navigator,
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
