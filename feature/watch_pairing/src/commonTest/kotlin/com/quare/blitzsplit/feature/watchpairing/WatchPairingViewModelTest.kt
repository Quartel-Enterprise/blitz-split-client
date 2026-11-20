package com.quare.blitzsplit.feature.watchpairing

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.profile.domain.usecase.impl.ObserveProfileUseCase
import com.quare.blitzsplit.core.profile.testing.FakeProfileRepository
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCode
import com.quare.blitzsplit.feature.watchpairing.presentation.model.PhoneReply
import com.quare.blitzsplit.feature.watchpairing.presentation.model.WatchPairingError
import com.quare.blitzsplit.feature.watchpairing.presentation.model.WatchPairingStep
import com.quare.blitzsplit.feature.watchpairing.presentation.model.WatchPairingUiAction
import com.quare.blitzsplit.feature.watchpairing.presentation.model.WatchPairingUiEvent
import com.quare.blitzsplit.feature.watchpairing.presentation.viewmodel.WatchPairingViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class WatchPairingViewModelTest {
    private val trackedEvents = mutableListOf<String>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val actions = mutableListOf<WatchPairingUiAction>()
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var signedOutOnPhone: MutableStateFlow<Boolean>
    private lateinit var profiles: FakeProfileRepository
    private lateinit var server: FakeWatchPairingServer
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: WatchPairingViewModel
    private var now = 0L
    private var dismissCount = 0

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN a first launch WHEN the screen opens THEN shows the welcome`() = runPairingTest {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(WatchPairingStep.WELCOME, state.step)
        assertNull(state.code)
        assertNull(state.error)
        assertFalse(state.isWaiting)
    }

    @Test
    fun `GIVEN the phone signed out WHEN the screen opens THEN shows the signed out notice`() = runPairingTest {
        // Given
        prepareScenario(isSignedOutOnPhone = true)

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals(WatchPairingStep.SIGNED_OUT_ON_PHONE, state.step)
    }

    @Test
    fun `GIVEN the screen open WHEN the connection drops THEN says it is offline`() = runPairingTest {
        // Given
        prepareScenario()

        // When
        isOnline.value = false

        // Then
        assertFalse(viewModel.uiState.value.isOnline)
    }

    @Test
    fun `GIVEN a profile with a photo WHEN the screen opens THEN shows its name color and photo`() = runPairingTest {
        // Given
        prepareScenario(profile = pierre)

        // When
        val state = viewModel.uiState.value

        // Then
        assertEquals("Pierre", state.profileName)
        assertEquals(AvatarColor.PEACH, state.profileColor)
        assertEquals("https://example.com/pierre.jpg", state.profilePhotoUrl)
    }

    @Test
    fun `GIVEN no profile yet WHEN the account profile arrives THEN shows its name`() = runPairingTest {
        // Given
        prepareScenario()

        // When
        profiles.profile.value = pierre

        // Then
        assertEquals("Pierre", viewModel.uiState.value.profileName)
        assertEquals(AvatarColor.PEACH, viewModel.uiState.value.profileColor)
    }

    @Test
    fun `GIVEN no internet WHEN Connect is clicked THEN shows the no internet error and asks nothing`() =
        runPairingTest {
            // Given
            prepareScenario(online = false)

            // When
            viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)

            // Then
            assertEquals(WatchPairingStep.WELCOME, viewModel.uiState.value.step)
            assertEquals(WatchPairingError.NO_INTERNET, viewModel.uiState.value.error)
            assertTrue(server.requestedCodes.isEmpty())
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN online WHEN Connect is clicked THEN sends a new code to the phone and polls it while connecting`() =
        runPairingTest {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)

            // Then
            val code = server.requestedCodes.single()
            assertEquals(WatchPairingStep.CONNECTING, viewModel.uiState.value.step)
            assertTrue(viewModel.uiState.value.isWaiting)
            assertNull(viewModel.uiState.value.error)
            assertEquals(listOf<WatchPairingUiAction>(WatchPairingUiAction.SendCodeToPhone(code.code)), actions)
            assertEquals(listOf(code.code), server.activePolls)
        }

    @Test
    fun `GIVEN the server gives no code WHEN Connect is clicked THEN goes back to the welcome with the failed error`() =
        runPairingTest {
            // Given
            prepareScenario()
            server.requestError = IllegalStateException("server down")

            // When
            viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)

            // Then
            assertEquals(WatchPairingStep.WELCOME, viewModel.uiState.value.step)
            assertEquals(WatchPairingError.FAILED, viewModel.uiState.value.error)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN offline once the code request fails WHEN Connect is clicked THEN shows the no internet error`() =
        runPairingTest {
            // Given
            prepareScenario()
            server.requestError = IllegalStateException("no route to host")
            server.beforeRequest = { isOnline.value = false }

            // When
            viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)

            // Then
            assertEquals(WatchPairingStep.WELCOME, viewModel.uiState.value.step)
            assertEquals(WatchPairingError.NO_INTERNET, viewModel.uiState.value.error)
        }

    @Test
    fun `GIVEN the phone signed out WHEN Connect again is clicked THEN dismisses the notice and connects`() =
        runPairingTest {
            // Given
            prepareScenario(isSignedOutOnPhone = true)

            // When
            viewModel.onEvent(WatchPairingUiEvent.OnConnectAgainClicked)

            // Then
            assertEquals(1, dismissCount)
            assertEquals(WatchPairingStep.CONNECTING, viewModel.uiState.value.step)
            assertEquals(1, server.requestedCodes.size)
            assertEquals(1, actions.size)
        }

    @Test
    fun `GIVEN connecting WHEN the phone is unreachable THEN shows the phone out of reach`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)

        // When
        viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))

        // Then
        assertEquals(WatchPairingStep.PHONE_OUT_OF_REACH, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.isWaiting)
    }

    @Test
    fun `GIVEN connecting WHEN the phone does not reply in 20 seconds THEN shows the phone out of reach`() =
        runPairingTest {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)

            // When
            advanceTimeBy(20.seconds)
            runCurrent()

            // Then
            assertEquals(WatchPairingStep.PHONE_OUT_OF_REACH, viewModel.uiState.value.step)
        }

    @Test
    fun `GIVEN connecting WHEN 19 seconds pass without a reply THEN keeps connecting`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)

        // When
        advanceTimeBy(19.seconds)
        runCurrent()

        // Then
        assertEquals(WatchPairingStep.CONNECTING, viewModel.uiState.value.step)
    }

    @Test
    fun `GIVEN the phone approved the code WHEN 20 seconds pass THEN keeps connecting`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
        viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.APPROVED))

        // When
        advanceTimeBy(30.seconds)
        runCurrent()

        // Then
        assertEquals(WatchPairingStep.CONNECTING, viewModel.uiState.value.step)
    }

    @Test
    fun `GIVEN connecting WHEN the phone profile is local only THEN shows the local only notice and stops polling`() =
        runPairingTest {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)

            // When
            viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.LOCAL_ONLY))

            // Then
            assertEquals(WatchPairingStep.LOCAL_ONLY, viewModel.uiState.value.step)
            assertTrue(server.activePolls.isEmpty())
        }

    @Test
    fun `GIVEN the local only notice WHEN OK is clicked THEN goes back to the welcome`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
        viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.LOCAL_ONLY))

        // When
        viewModel.onEvent(WatchPairingUiEvent.OnLocalOnlyAcknowledged)

        // Then
        assertEquals(WatchPairingStep.WELCOME, viewModel.uiState.value.step)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `GIVEN connecting WHEN the phone approves the code THEN signs in with its token hash and shows connected`() =
        runPairingTest {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
            val code = server.requestedCodes.single().code

            // When
            server.approve(code)

            // Then
            assertEquals(listOf(FakeWatchPairingServer.tokenHashOf(code)), server.signedInTokenHashes)
            assertEquals(WatchPairingStep.CONNECTED, viewModel.uiState.value.step)
            assertFalse(viewModel.uiState.value.isWaiting)
        }

    @Test
    fun `GIVEN the first sign in fails WHEN the code is approved THEN polls again and connects with the next token`() =
        runPairingTest {
            // Given
            prepareScenario()
            server.signInFailuresLeft = 1
            viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)

            // When
            server.approve(server.requestedCodes.single().code)
            advanceTimeBy(3.seconds)
            runCurrent()

            // Then
            assertEquals(2, server.signedInTokenHashes.size)
            assertEquals(WatchPairingStep.CONNECTED, viewModel.uiState.value.step)
        }

    @Test
    fun `GIVEN the phone out of reach WHEN another device approves the code THEN shows connected`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
        viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))

        // When
        server.approve(server.requestedCodes.single().code)

        // Then
        assertEquals(WatchPairingStep.CONNECTED, viewModel.uiState.value.step)
    }

    @Test
    fun `GIVEN the code on screen WHEN another device approves it THEN shows connected`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
        viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))
        viewModel.onEvent(WatchPairingUiEvent.OnUseCodeClicked)

        // When
        server.approve(server.requestedCodes.single().code)

        // Then
        assertEquals(WatchPairingStep.CONNECTED, viewModel.uiState.value.step)
    }

    @Test
    fun `GIVEN a code with more than a minute left WHEN Try again is clicked THEN sends the same code again`() =
        runPairingTest {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
            viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))
            val code = server.requestedCodes.single().code
            now = codeLifetime.inWholeMilliseconds - 61.seconds.inWholeMilliseconds

            // When
            viewModel.onEvent(WatchPairingUiEvent.OnTryAgainClicked)

            // Then
            assertEquals(WatchPairingStep.CONNECTING, viewModel.uiState.value.step)
            assertEquals(1, server.requestedCodes.size)
            assertEquals(
                listOf<WatchPairingUiAction>(
                    WatchPairingUiAction.SendCodeToPhone(code),
                    WatchPairingUiAction.SendCodeToPhone(code),
                ),
                actions,
            )
            assertEquals(listOf(code), server.activePolls)
        }

    @Test
    fun `GIVEN a code with less than a minute left WHEN Try again is clicked THEN sends a new code to the phone`() =
        runPairingTest {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
            viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))
            now = codeLifetime.inWholeMilliseconds - 59.seconds.inWholeMilliseconds

            // When
            viewModel.onEvent(WatchPairingUiEvent.OnTryAgainClicked)

            // Then
            val newCode = server.requestedCodes[1].code
            assertEquals(2, server.requestedCodes.size)
            assertEquals(WatchPairingUiAction.SendCodeToPhone(newCode), actions.last())
            assertEquals(WatchPairingStep.CONNECTING, viewModel.uiState.value.step)
        }

    @Test
    fun `GIVEN a new code sent by Try again WHEN the phone approves it THEN shows connected`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
        viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))
        now = codeLifetime.inWholeMilliseconds - 59.seconds.inWholeMilliseconds
        viewModel.onEvent(WatchPairingUiEvent.OnTryAgainClicked)
        val newCode = server.requestedCodes[1].code

        // When
        server.approve(newCode)

        // Then
        assertEquals(WatchPairingStep.CONNECTED, viewModel.uiState.value.step)
    }

    @Test
    fun `GIVEN a code with more than a minute left WHEN Use code is clicked THEN shows that code`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
        viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))
        val code = server.requestedCodes.single().code

        // When
        viewModel.onEvent(WatchPairingUiEvent.OnUseCodeClicked)

        // Then
        assertEquals(WatchPairingStep.CODE, viewModel.uiState.value.step)
        assertEquals(code, viewModel.uiState.value.code)
        assertFalse(viewModel.uiState.value.isWaiting)
        assertEquals(1, server.requestedCodes.size)
        assertEquals(listOf(code), server.activePolls)
    }

    @Test
    fun `GIVEN a code with less than a minute left WHEN Use code is clicked THEN shows a new code and polls it`() =
        runPairingTest {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
            viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))
            now = codeLifetime.inWholeMilliseconds - 59.seconds.inWholeMilliseconds

            // When
            viewModel.onEvent(WatchPairingUiEvent.OnUseCodeClicked)

            // Then
            val newCode = server.requestedCodes[1].code
            assertEquals(WatchPairingStep.CODE, viewModel.uiState.value.step)
            assertEquals(newCode, viewModel.uiState.value.code)
            assertEquals(listOf(newCode), server.activePolls)
        }

    @Test
    fun `GIVEN the new code still on its way WHEN Use code is clicked THEN waits on the code screen`() =
        runPairingTest {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
            viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))
            now = codeLifetime.inWholeMilliseconds - 59.seconds.inWholeMilliseconds
            server.requestLatency = 1.seconds

            // When
            viewModel.onEvent(WatchPairingUiEvent.OnUseCodeClicked)

            // Then
            assertEquals(WatchPairingStep.CODE, viewModel.uiState.value.step)
            assertNull(viewModel.uiState.value.code)
            assertTrue(viewModel.uiState.value.isWaiting)
        }

    @Test
    fun `GIVEN the code on screen WHEN it expires THEN shows a new code by itself and polls it`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
        viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))
        viewModel.onEvent(WatchPairingUiEvent.OnUseCodeClicked)
        val expiredCode = server.requestedCodes.single().code
        now = codeLifetime.inWholeMilliseconds
        server.requestLatency = 1.seconds

        // When
        server.expire(expiredCode)
        advanceTimeBy(1.seconds)
        runCurrent()

        // Then
        val newCode = server.requestedCodes[1].code
        assertEquals(WatchPairingStep.CODE, viewModel.uiState.value.step)
        assertEquals(newCode, viewModel.uiState.value.code)
        assertEquals(listOf(newCode), server.activePolls)
    }

    @Test
    fun `GIVEN the code on screen WHEN only a minute is left THEN shows a new code by itself`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
        viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))
        viewModel.onEvent(WatchPairingUiEvent.OnUseCodeClicked)
        val firstCode = server.requestedCodes.single().code

        // When
        now = (codeLifetime - 1.minutes).inWholeMilliseconds
        advanceTimeBy(codeLifetime - 1.minutes)
        runCurrent()

        // Then
        val newCode = server.requestedCodes.last().code
        assertNotEquals(firstCode, newCode)
        assertEquals(newCode, viewModel.uiState.value.code)
    }

    @Test
    fun `GIVEN the phone approved WHEN the code expires before the watch hears THEN the phone shows out of reach`() =
        runPairingTest {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
            viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.APPROVED))
            val code = server.requestedCodes.single().code

            // When
            server.expire(code)
            runCurrent()

            // Then
            assertEquals(WatchPairingStep.PHONE_OUT_OF_REACH, viewModel.uiState.value.step)
        }

    @Test
    fun `GIVEN the code on screen WHEN Cancel is clicked THEN goes back to the phone out of reach and keeps polling`() =
        runPairingTest {
            // Given
            prepareScenario()
            viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
            viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))
            viewModel.onEvent(WatchPairingUiEvent.OnUseCodeClicked)
            val code = server.requestedCodes.single().code

            // When
            viewModel.onEvent(WatchPairingUiEvent.OnCodeCancelled)

            // Then
            assertEquals(WatchPairingStep.PHONE_OUT_OF_REACH, viewModel.uiState.value.step)
            assertEquals(listOf(code), server.activePolls)
        }

    @Test
    fun `GIVEN connected WHEN Continue is clicked THEN opens the balances alone`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
        server.approve(server.requestedCodes.single().code)

        // When
        viewModel.onEvent(WatchPairingUiEvent.OnContinueClicked)

        // Then
        assertEquals(listOf<Route>(Route.WatchBalances), navigator.backStack.value)
        assertEquals("watch_connected_continue_clicked", trackedEvents.last())
    }

    @Test
    fun `GIVEN connected WHEN a late phone reply arrives THEN stays connected`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
        server.approve(server.requestedCodes.single().code)

        // When
        viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))

        // Then
        assertEquals(WatchPairingStep.CONNECTED, viewModel.uiState.value.step)
    }

    @Test
    fun `GIVEN connected WHEN Try again is clicked THEN ignores it without tracking it`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
        server.approve(server.requestedCodes.single().code)

        // When
        viewModel.onEvent(WatchPairingUiEvent.OnTryAgainClicked)

        // Then
        assertEquals(WatchPairingStep.CONNECTED, viewModel.uiState.value.step)
        assertEquals(listOf("watch_connect_clicked"), trackedEvents)
        assertEquals(1, server.requestedCodes.size)
        assertEquals(1, actions.size)
    }

    @Test
    fun `GIVEN the welcome WHEN Connect is clicked THEN tracks it`() = runPairingTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)

        // Then
        assertEquals(listOf("watch_connect_clicked"), trackedEvents)
    }

    @Test
    fun `GIVEN the phone signed out WHEN Connect again is clicked THEN tracks it`() = runPairingTest {
        // Given
        prepareScenario(isSignedOutOnPhone = true)

        // When
        viewModel.onEvent(WatchPairingUiEvent.OnConnectAgainClicked)

        // Then
        assertEquals(listOf("watch_connect_again_clicked"), trackedEvents)
    }

    @Test
    fun `GIVEN connecting WHEN the phone replies THEN tracks nothing more`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)

        // When
        viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))

        // Then
        assertEquals(listOf("watch_connect_clicked"), trackedEvents)
    }

    @Test
    fun `GIVEN the phone out of reach WHEN Try again is clicked THEN tracks it`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
        viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))

        // When
        viewModel.onEvent(WatchPairingUiEvent.OnTryAgainClicked)

        // Then
        assertEquals("watch_connect_try_again_clicked", trackedEvents.last())
    }

    @Test
    fun `GIVEN the phone out of reach WHEN Use code is clicked THEN tracks it`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
        viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))

        // When
        viewModel.onEvent(WatchPairingUiEvent.OnUseCodeClicked)

        // Then
        assertEquals("watch_use_code_clicked", trackedEvents.last())
    }

    @Test
    fun `GIVEN the code on screen WHEN Cancel is clicked THEN tracks it`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
        viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.UNREACHABLE))
        viewModel.onEvent(WatchPairingUiEvent.OnUseCodeClicked)

        // When
        viewModel.onEvent(WatchPairingUiEvent.OnCodeCancelled)

        // Then
        assertEquals("watch_code_cancelled", trackedEvents.last())
    }

    @Test
    fun `GIVEN the local only notice WHEN OK is clicked THEN tracks it`() = runPairingTest {
        // Given
        prepareScenario()
        viewModel.onEvent(WatchPairingUiEvent.OnConnectClicked)
        viewModel.onEvent(WatchPairingUiEvent.OnPhoneReplied(PhoneReply.LOCAL_ONLY))

        // When
        viewModel.onEvent(WatchPairingUiEvent.OnLocalOnlyAcknowledged)

        // Then
        assertEquals("watch_local_only_acknowledged", trackedEvents.last())
    }

    private val codeLifetime = 5.minutes

    private val pierre = UserProfile(
        displayName = "Pierre",
        avatarColor = AvatarColor.PEACH,
        photo = ProfilePhoto.Remote("https://example.com/pierre.jpg"),
    )

    /*
     * Why: the code on the watch's screen is replaced a minute before it ends for as long as it shows, a timer that
     * never runs out; the ViewModel's scope ends with the test, as leaving the screen ends it, so runTest can finish.
     */
    private fun runPairingTest(body: suspend TestScope.() -> Unit) = runTest(testDispatcher) {
        try {
            body()
        } finally {
            if (::viewModel.isInitialized) viewModel.viewModelScope.cancel()
        }
    }

    private fun TestScope.prepareScenario(
        online: Boolean = true,
        isSignedOutOnPhone: Boolean = false,
        profile: UserProfile? = null,
    ) {
        now = 0L
        dismissCount = 0
        isOnline = MutableStateFlow(online)
        signedOutOnPhone = MutableStateFlow(isSignedOutOnPhone)
        profiles = FakeProfileRepository(initialProfile = profile)
        server = FakeWatchPairingServer(currentTimeMillis = { now })
        navigator = BackStackNavigator(startRoute = Route.WatchConnect)
        viewModel = WatchPairingViewModel(
            navigator = navigator,
            requestWatchPairing = { server.request() },
            awaitWatchPairing = { code -> server.await(code) },
            signInWithWatchPairing = { tokenHash -> server.signIn(tokenHash) },
            dismissSignedOutOnPhone = {
                dismissCount++
                signedOutOnPhone.value = false
            },
            currentTimeMillis = { now },
            observeSignedOutOnPhone = { signedOutOnPhone },
            observeProfile = ObserveProfileUseCase(profiles),
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            trackEvent = { name, _ -> trackedEvents += name },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}

// Why: the server's side of a pairing: codes it hands out, the polls waiting on them, and the approvals that end them.
private class FakeWatchPairingServer(
    private val currentTimeMillis: () -> Long,
) {
    var requestError: Throwable? = null
    var signInFailuresLeft = 0
    var requestLatency: Duration = Duration.ZERO
    var beforeRequest: () -> Unit = {}
    val requestedCodes = mutableListOf<WatchPairingCode>()
    val signedInTokenHashes = mutableListOf<String>()

    // The codes polled right now: a poll leaves the list once it ends or is cancelled.
    val activePolls = mutableListOf<String>()
    private val outcomes = mutableMapOf<String, CompletableDeferred<Result<String>>>()

    suspend fun request(): Result<WatchPairingCode> {
        beforeRequest()
        if (requestLatency > Duration.ZERO) delay(requestLatency)
        requestError?.let { return Result.failure(it) }
        val index = requestedCodes.size
        val code = WatchPairingCode(
            code = "K7P4Q$index",
            secret = "secret-$index",
            expiresAt = currentTimeMillis() + 5.minutes.inWholeMilliseconds,
            pollInterval = 2.seconds,
        )
        requestedCodes += code
        return Result.success(code)
    }

    suspend fun await(code: WatchPairingCode): Result<String> {
        activePolls += code.code
        try {
            return outcomeOf(code.code).await()
        } finally {
            activePolls -= code.code
        }
    }

    suspend fun signIn(tokenHash: String): Result<Unit> {
        signedInTokenHashes += tokenHash
        if (signInFailuresLeft == 0) return Result.success(Unit)
        signInFailuresLeft--
        return Result.failure(IllegalStateException("refused"))
    }

    fun approve(code: String) {
        outcomeOf(code).complete(Result.success(tokenHashOf(code)))
    }

    fun expire(code: String) {
        outcomeOf(code).complete(Result.failure(IllegalStateException("code expired")))
    }

    private fun outcomeOf(code: String): CompletableDeferred<Result<String>> =
        outcomes.getOrPut(code) { CompletableDeferred() }

    companion object {
        fun tokenHashOf(code: String): String = "token-hash-$code"
    }
}
