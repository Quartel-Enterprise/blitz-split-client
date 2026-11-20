package com.quare.blitzsplit.feature.watchpairing.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfile
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.watch.domain.model.WatchPairingCode
import com.quare.blitzsplit.core.watch.domain.usecase.AwaitWatchPairing
import com.quare.blitzsplit.core.watch.domain.usecase.DismissSignedOutOnPhone
import com.quare.blitzsplit.core.watch.domain.usecase.ObserveSignedOutOnPhone
import com.quare.blitzsplit.core.watch.domain.usecase.RequestWatchPairing
import com.quare.blitzsplit.core.watch.domain.usecase.SignInWithWatchPairing
import com.quare.blitzsplit.feature.watchpairing.presentation.model.PhoneReply
import com.quare.blitzsplit.feature.watchpairing.presentation.model.WatchPairingError
import com.quare.blitzsplit.feature.watchpairing.presentation.model.WatchPairingStep
import com.quare.blitzsplit.feature.watchpairing.presentation.model.WatchPairingUiAction
import com.quare.blitzsplit.feature.watchpairing.presentation.model.WatchPairingUiEvent
import com.quare.blitzsplit.feature.watchpairing.presentation.model.WatchPairingUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/*
 * Why: the watch's way to the account (15.9 to 15.13). One flow for both cases: the watch asks the server for a code
 * and polls it; a nearby phone gets the code over the paired channel and approves it on its own, while out of reach
 * the watch shows it for any device with the account to type in Profile › Connect watch.
 */
