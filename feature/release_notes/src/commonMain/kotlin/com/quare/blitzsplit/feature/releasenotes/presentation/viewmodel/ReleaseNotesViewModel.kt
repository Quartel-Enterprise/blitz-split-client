package com.quare.blitzsplit.feature.releasenotes.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.navigateBackFrom
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.releasenotes.domain.usecase.GetReleaseNotesLanguage
import com.quare.blitzsplit.core.releasenotes.domain.usecase.ObserveReleaseNotes
import com.quare.blitzsplit.core.releasenotes.domain.usecase.ObserveShowsBetaReleaseNotes
import com.quare.blitzsplit.core.releasenotes.domain.usecase.RefreshReleaseNotes
import com.quare.blitzsplit.core.settings.domain.usecase.UpdateBetaReleaseNotesChoice
import com.quare.blitzsplit.feature.releasenotes.presentation.model.ReleaseNotesUiEvent
import com.quare.blitzsplit.feature.releasenotes.presentation.model.ReleaseNotesUiState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: "Release notes", what each version changed on the platform this app runs on, from the feed the site's own
 * "What's new" page is made of. The notes the device last saved (usually at launch) show at once, offline too, while
 * the site is asked for newer ones when a day passed since; only a device that never saved them and can't download
 * them shows "Couldn't load".
 */
class ReleaseNotesViewModel(
    private val navigator: Navigator,
    private val refreshReleaseNotes: RefreshReleaseNotes,
    private val updateBetaReleaseNotesChoice: UpdateBetaReleaseNotesChoice,
    getReleaseNotesLanguage: GetReleaseNotesLanguage,
    observeReleaseNotes: ObserveReleaseNotes,
    observeShowsBetaReleaseNotes: ObserveShowsBetaReleaseNotes,
    trackEvent: TrackEvent,
) : TrackedViewModel<ReleaseNotesUiEvent>(trackEvent) {
    val uiState: StateFlow<ReleaseNotesUiState>
        field = MutableStateFlow(
            ReleaseNotesUiState(
                releases = null,
                showsBeta = null,
                hasLoadFailed = false,
            ),
        )

    val stateWatcher: FlowWatcher<ReleaseNotesUiState> = FlowWatcher(uiState)

    /*
     * Why: worked out once for as long as the screen lives, so the notes it watches are the ones every download saves,
     * even when Profile › Language or the device's changes under it.
     */
    private val language = viewModelScope.async(start = CoroutineStart.LAZY) { getReleaseNotesLanguage() }

    init {
        observeShowsBetaReleaseNotes()
            .onEach { showsBeta -> uiState.update { it.copy(showsBeta = showsBeta) } }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            observeReleaseNotes(language.await()).collectIndexed { index, releases ->
                uiState.update { it.copy(releases = releases) }
                /*
                 * Why: the download starts once the saved notes were read, so one that fails at once never shows
                 * "Couldn't load" over a device that holds them.
                 */
                if (index == 0) refresh()
            }
        }
    }

    override fun handleEvent(event: ReleaseNotesUiEvent) {
        when (event) {
            ReleaseNotesUiEvent.OnBackClicked -> navigator.navigateBackFrom(Route.ReleaseNotes)
            ReleaseNotesUiEvent.OnRetryClicked -> retry()
            is ReleaseNotesUiEvent.OnShowBetaToggled -> toggleBeta(event.isOn)
        }
    }

    /*
     * Why: "Try again" only exists on "Couldn't load", and the switch only with the list; one from anywhere else, or
     * that changes nothing, is neither run nor counted.
     */
    override fun isAccepted(event: ReleaseNotesUiEvent): Boolean = when (event) {
        ReleaseNotesUiEvent.OnBackClicked -> true

        ReleaseNotesUiEvent.OnRetryClicked -> uiState.value.showsLoadFailed

        is ReleaseNotesUiEvent.OnShowBetaToggled ->
            uiState.value.showsBetaSwitch &&
                uiState.value.showsBeta != event.isOn
    }

    private fun retry() {
        uiState.update { it.copy(hasLoadFailed = false) }
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            // Why: a download that worked shows through the saved notes; one that failed only matters without any.
            refreshReleaseNotes(language.await()).onFailure { uiState.update { it.copy(hasLoadFailed = true) } }
        }
    }

    // Why: the switch moves at once, so a second tap before the device saved the first is one that changes nothing.
    private fun toggleBeta(isOn: Boolean) {
        uiState.update { it.copy(showsBeta = isOn) }
        viewModelScope.launch { updateBetaReleaseNotesChoice(isOn) }
    }
}
