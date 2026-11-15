@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.core.releasenotes.domain.model.ReleaseDate
import com.quare.blitzsplit.feature.releasenotes.presentation.model.ReleaseNotesUiEvent
import com.quare.blitzsplit.feature.releasenotes.presentation.viewmodel.ReleaseNotesViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

class JsReleaseNotes(
    // Why: the version's whole name, "3.1.0-beta_2", which keys its card.
    val version: String,
    // Why: "3.1.0", the card's title; a beta shares it with the version it leads to.
    val name: String,
    // Why: the beta's number for its "Beta 2" badge, null for a stable version.
    val betaNumber: Int?,
    // Why: the day it came out as YYYY-MM-DD, which the page writes in the reader's format.
    val date: String,
    val isInstalled: Boolean,
    val notes: Array<String>,
)

class JsReleaseNotesState(
    // Why: the versions on screen, newest first, already narrowed to the web's notes and to the switch.
    val releases: Array<JsReleaseNotes>,
    val showsBeta: Boolean,
    val showsBetaSwitch: Boolean,
    val showsSkeleton: Boolean,
    val showsLoadFailed: Boolean,
    val showsEmpty: Boolean,
    // Why: the empty list says to turn the switch on when that would show something.
    val hasOnlyBetaReleases: Boolean,
)

class ReleaseNotesPresenter {
    private val holder = ViewModelHolder(ReleaseNotesViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsReleaseNotesState) -> Unit): JsWatch {
        val states = viewModel.uiState.map { state ->
            JsReleaseNotesState(
                releases = state.visibleReleases
                    .map { release ->
                        JsReleaseNotes(
                            version = release.version.fullName,
                            name = release.version.name,
                            betaNumber = release.version.beta,
                            date = release.date.toIsoDate(),
                            isInstalled = release.isInstalled,
                            notes = release.notes.toTypedArray(),
                        )
                    }.toTypedArray(),
                showsBeta = state.showsBeta == true,
                showsBetaSwitch = state.showsBetaSwitch,
                showsSkeleton = state.showsSkeleton,
                showsLoadFailed = state.showsLoadFailed,
                showsEmpty = state.showsEmpty,
                hasOnlyBetaReleases = state.hasOnlyBetaReleases,
            )
        }
        val watch = FlowWatcher(states).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(ReleaseNotesUiEvent.OnBackClicked)
    }

    fun retry() {
        viewModel.onEvent(ReleaseNotesUiEvent.OnRetryClicked)
    }

    fun toggleBeta(isOn: Boolean) {
        viewModel.onEvent(ReleaseNotesUiEvent.OnShowBetaToggled(isOn))
    }

    fun dispose() {
        holder.clear()
    }

    private fun ReleaseDate.toIsoDate(): String = listOf(
        year.toString().padStart(YEAR_DIGITS, '0'),
        month.toString().padStart(PART_DIGITS, '0'),
        day.toString().padStart(PART_DIGITS, '0'),
    ).joinToString("-")

    private companion object {
        const val YEAR_DIGITS = 4
        const val PART_DIGITS = 2
    }
}