class WatchPairingViewModel(
    private val navigator: Navigator,
    private val requestWatchPairing: RequestWatchPairing,
    private val awaitWatchPairing: AwaitWatchPairing,
    private val signInWithWatchPairing: SignInWithWatchPairing,
    private val dismissSignedOutOnPhone: DismissSignedOutOnPhone,
    private val currentTimeMillis: CurrentTimeMillis,
    private val connectivityObserver: NetworkConnectivityObserver,
    observeSignedOutOnPhone: ObserveSignedOutOnPhone,
    observeProfile: ObserveProfile,
    trackEvent: TrackEvent,
) : TrackedViewModel<WatchPairingUiEvent>(trackEvent) {
    val uiState: StateFlow<WatchPairingUiState>
        field = MutableStateFlow(
            WatchPairingUiState(
                step = WatchPairingStep.WELCOME,
                code = null,
                error = null,
                isOnline = true,
                profileName = null,
                profileColor = null,
                profilePhotoUrl = null,
            ),
        )

    val uiAction: SharedFlow<WatchPairingUiAction>
        field = MutableSharedFlow<WatchPairingUiAction>()

    val stateWatcher: FlowWatcher<WatchPairingUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<WatchPairingUiAction> = FlowWatcher(uiAction)

    // Why: the code asked last, reused while it still has time, so Try again and Use code share one.
    private var pairingCode: WatchPairingCode? = null
    private var pollJob: Job? = null

    // Why: the code pollJob waits on, so a newer code starts its own poll.
    private var polledCode: WatchPairingCode? = null
    private var phoneWait: Job? = null
    private var request: Job? = null

    private val phoneTimeout = 20.seconds

    // Why: a code about to end isn't handed out, since typing it on another device takes a while.
    private val minTimeLeft = 60.seconds

    init {
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
        observeSignedOutOnPhone()
            .onEach { isSignedOut ->
                uiState.update { state ->
                    if (isSignedOut && state.step == WatchPairingStep.WELCOME) {
                        state.copy(step = WatchPairingStep.SIGNED_OUT_ON_PHONE)
                    } else {
                        state
                    }
                }
            }.launchIn(viewModelScope)
        observeProfile()
            .onEach { profile ->
                uiState.update {
                    it.copy(
                        profileName = profile?.displayName,
                        profileColor = profile?.avatarColor,
                        profilePhotoUrl = profile?.photo?.toImageUrl(),
                    )
                }
            }.launchIn(viewModelScope)
    }

    override fun handleEvent(event: WatchPairingUiEvent) {
        when (event) {
            WatchPairingUiEvent.OnConnectClicked -> connectNearby()
            WatchPairingUiEvent.OnConnectAgainClicked -> connectNearby()
            is WatchPairingUiEvent.OnPhoneReplied -> handlePhoneReply(event.reply)
            WatchPairingUiEvent.OnTryAgainClicked -> connectNearby()
            WatchPairingUiEvent.OnUseCodeClicked -> showCode()
            WatchPairingUiEvent.OnCodeCancelled -> showStep(WatchPairingStep.PHONE_OUT_OF_REACH)
            WatchPairingUiEvent.OnLocalOnlyAcknowledged -> showStep(WatchPairingStep.WELCOME)
            WatchPairingUiEvent.OnContinueClicked -> navigator.replaceAll(Route.WatchBalances)
        }
    }

    // Why: once connected, nothing goes back to pairing: a late reply or tap would undo the step on screen.
    override fun isAccepted(event: WatchPairingUiEvent): Boolean =
        uiState.value.step != WatchPairingStep.CONNECTED || event == WatchPairingUiEvent.OnContinueClicked

    private fun connectNearby() {
        dismissSignedOutOnPhone()
        if (!uiState.value.isOnline) {
            uiState.update {
                it.copy(
                    step = WatchPairingStep.WELCOME,
                    error = WatchPairingError.NO_INTERNET,
                )
            }
            return
        }
        uiState.update {
            it.copy(
                step = WatchPairingStep.CONNECTING,
                error = null,
            )
        }
        request?.cancel()
        request = viewModelScope.launch {
            val code = loadCode() ?: return@launch
            startPolling(code)
            uiAction.emit(WatchPairingUiAction.SendCodeToPhone(code.code))
            waitForPhone()
        }
    }

    private fun showCode() {
        uiState.update {
            it.copy(
                step = WatchPairingStep.CODE,
                code = pairingCode?.takeIf(::hasTimeLeft)?.code,
            )
        }
        request?.cancel()
        request = viewModelScope.launch {
            val code = loadCode() ?: return@launch
            uiState.update { it.copy(code = code.code) }
            startPolling(code)
            // Why: a code with less than a minute left is never on screen, so a new one replaces it then.
            delay((code.expiresAt - currentTimeMillis()).milliseconds - minTimeLeft)
            if (pairingCode == code) pairingCode = null
            if (uiState.value.step == WatchPairingStep.CODE) showCode()
        }
    }

    private fun handlePhoneReply(reply: PhoneReply) {
        when (reply) {
            // Why: the poll brings the approval; the wait for the phone is over.
            PhoneReply.APPROVED -> phoneWait?.cancel()

            PhoneReply.LOCAL_ONLY -> {
                stopPairing()
                showStep(WatchPairingStep.LOCAL_ONLY)
            }

            PhoneReply.UNREACHABLE -> phoneOutOfReach()
        }
    }

    // Why: a phone that got the code but never answers (its app killed mid-way) counts as out of reach.
    private fun waitForPhone() {
        phoneWait?.cancel()
        phoneWait = viewModelScope.launch {
            delay(phoneTimeout)
            phoneOutOfReach()
        }
    }

    private fun phoneOutOfReach() {
        phoneWait?.cancel()
        if (uiState.value.step == WatchPairingStep.CONNECTING) showStep(WatchPairingStep.PHONE_OUT_OF_REACH)
    }

    // Why: null after showing the welcome's error, which the screen then shows instead of waiting.
    private suspend fun loadCode(): WatchPairingCode? {
        pairingCode?.takeIf(::hasTimeLeft)?.let { return it }
        return requestWatchPairing()
            .onSuccess { pairingCode = it }
            .onFailure {
                // Why: read now, since the connection may have dropped during the request.
                val isOnline = connectivityObserver.observe().first()
                val error = if (isOnline) WatchPairingError.FAILED else WatchPairingError.NO_INTERNET
                uiState.update { state ->
                    state.copy(
                        step = WatchPairingStep.WELCOME,
                        error = error,
                    )
                }
            }.getOrNull()
    }

    private fun hasTimeLeft(code: WatchPairingCode): Boolean =
        code.expiresAt - currentTimeMillis() > minTimeLeft.inWholeMilliseconds

    private fun startPolling(code: WatchPairingCode) {
        if (pollJob?.isActive == true && polledCode == code) return
        pollJob?.cancel()
        polledCode = code
        /*
         * Why: until the code ends, each approved poll brings a fresh token that replaces the last one, so a sign in
         * that fails polls again for the next; a code that ends meanwhile goes as any expired code.
         */
        pollJob = viewModelScope.launch {
            while (true) {
                val tokenHash = awaitWatchPairing(code).getOrElse {
                    codeExpired(code)
                    return@launch
                }
                phoneWait?.cancel()
                request?.cancel()
                if (signInWithWatchPairing(tokenHash).isSuccess) {
                    showStep(WatchPairingStep.CONNECTED)
                    return@launch
                }
                delay(code.pollInterval)
            }
        }
    }

    /*
     * Why: the code on screen gets a new one by itself; a watch still waiting for its phone (which may have approved,
     * then the watch lost its connection) stops waiting, so Try again or Use code asks for a new one.
     */
    private fun codeExpired(code: WatchPairingCode) {
        if (pairingCode == code) pairingCode = null
        when (uiState.value.step) {
            WatchPairingStep.CODE -> showCode()
            WatchPairingStep.CONNECTING -> showStep(WatchPairingStep.PHONE_OUT_OF_REACH)
            else -> Unit
        }
    }

    private fun stopPairing() {
        request?.cancel()
        pollJob?.cancel()
        phoneWait?.cancel()
    }

    private fun showStep(step: WatchPairingStep) {
        uiState.update {
            it.copy(
                step = step,
                error = null,
            )
        }
    }
}
