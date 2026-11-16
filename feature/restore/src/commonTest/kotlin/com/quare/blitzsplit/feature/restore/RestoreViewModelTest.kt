package com.quare.blitzsplit.feature.restore

import com.quare.blitzsplit.core.account.domain.model.AbandonedBrowserSignIn
import com.quare.blitzsplit.core.account.domain.model.ConnectResult
import com.quare.blitzsplit.core.account.domain.model.InvalidRecoveryCodeException
import com.quare.blitzsplit.core.account.domain.model.NativeSignIn
import com.quare.blitzsplit.core.account.domain.model.TooManyRecoveryAttemptsException
import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithBrowserUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithIdTokenUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAbandonedBrowserSignInUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveReturningBrowserSignInUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.RestoreWithRecoveryCodeUseCase
import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.model.SignInOutcome
import com.quare.blitzsplit.core.sync.signin.ProviderSignInImpl
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreError
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiAction
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiEvent
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreWordField
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
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val recoveryWords: List<String> = RECOVERY_CODE.split(" ")
    private val codeSubmitted: Pair<String, Map<String, Any>> = "restore_code_submitted" to emptyMap()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var account: FakeAccountRepository
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: RestoreViewModel
    private val actions = mutableListOf<RestoreUiAction>()
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
    fun `GIVEN half a code WHEN typing THEN restoring stays blocked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "legal winner thank"))

        // Then
        assertFalse(viewModel.uiState.value.canRestore)
    }

    @Test
    fun `GIVEN a complete code WHEN restoring THEN restores and opens the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario(outcome = SignInOutcome.PROFILE_RESTORED)
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, RECOVERY_CODE))

        // When
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

        // Then
        assertEquals(listOf(RECOVERY_CODE), account.restoredCodes)
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN an unknown code WHEN restoring THEN shows the invalid code error`() = runTest(testDispatcher) {
        // Given
        prepareScenario(restoreError = InvalidRecoveryCodeException())
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, RECOVERY_CODE))

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
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, RECOVERY_CODE))

        // When
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

        // Then
        assertEquals(RestoreError.TOO_MANY_ATTEMPTS, viewModel.uiState.value.error)
    }

    @Test
    fun `GIVEN no internet WHEN restoring THEN keeps the code and calls nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, RECOVERY_CODE))

        // When
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

        // Then
        assertTrue(account.restoredCodes.isEmpty())
        assertEquals(
            RECOVERY_CODE.split(" "),
            viewModel.uiState.value.words
                .map { it.text },
        )
        assertTrue(viewModel.uiState.value.showsOfflineNotice)
    }

    @Test
    fun `GIVEN the old device QR WHEN scanned THEN restores with its code`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RestoreUiEvent.OnQrScanned("borarachar://recover?code=${RECOVERY_CODE.replace(' ', '+')}"))

        // Then
        assertEquals(listOf(RECOVERY_CODE), account.restoredCodes)
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
    fun `GIVEN a browser sign in WHEN its callback comes back failed THEN shows the error and enables them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RestoreUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // When
            account.abandonedBrowserSignIns.emit(AbandonedBrowserSignIn.FAILED)

            // Then
            assertEquals(RestoreError.SIGN_IN_FAILED, viewModel.uiState.value.error)
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN a browser sign in WHEN the browser closes without finishing THEN enables the buttons`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RestoreUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // When
            account.abandonedBrowserSignIns.emit(AbandonedBrowserSignIn.CANCELLED)

            // Then
            assertNull(viewModel.uiState.value.connectingProvider)
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
            assertNull(viewModel.uiState.value.error)
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
                RestoreUiEvent.OnIdTokenReceived(AccountProvider.APPLE, "token", "nonce", "Pierre Vieira", null),
            )

            // Then
            assertEquals(listOf<String?>("Pierre Vieira"), account.idTokenFullNames)
        }

    @Test
    fun `GIVEN an account with a profile WHEN the ID token arrives THEN opens the groups`() = runTest(testDispatcher) {
        // Given
        prepareScenario(outcome = SignInOutcome.PROFILE_RESTORED)

        // When
        viewModel.onEvent(RestoreUiEvent.OnIdTokenReceived(AccountProvider.GOOGLE, "token", "nonce", null, null))

        // Then
        assertEquals(listOf(AccountProvider.GOOGLE to "token"), account.idTokenConnections)
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN an account without a profile WHEN the ID token arrives THEN opens the profile setup`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(outcome = SignInOutcome.NEEDS_PROFILE)

            // When
            viewModel.onEvent(RestoreUiEvent.OnIdTokenReceived(AccountProvider.APPLE, "token", null, null, null))

            // Then
            assertEquals(Route.ProfileSetup, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN the profile pull fails WHEN restoring THEN shows the error and stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario(profilePullError = IllegalStateException("offline"))
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, RECOVERY_CODE))

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
            viewModel.onEvent(RestoreUiEvent.OnIdTokenReceived(AccountProvider.GOOGLE, "token", null, null, null))

            // Then
            assertEquals(RestoreError.SIGN_IN_FAILED, viewModel.uiState.value.error)
            assertEquals(Route.Restore, navigator.backStack.value.last())
        }

    @Test
    fun `GIVEN a native sign in in progress WHEN it fails THEN shows the error and enables the buttons`() =
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
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, RECOVERY_CODE))
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
        account.status = AccountStatus.Connected("user", AccountProvider.APPLE)

        // Then
        assertEquals(listOf(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the page reloaded after the Google sign in WHEN connected THEN shows the spinner on Google`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isProfilePullPending = true)
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, RECOVERY_CODE))

            // When
            account.status = AccountStatus.Connected("user", AccountProvider.GOOGLE)

            // Then
            assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.connectingProvider)
            assertFalse(viewModel.uiState.value.areAccountButtonsEnabled)
            assertFalse(viewModel.uiState.value.canRestore)
        }

    @Test
    fun `GIVEN a browser sign in waiting on the profile pull WHEN it fails THEN shows the error and enables them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                profilePullError = IllegalStateException("offline"),
                isProfilePullPending = true,
            )
            account.status = AccountStatus.Connected("user", AccountProvider.APPLE)

            // When
            profilePull.complete(Unit)

            // Then
            assertEquals(RestoreError.SIGN_IN_FAILED, viewModel.uiState.value.error)
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN the code of an account with Google WHEN restoring THEN finishes the sign in once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                outcome = SignInOutcome.NEEDS_PROFILE,
                isProfilePullPending = true,
            )
            account.restoredStatus = AccountStatus.Connected("user", AccountProvider.GOOGLE)
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, RECOVERY_CODE))
            viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

            // When
            profilePull.complete(Unit)

            // Then
            assertEquals(listOf(Route.Welcome, Route.Restore, Route.ProfileSetup), navigator.backStack.value)
            assertNull(viewModel.uiState.value.connectingProvider)
        }

    @Test
    fun `GIVEN a code restore whose profile pull failed WHEN the Google status arrives late THEN keeps the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profilePullError = IllegalStateException("offline"))
            account.defersStatusUpdates = true
            account.restoredStatus = AccountStatus.Connected("user", AccountProvider.GOOGLE)
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, RECOVERY_CODE))
            viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

            // When
            account.requeryStatus()

            // Then
            assertEquals(RestoreError.SIGN_IN_FAILED, viewModel.uiState.value.error)
            assertNull(viewModel.uiState.value.connectingProvider)
            assertEquals(1, profilePullCount)
        }

    @Test
    fun `GIVEN an ID token sign in whose profile pull failed WHEN the status arrives late THEN keeps the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(profilePullError = IllegalStateException("offline"))
            account.defersStatusUpdates = true
            viewModel.onEvent(RestoreUiEvent.OnIdTokenReceived(AccountProvider.APPLE, "token", "nonce", null, null))

            // When
            account.requeryStatus()

            // Then
            assertEquals(RestoreError.SIGN_IN_FAILED, viewModel.uiState.value.error)
            assertEquals(1, profilePullCount)
        }

    @Test
    fun `GIVEN the page reloaded on a Google callback WHEN not mirrored yet THEN shows the spinner on Google`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            account.returningBrowserSignIn.value = AccountProvider.GOOGLE

            // Then
            assertEquals(AccountProvider.GOOGLE, viewModel.uiState.value.connectingProvider)
            assertFalse(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN a returning Apple sign in WHEN its session connects THEN keeps the spinner for the profile pull`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(isProfilePullPending = true)
            account.returningBrowserSignIn.value = AccountProvider.APPLE

            // When
            account.status = AccountStatus.Connected("user", AccountProvider.APPLE)
            account.returningBrowserSignIn.value = null

            // Then
            assertEquals(AccountProvider.APPLE, viewModel.uiState.value.connectingProvider)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `GIVEN a returning Apple sign in WHEN it ends without connecting THEN shows the sign in error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            account.returningBrowserSignIn.value = AccountProvider.APPLE

            // When
            account.returningBrowserSignIn.value = null

            // Then
            assertEquals(RestoreError.SIGN_IN_FAILED, viewModel.uiState.value.error)
            assertTrue(viewModel.uiState.value.areAccountButtonsEnabled)
        }

    @Test
    fun `GIVEN the restore screen WHEN typing the code THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, RECOVERY_CODE))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a typed code WHEN restoring THEN tracks only the submit with no params`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, RECOVERY_CODE))

        // When
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

        // Then
        assertEquals(listOf(codeSubmitted), trackedEvents)
    }

    @Test
    fun `GIVEN half a code WHEN restoring THEN still tracks the submit`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "legal winner thank"))

        // When
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

        // Then
        assertEquals(listOf(codeSubmitted), trackedEvents)
    }

    @Test
    fun `GIVEN nothing typed WHEN restoring THEN still tracks the submit`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

        // Then
        assertEquals(listOf(codeSubmitted), trackedEvents)
    }

    @Test
    fun `GIVEN an unknown code WHEN restoring THEN tracks the submit once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(restoreError = InvalidRecoveryCodeException())
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, RECOVERY_CODE))

        // When
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

        // Then
        assertEquals(listOf(codeSubmitted), trackedEvents)
    }

    @Test
    fun `GIVEN the restore screen WHEN scanning THEN tracks the scan click with no params`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RestoreUiEvent.OnScanQrClicked)

        // Then
        assertEquals(listOf("restore_scan_qr_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the old device QR WHEN scanned THEN tracks nothing even though it restores`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RestoreUiEvent.OnQrScanned("borarachar://recover?code=${RECOVERY_CODE.replace(' ', '+')}"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN Google is native here WHEN clicking it THEN tracks Google as the provider from the restore`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(native = setOf(AccountProvider.GOOGLE))

            // When
            viewModel.onEvent(RestoreUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // Then
            assertEquals(listOf(providerClicked(AccountProvider.GOOGLE)), trackedEvents)
        }

    @Test
    fun `GIVEN Apple is not native here WHEN clicking it THEN tracks Apple as the provider from the restore`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RestoreUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // Then
            assertEquals(listOf(providerClicked(AccountProvider.APPLE)), trackedEvents)
        }

    @Test
    fun `GIVEN a Google sign in in progress WHEN clicking Apple THEN tracks each click once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(native = setOf(AccountProvider.GOOGLE))
            viewModel.onEvent(RestoreUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // When
            viewModel.onEvent(RestoreUiEvent.OnProviderClicked(AccountProvider.APPLE))

            // Then
            assertEquals(
                listOf(providerClicked(AccountProvider.GOOGLE), providerClicked(AccountProvider.APPLE)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the restore screen WHEN the ID token arrives THEN tracks nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario(outcome = SignInOutcome.NEEDS_PROFILE)

        // When
        viewModel.onEvent(
            RestoreUiEvent.OnIdTokenReceived(AccountProvider.APPLE, "token", "nonce", "Pierre Vieira", "code"),
        )

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a native sign in in progress WHEN it fails THEN tracks the failure from the restore`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(native = setOf(AccountProvider.GOOGLE))
            viewModel.onEvent(RestoreUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // When
            viewModel.onEvent(RestoreUiEvent.OnNativeSignInFailed)

            // Then
            assertEquals(
                listOf(
                    providerClicked(AccountProvider.GOOGLE),
                    "native_sign_in_failed" to mapOf<String, Any>("source" to "restore"),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a native sign in in progress WHEN the person cancels THEN tracks the cancel from the restore`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(native = setOf(AccountProvider.GOOGLE))
            viewModel.onEvent(RestoreUiEvent.OnProviderClicked(AccountProvider.GOOGLE))

            // When
            viewModel.onEvent(RestoreUiEvent.OnNativeSignInCancelled)

            // Then
            assertEquals(
                listOf(
                    providerClicked(AccountProvider.GOOGLE),
                    "native_sign_in_cancelled" to mapOf<String, Any>("source" to "restore"),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN no internet WHEN starting fresh THEN tracks it with no params`() = runTest(testDispatcher) {
        // Given
        prepareScenario(online = false)

        // When
        viewModel.onEvent(RestoreUiEvent.OnStartFreshClicked)

        // Then
        assertEquals(listOf("restore_start_fresh_clicked" to emptyMap<String, Any>()), trackedEvents)
    }

    @Test
    fun `GIVEN the restore screen WHEN going back THEN sends no event`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RestoreUiEvent.OnBackClicked)

        // Then
        assertEquals(emptyList(), trackedEvents)
    }

    @Test
    fun `GIVEN an error on screen WHEN dismissing it THEN sends nothing more`() = runTest(testDispatcher) {
        // Given
        prepareScenario(restoreError = InvalidRecoveryCodeException())
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, RECOVERY_CODE))
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

        // When
        viewModel.onEvent(RestoreUiEvent.OnErrorDismissed)

        // Then
        assertEquals(
            listOf(codeSubmitted),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN no internet WHEN the button is tapped THEN tracks a retry rather than a submit`() = runTest(
        testDispatcher,
    ) {
        // Given
        prepareScenario(online = false)

        // When
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

        // Then
        assertEquals(listOf("retry_clicked" to mapOf<String, Any>("source" to "restore")), trackedEvents)
    }

    @Test
    fun `GIVEN the restore screen WHEN it opens THEN shows 12 empty fields none marked`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(
            List(12) {
                RestoreWordField(
                    text = "",
                    isDeadEnd = false,
                )
            },
            state.words,
        )
        assertNull(state.focusedWordIndex)
        assertEquals(emptyList(), state.suggestions)
        assertFalse(state.isCodeComplete)
    }

    @Test
    fun `GIVEN an empty field WHEN typing a word with capitals THEN shows it in lowercase`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(3, "Leg"))

        // Then
        assertEquals(List(12) { if (it == 3) "leg" else "" }, wordTexts())
        assertEquals(emptyList(), actions)
    }

    @Test
    fun `GIVEN an error on screen WHEN typing a word THEN clears the error`() = runTest(testDispatcher) {
        // Given
        prepareScenario(restoreError = InvalidRecoveryCodeException())
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, RECOVERY_CODE))
        viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

        // When
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "leg"))

        // Then
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `GIVEN the first field WHEN pasting three words THEN fills three whole words and moves on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "lega winn thank"))

            // Then
            assertEquals(listOf("legal", "winner", "thank") + List(9) { "" }, wordTexts())
            assertEquals(listOf<RestoreUiAction>(RestoreUiAction.FocusWord(3)), actions)
        }

    @Test
    fun `GIVEN the third field WHEN pasting three words THEN fills from it and moves to the first empty`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(2, "legal winner thank"))

            // Then
            assertEquals(listOf("", "", "legal", "winner", "thank") + List(7) { "" }, wordTexts())
            assertEquals(listOf<RestoreUiAction>(RestoreUiAction.FocusWord(0)), actions)
        }

    @Test
    fun `GIVEN the eleventh field WHEN pasting three words THEN keeps only the two that fit`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(10, "legal winner thank"))

            // Then
            assertEquals(List(10) { "" } + listOf("legal", "winner"), wordTexts())
        }

    @Test
    fun `GIVEN the sixth field WHEN pasting the 12 words THEN fills all 12 from the first and moves nowhere`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(5, RECOVERY_CODE))

            // Then
            assertEquals(recoveryWords, wordTexts())
            assertEquals(emptyList(), actions)
        }

    @Test
    fun `GIVEN the fifth field WHEN pasting a list of 13 words THEN fills all 12 fields from the first`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val numberedList =
                recoveryWords.mapIndexed { i, word -> "${i + 1}. $word" }.joinToString("\n") + "\n13. act"

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(4, numberedList))

            // Then
            assertEquals(recoveryWords, wordTexts())
        }

    @Test
    fun `GIVEN the first field WHEN typing a word start and a space THEN completes it and moves on`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "lega "))

            // Then
            assertEquals(
                "legal",
                viewModel.uiState.value.words[0]
                    .text,
            )
            assertEquals(listOf<RestoreUiAction>(RestoreUiAction.FocusWord(1)), actions)
        }

    @Test
    fun `GIVEN the last field WHEN typing a word and a space THEN completes it and moves nowhere`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(11, "yell "))

            // Then
            assertEquals(
                "yellow",
                viewModel.uiState.value.words[11]
                    .text,
            )
            assertEquals(emptyList(), actions)
        }

    @Test
    fun `GIVEN the first field WHEN typing a non word and a space THEN keeps the letters and moves nowhere`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "qqx "))

            // Then
            assertEquals(
                "qqx",
                viewModel.uiState.value.words[0]
                    .text,
            )
            assertEquals(emptyList(), actions)
        }

    @Test
    fun `GIVEN an empty field WHEN typing letters no word starts with THEN marks the field at once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "qqx"))

            // Then
            assertTrue(
                viewModel.uiState.value.words[0]
                    .isDeadEnd,
            )
            assertFalse(
                viewModel.uiState.value.words[1]
                    .isDeadEnd,
            )
        }

    @Test
    fun `GIVEN an empty field WHEN typing letters a word can still come out of THEN leaves the field unmarked`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "le"))

            // Then
            assertFalse(
                viewModel.uiState.value.words[0]
                    .isDeadEnd,
            )
        }

    @Test
    fun `GIVEN a marked field WHEN erasing it THEN unmarks it`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "qqx"))

        // When
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, ""))

        // Then
        assertEquals(
            RestoreWordField(
                text = "",
                isDeadEnd = false,
            ),
            viewModel.uiState.value.words[0],
        )
    }

    @Test
    fun `GIVEN no field focused WHEN focusing the third THEN marks it as focused`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RestoreUiEvent.OnWordFocused(2))

        // Then
        assertEquals(2, viewModel.uiState.value.focusedWordIndex)
    }

    @Test
    fun `GIVEN a focused field holding the first letters of a word WHEN focusing another THEN shows the whole word`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RestoreUiEvent.OnWordFocused(0))
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "lega"))

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordFocused(1))

            // Then
            assertEquals(listOf("legal", ""), wordTexts().take(2))
            assertEquals(1, viewModel.uiState.value.focusedWordIndex)
        }

    @Test
    fun `GIVEN a focused field holding letters of several words WHEN focusing another THEN keeps the letters`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RestoreUiEvent.OnWordFocused(0))
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "le"))

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordFocused(1))

            // Then
            assertEquals(
                "le",
                viewModel.uiState.value.words[0]
                    .text,
            )
        }

    @Test
    fun `GIVEN a focused field holding the first letters of a word WHEN focusing it again THEN keeps the letters`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RestoreUiEvent.OnWordFocused(0))
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "lega"))

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordFocused(0))

            // Then
            assertEquals(
                "lega",
                viewModel.uiState.value.words[0]
                    .text,
            )
        }

    @Test
    fun `GIVEN a focused field WHEN typing the first letters of a word THEN suggests up to three words`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RestoreUiEvent.OnWordFocused(0))

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "le"))

            // Then
            assertEquals(listOf("leader", "leaf", "learn"), viewModel.uiState.value.suggestions)
        }

    @Test
    fun `GIVEN no field focused WHEN typing letters THEN suggests nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "le"))

        // Then
        assertEquals(emptyList(), viewModel.uiState.value.suggestions)
    }

    @Test
    fun `GIVEN another field holding letters WHEN focusing an empty field THEN suggests nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "le"))

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordFocused(1))

            // Then
            assertEquals(emptyList(), viewModel.uiState.value.suggestions)
        }

    @Test
    fun `GIVEN a focused field WHEN typing a whole word no other word starts with THEN suggests nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RestoreUiEvent.OnWordFocused(0))

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "legal"))

            // Then
            assertEquals(emptyList(), viewModel.uiState.value.suggestions)
        }

    @Test
    fun `GIVEN a focused field WHEN typing a short whole word other words start with THEN still suggests them`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RestoreUiEvent.OnWordFocused(0))

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "act"))

            // Then
            assertEquals(listOf("act", "action", "actor"), viewModel.uiState.value.suggestions)
        }

    @Test
    fun `GIVEN a focused field WHEN tapping a suggestion THEN writes the word and moves to the next`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RestoreUiEvent.OnWordFocused(4))
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(4, "le"))

            // When
            viewModel.onEvent(RestoreUiEvent.OnSuggestionClicked("learn"))

            // Then
            assertEquals(
                "learn",
                viewModel.uiState.value.words[4]
                    .text,
            )
            assertEquals(listOf<RestoreUiAction>(RestoreUiAction.FocusWord(5)), actions)
        }

    @Test
    fun `GIVEN no field focused WHEN tapping a suggestion THEN changes nothing`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(RestoreUiEvent.OnWordChanged(0, "le"))

        // When
        viewModel.onEvent(RestoreUiEvent.OnSuggestionClicked("learn"))

        // Then
        assertEquals(List(12) { if (it == 0) "le" else "" }, wordTexts())
        assertEquals(emptyList(), actions)
    }

    @Test
    fun `GIVEN 11 words typed WHEN typing the start of the last THEN the code can be restored`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            recoveryWords.dropLast(1).forEachIndexed { index, word ->
                viewModel.onEvent(RestoreUiEvent.OnWordChanged(index, word))
            }

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(11, "yell"))

            // Then
            assertTrue(viewModel.uiState.value.isCodeComplete)
            assertTrue(viewModel.uiState.value.canRestore)
        }

    @Test
    fun `GIVEN 11 words typed WHEN typing a non word in the last field THEN the code stays incomplete`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            recoveryWords.dropLast(1).forEachIndexed { index, word ->
                viewModel.onEvent(RestoreUiEvent.OnWordChanged(index, word))
            }

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(11, "qqx"))

            // Then
            assertFalse(viewModel.uiState.value.isCodeComplete)
            assertFalse(viewModel.uiState.value.canRestore)
        }

    @Test
    fun `GIVEN the 12 words typed as their first letters WHEN restoring THEN sends them joined by single spaces`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val prefixes = recoveryWords.map { it.take(4) }
            prefixes.forEachIndexed { index, prefix -> viewModel.onEvent(RestoreUiEvent.OnWordChanged(index, prefix)) }

            // When
            viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

            // Then
            assertEquals(listOf(prefixes.joinToString(" ")), account.restoredCodes)
        }

    @Test
    fun `GIVEN the old device QR WHEN scanned THEN fills the 12 fields`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        val content = "borarachar://recover?code=${RECOVERY_CODE.replace(' ', '+')}"

        // When
        viewModel.onEvent(RestoreUiEvent.OnQrScanned(content))

        // Then
        assertEquals(recoveryWords, wordTexts())
    }

    @Test
    fun `GIVEN a QR with the bare words WHEN scanned THEN fills the 12 fields and restores`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RestoreUiEvent.OnQrScanned(RECOVERY_CODE))

            // Then
            assertEquals(recoveryWords, wordTexts())
            assertEquals(listOf(RECOVERY_CODE), account.restoredCodes)
        }

    @Test
    fun `GIVEN the restore screen WHEN a word arrives for a field past the last THEN changes nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(12, "legal"))

            // Then
            assertEquals(List(12) { "" }, wordTexts())
            assertEquals(emptyList(), actions)
        }

    @Test
    fun `GIVEN the restore screen WHEN a word arrives for a negative field THEN changes nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordChanged(-1, "legal winner"))

            // Then
            assertEquals(List(12) { "" }, wordTexts())
            assertEquals(emptyList(), actions)
        }

    @Test
    fun `GIVEN a focused field WHEN focusing a field past the last THEN keeps the focus where it was`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(RestoreUiEvent.OnWordFocused(3))

            // When
            viewModel.onEvent(RestoreUiEvent.OnWordFocused(12))

            // Then
            assertEquals(3, viewModel.uiState.value.focusedWordIndex)
        }

    private fun wordTexts(): List<String> = viewModel.uiState.value.words
        .map { it.text }

    private fun providerClicked(provider: AccountProvider): Pair<String, Map<String, Any>> =
        "sign_in_provider_clicked" to mapOf("provider" to provider, "source" to "restore")

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
            trackEvent = { name, params -> trackedEvents += name to params },
            providerSignIn = ProviderSignInImpl(
                nativeSignIn = NativeSignIn(native),
                connectWithIdToken = ConnectWithIdTokenUseCase(account),
                connectWithBrowser = ConnectWithBrowserUseCase(account),
                completeSignIn = {
                    profilePullCount++
                    profilePull.await()
                    profilePullError?.let { Result.failure(it) } ?: Result.success(outcome)
                },
                observeAccountStatus = ObserveAccountStatusUseCase(account),
                observeReturningBrowserSignIn = ObserveReturningBrowserSignInUseCase(account),
                observeAbandonedBrowserSignIn = ObserveAbandonedBrowserSignInUseCase(account),
            ),
            restoreWithRecoveryCode = RestoreWithRecoveryCodeUseCase(account),
            navigator = navigator,
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(online) },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        private const val RECOVERY_CODE = "legal winner thank year wave sausage worth useful legal winner thank yellow"
    }
}
