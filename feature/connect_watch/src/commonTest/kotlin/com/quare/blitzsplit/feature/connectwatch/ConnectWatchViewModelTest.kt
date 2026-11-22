package com.quare.blitzsplit.feature.connectwatch

import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.watch.domain.model.ExpiredWatchPairingException
import com.quare.blitzsplit.core.watch.domain.model.InvalidWatchPairingCodeException
import com.quare.blitzsplit.core.watch.domain.model.TooManyWatchPairingAttemptsException
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingNeedsAccountException
import com.quare.blitzsplit.feature.connectwatch.presentation.model.ConnectWatchError
import com.quare.blitzsplit.feature.connectwatch.presentation.model.ConnectWatchUiEvent
import com.quare.blitzsplit.feature.connectwatch.presentation.viewmodel.ConnectWatchViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectWatchViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val approvedCodes = mutableListOf<String>()
    private lateinit var isOnline: MutableStateFlow<Boolean>
    private lateinit var serverAnswer: CompletableDeferred<Result<Unit>>
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: ConnectWatchViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN online WHEN the screen opens THEN shows an empty code with Connect disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("", state.code)
            assertTrue(state.isOnline)
            assertFalse(state.isConnecting)
            assertNull(state.error)
            assertFalse(state.isConnected)
            assertFalse(state.canConnect)
            assertFalse(state.showsOfflineNotice)
        }

    @Test
    fun `GIVEN the screen open WHEN k7p 4qx is typed THEN keeps K7P4QX and enables Connect`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val typed = "k7p 4qx"

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged(typed))

            // Then
            assertEquals("K7P4QX", viewModel.uiState.value.code)
            assertTrue(viewModel.uiState.value.canConnect)
        }

    @Test
    fun `GIVEN the screen open WHEN a longer code with dashes is pasted THEN keeps its first six`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val pasted = "k7p-4qx-9z"

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged(pasted))

            // Then
            assertEquals("K7P4QX", viewModel.uiState.value.code)
        }

    @Test
    fun `GIVEN the screen open WHEN only five characters are typed THEN Connect stays disabled`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val typed = "K7P 4Q"

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged(typed))

            // Then
            assertEquals("K7P4Q", viewModel.uiState.value.code)
            assertFalse(viewModel.uiState.value.canConnect)
        }

    @Test
    fun `GIVEN no internet WHEN a full code is typed THEN Connect stays disabled and says it is offline`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))

            // Then
            assertFalse(viewModel.uiState.value.isOnline)
            assertFalse(viewModel.uiState.value.canConnect)
            assertTrue(viewModel.uiState.value.showsOfflineNotice)
        }

    @Test
    fun `GIVEN no internet and a full code WHEN back online THEN enables Connect without the notice`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))

            // When
            isOnline.value = true

            // Then
            assertTrue(viewModel.uiState.value.isOnline)
            assertTrue(viewModel.uiState.value.canConnect)
            assertFalse(viewModel.uiState.value.showsOfflineNotice)
        }

    @Test
    fun `GIVEN a full code WHEN Connect is clicked and the server approves THEN the watch is connected`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("k7p 4qx"))

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // Then
            assertEquals(listOf("K7P4QX"), approvedCodes)
            assertTrue(viewModel.uiState.value.isConnected)
            assertFalse(viewModel.uiState.value.isConnecting)
            assertNull(viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.canConnect)
            assertEquals(listOf("connect_watch_clicked" to mapOf<String, Any>("result" to "connected")), trackedEvents)
        }

    @Test
    fun `GIVEN a full code WHEN Connect is clicked before the answer THEN shows it connecting`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(awaitsServer = true)
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // Then
            assertTrue(viewModel.uiState.value.isConnecting)
            assertFalse(viewModel.uiState.value.isConnected)
            assertFalse(viewModel.uiState.value.canConnect)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Connect waiting on the server WHEN Connect is clicked again THEN sends the code only once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(awaitsServer = true)
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // Then
            assertEquals(listOf("K7P4QX"), approvedCodes)
            assertTrue(trackedEvents.isEmpty())
        }

    @Test
    fun `GIVEN Connect waiting on the server WHEN the code is changed THEN keeps the code being sent`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(awaitsServer = true)
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4Q"))

            // Then
            assertEquals("K7P4QX", viewModel.uiState.value.code)
            assertTrue(viewModel.uiState.value.isConnecting)
        }

    @Test
    fun `GIVEN the watch connected WHEN the code is changed THEN keeps the code that connected it`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("ABC123"))

            // Then
            assertEquals("K7P4QX", viewModel.uiState.value.code)
            assertTrue(viewModel.uiState.value.isConnected)
        }

    @Test
    fun `GIVEN the watch connected WHEN Connect is clicked again THEN sends nothing more`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))
        viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

        // When
        viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

        // Then
        assertEquals(1, approvedCodes.size)
        assertEquals(1, trackedEvents.size)
    }

    @Test
    fun `GIVEN the watch connected WHEN the connection drops THEN does not say it is offline`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // When
            isOnline.value = false

            // Then
            assertFalse(viewModel.uiState.value.isOnline)
            assertFalse(viewModel.uiState.value.showsOfflineNotice)
        }

    @Test
    fun `GIVEN a wrong code WHEN Connect is clicked THEN says the code is wrong or expired and disables Connect`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(approveResult = Result.failure(InvalidWatchPairingCodeException()))
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // Then
            assertEquals(ConnectWatchError.INVALID_OR_EXPIRED, viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isConnecting)
            assertFalse(viewModel.uiState.value.isConnected)
            assertFalse(viewModel.uiState.value.canConnect)
            assertEquals(
                listOf("connect_watch_clicked" to mapOf<String, Any>("result" to "invalid_or_expired")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN an expired code WHEN Connect is clicked THEN says the code is wrong or expired`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(approveResult = Result.failure(ExpiredWatchPairingException()))
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // Then
            assertEquals(ConnectWatchError.INVALID_OR_EXPIRED, viewModel.uiState.value.error)
            assertEquals(
                listOf("connect_watch_clicked" to mapOf<String, Any>("result" to "invalid_or_expired")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN too many wrong codes WHEN Connect is clicked THEN says there were too many attempts`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(approveResult = Result.failure(TooManyWatchPairingAttemptsException()))
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // Then
            assertEquals(ConnectWatchError.TOO_MANY_ATTEMPTS, viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isConnecting)
            assertEquals(
                listOf("connect_watch_clicked" to mapOf<String, Any>("result" to "too_many_attempts")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a local only profile WHEN Connect is clicked THEN says an account is needed`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(approveResult = Result.failure(WatchPairingNeedsAccountException()))
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // Then
            assertEquals(ConnectWatchError.NEEDS_ACCOUNT, viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isConnected)
            assertEquals(
                listOf("connect_watch_clicked" to mapOf<String, Any>("result" to "needs_account")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the server does not answer WHEN Connect is clicked THEN says it failed and lets Connect again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(approveResult = Result.failure(IllegalStateException("No answer")))
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // Then
            assertEquals(ConnectWatchError.FAILED, viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isConnecting)
            assertEquals(listOf("connect_watch_clicked" to mapOf<String, Any>("result" to "failed")), trackedEvents)
            assertTrue(viewModel.uiState.value.canConnect)
            assertFalse(viewModel.uiState.value.isCodeWrong)
        }

    @Test
    fun `GIVEN a wrong code shown WHEN another code is typed THEN clears the error and enables Connect again`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(approveResult = Result.failure(InvalidWatchPairingCodeException()))
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QY"))

            // Then
            assertEquals("K7P4QY", viewModel.uiState.value.code)
            assertNull(viewModel.uiState.value.error)
            assertTrue(viewModel.uiState.value.canConnect)
        }

    @Test
    fun `GIVEN a wrong code shown WHEN the same code is typed again in another form THEN keeps the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(approveResult = Result.failure(InvalidWatchPairingCodeException()))
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("k7p 4qx"))

            // Then
            assertEquals(ConnectWatchError.INVALID_OR_EXPIRED, viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.canConnect)
        }

    @Test
    fun `GIVEN a wrong code shown WHEN Connect is clicked again THEN sends nothing and tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(approveResult = Result.failure(InvalidWatchPairingCodeException()))
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // Then
            assertEquals(1, approvedCodes.size)
            assertEquals(1, trackedEvents.size)
        }

    @Test
    fun `GIVEN an incomplete code WHEN Connect is clicked THEN sends nothing and tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P"))

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // Then
            assertTrue(approvedCodes.isEmpty())
            assertTrue(trackedEvents.isEmpty())
            assertFalse(viewModel.uiState.value.isConnecting)
        }

    @Test
    fun `GIVEN no internet and a full code WHEN Connect is clicked THEN sends nothing and tracks nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(online = false)
            viewModel.onEvent(ConnectWatchUiEvent.OnCodeChanged("K7P4QX"))

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnConnectClicked)

            // Then
            assertTrue(approvedCodes.isEmpty())
            assertTrue(trackedEvents.isEmpty())
            assertFalse(viewModel.uiState.value.isConnecting)
        }

    @Test
    fun `GIVEN the screen open from Profile WHEN Close is clicked THEN goes back to Profile`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(ConnectWatchUiEvent.OnCloseClicked)

            // Then
            assertEquals(listOf<Route>(Route.Profile), navigator.backStack.value)
            assertEquals(listOf("connect_watch_closed"), trackedEvents.map { it.first })
        }

    private fun prepareScenario(
        online: Boolean = true,
        approveResult: Result<Unit> = Result.success(Unit),
        awaitsServer: Boolean = false,
    ) {
        isOnline = MutableStateFlow(online)
        serverAnswer = CompletableDeferred()
        navigator = BackStackNavigator(startRoute = Route.Profile).also { it.navigate(Route.ConnectWatch) }
        viewModel = ConnectWatchViewModel(
            navigator = navigator,
            approveWatchPairing = { code ->
                approvedCodes += code
                if (awaitsServer) serverAnswer.await() else approveResult
            },
            connectivityObserver = NetworkConnectivityObserver { isOnline },
            trackEvent = { name, params -> trackedEvents += name to params },
        )
    }
}
