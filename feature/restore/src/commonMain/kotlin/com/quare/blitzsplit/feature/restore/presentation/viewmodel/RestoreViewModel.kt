package com.quare.blitzsplit.feature.restore.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.RestoreWithRecoveryCode
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.signin.ProviderSignIn
import com.quare.blitzsplit.core.sync.signin.ProviderSignInController
import com.quare.blitzsplit.core.utils.RecoveryWords
import com.quare.blitzsplit.feature.restore.presentation.mapper.toRecoveryCodeFromQr
import com.quare.blitzsplit.feature.restore.presentation.mapper.toRestoreError
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreError
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiAction
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiEvent
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreUiState
import com.quare.blitzsplit.feature.restore.presentation.model.RestoreWordField
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class RestoreViewModel(
    private val restoreWithRecoveryCode: RestoreWithRecoveryCode,
    private val navigator: Navigator,
    providerSignIn: ProviderSignIn,
    connectivityObserver: NetworkConnectivityObserver,
    trackEvent: TrackEvent,
) : TrackedViewModel<RestoreUiEvent>(trackEvent) {
    val uiState: StateFlow<RestoreUiState>
        field = MutableStateFlow(
            RestoreUiState(
                words = List(RecoveryWords.COUNT) {
                    RestoreWordField(
                        text = "",
                        isDeadEnd = false,
                    )
                },
                focusedWordIndex = null,
                suggestions = emptyList(),
                isCodeComplete = false,
                isOnline = true,
                isRestoring = false,
                connectingProvider = null,
                error = null,
            ),
        )

    val uiAction: SharedFlow<RestoreUiAction>
        field = MutableSharedFlow<RestoreUiAction>()

    val stateWatcher: FlowWatcher<RestoreUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<RestoreUiAction> = FlowWatcher(uiAction)

    private val signIn = ProviderSignInController(
        providerSignIn = providerSignIn,
        navigator = navigator,
        scope = viewModelScope,
        connectingProvider = { uiState.value.connectingProvider },
        showConnecting = ::showConnecting,
        launchNativeSignIn = { provider -> uiAction.emit(RestoreUiAction.LaunchNativeSignIn(provider)) },
    )

    init {
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
        signIn.observeBrowserSignIn(isShown = { navigator.backStack.value.lastOrNull() == Route.Restore })
    }

    override fun handleEvent(event: RestoreUiEvent) {
        when (event) {
            is RestoreUiEvent.OnWordChanged -> changeWord(event.index, event.text)
            is RestoreUiEvent.OnWordFocused -> focusWord(event.index)
            is RestoreUiEvent.OnSuggestionClicked -> pickSuggestion(event.word)
            RestoreUiEvent.OnRestoreClicked -> handleRestoreClicked()
            RestoreUiEvent.OnScanQrClicked -> emitAction(RestoreUiAction.OpenQrScanner)
            is RestoreUiEvent.OnQrScanned -> handleQrScanned(event.content)
            is RestoreUiEvent.OnProviderClicked -> handleProviderClicked(event.provider)
            is RestoreUiEvent.OnIdTokenReceived -> handleIdToken(event)
            RestoreUiEvent.OnNativeSignInFailed -> signIn.onNativeSignInFailed()
            RestoreUiEvent.OnNativeSignInCancelled -> signIn.onNativeSignInCancelled()
            RestoreUiEvent.OnStartFreshClicked -> navigator.navigate(Route.ProfileSetup)
            RestoreUiEvent.OnBackClicked -> navigator.navigateBack()
            RestoreUiEvent.OnErrorDismissed -> uiState.update { it.copy(error = null) }
        }
    }

    private fun changeWord(
        index: Int,
        text: String,
    ) {
        if (index !in uiState.value.words.indices) return
        val tokens = RecoveryWords.split(text)
        when {
            tokens.size >= RecoveryWords.COUNT -> fillWords(
                from = 0,
                tokens = RecoveryWords.findCodeIn(text) ?: RecoveryWords.findTypedWordsIn(text) ?: tokens,
            )

            tokens.size > 1 -> fillWords(
                from = index,
                tokens = tokens,
            )

            else -> typeWord(index, text, tokens.firstOrNull().orEmpty())
        }
    }

    // Why: a space or the keyboard's next after a word completes it, as picking it would.
    private fun typeWord(
        index: Int,
        text: String,
        token: String,
    ) {
        val word = RecoveryWords.wordOf(token)
        if (word != null && !text.last().isLetter()) {
            setWords(index, listOf(word))
            focusNext(index)
        } else {
            setWords(index, listOf(token))
        }
    }

    private fun fillWords(
        from: Int,
        tokens: List<String>,
    ) {
        val words = tokens.take(RecoveryWords.COUNT - from).map { RecoveryWords.wordOf(it) ?: it }
        setWords(from, words)
        // Why: a last word not whole yet ("legal wi") is still being typed, so the keyboard stays on it.
        val lastIndex = from + words.lastIndex
        val isLastWordWhole = RecoveryWords.isWord(words.lastOrNull().orEmpty())
        val nextEmpty = uiState.value.words.indexOfFirst { it.text.isEmpty() }
        when {
            !isLastWordWhole -> emitAction(RestoreUiAction.FocusWord(lastIndex))
            nextEmpty >= 0 -> emitAction(RestoreUiAction.FocusWord(nextEmpty))
        }
    }

    // Why: leaving a field shows the whole word its first letters stand for.
    private fun focusWord(index: Int) {
        if (index !in uiState.value.words.indices) return
        val previous = uiState.value.focusedWordIndex
        if (previous != null && previous != index) {
            val text = uiState.value.words[previous].text
            val word = RecoveryWords.wordOf(text)
            // Why: only a prefix that grows is rewritten, so tapping a field keeps the restore's error in view.
            if (word != null && word != text) setWords(previous, listOf(word))
        }
        uiState.update { it.copy(focusedWordIndex = index) }
        updateSuggestions()
    }

    private fun pickSuggestion(word: String) {
        val index = uiState.value.focusedWordIndex ?: return
        setWords(index, listOf(word))
        focusNext(index)
    }

    private fun focusNext(index: Int) {
        if (index + 1 < RecoveryWords.COUNT) emitAction(RestoreUiAction.FocusWord(index + 1))
    }

    private fun setWords(
        from: Int,
        texts: List<String>,
    ) {
        uiState.update { state ->
            val words = state.words.toMutableList()
            texts.forEachIndexed { offset, text ->
                words[from + offset] = RestoreWordField(
                    text = text,
                    isDeadEnd = RecoveryWords.isDeadEnd(text),
                )
            }
            state.copy(
                words = words,
                isCodeComplete = RecoveryWords.normalize(words.joinToString(" ") { it.text }) != null,
                error = null,
            )
        }
        updateSuggestions()
    }

    // Why: a field already holding the one word its letters can become needs no suggestion.
    private fun updateSuggestions() {
        uiState.update { state ->
            val text = state.focusedWordIndex?.let { state.words[it].text }.orEmpty()
            val suggestions = RecoveryWords.suggestionsFor(text).takeUnless { it == listOf(text) }.orEmpty()
            state.copy(suggestions = suggestions)
        }
    }

    // Why: a QR that isn't a recovery code (another app's, an old code's) says so instead of restoring nothing.
    private fun handleQrScanned(content: String) {
        val text = content.toRecoveryCodeFromQr()
        val code = RecoveryWords.findCodeIn(text)
        val tokens = code ?: RecoveryWords.split(text).take(RecoveryWords.COUNT)
        // Why: a QR replaces all 12 fields, so no word typed before stays mixed with what it held.
        setWords(0, tokens + List(RecoveryWords.COUNT - tokens.size) { "" })
        if (code == null) {
            uiState.update { it.copy(error = RestoreError.INVALID_CODE) }
        } else {
            restore()
        }
    }

    private fun handleRestoreClicked() {
        if (uiState.value.isOnline) {
            trackEvent("restore_code_submitted", emptyMap())
        } else {
            trackEvent("retry_clicked", mapOf("source" to "restore"))
        }
        restore()
    }

    private fun restore() {
        val state = uiState.value
        if (!state.canRestore) return
        uiState.update {
            it.copy(
                isRestoring = true,
                error = null,
            )
        }
        viewModelScope.launch {
            signIn
                .signInDirectly { restoreWithRecoveryCode(state.words.joinToString(" ") { it.text }) }
                .onFailure { throwable -> uiState.update { it.copy(error = throwable.toRestoreError()) } }
            uiState.update { it.copy(isRestoring = false) }
        }
    }

    private fun handleProviderClicked(provider: AccountProvider) {
        if (!uiState.value.areAccountButtonsEnabled) return
        signIn.signIn(provider)
    }

    private fun handleIdToken(event: RestoreUiEvent.OnIdTokenReceived) {
        signIn.onIdTokenReceived(
            provider = event.provider,
            idToken = event.idToken,
            rawNonce = event.rawNonce,
            fullName = event.fullName,
            authorizationCode = event.authorizationCode,
        )
    }

    private fun showConnecting(
        provider: AccountProvider?,
        hasFailed: Boolean,
    ) {
        uiState.update {
            it.copy(
                connectingProvider = provider,
                error = RestoreError.SIGN_IN_FAILED.takeIf { hasFailed },
            )
        }
    }

    private fun emitAction(action: RestoreUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }
}
