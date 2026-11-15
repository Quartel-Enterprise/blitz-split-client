package com.quare.blitzsplit.feature.releasenotes.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion

sealed interface ReleaseNotesUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: the screen view under it coming back says it.
            OnBackClicked -> EventAnalytics.NotTracked

            OnRetryClicked -> EventAnalytics.Automatic("release_notes_retry_clicked")

            is OnShowBetaToggled -> EventAnalytics.Automatic(
                "release_notes_beta_toggled",
                mapOf("is_on" to isOn),
            )

            OnAllReleasesClicked -> EventAnalytics.Automatic("release_notes_github_clicked")

            is OnReleaseClicked -> EventAnalytics.Automatic(
                "release_notes_release_clicked",
                mapOf("version" to version.fullName),
            )
        }

    data object OnBackClicked : ReleaseNotesUiEvent

    data object OnRetryClicked : ReleaseNotesUiEvent

    // Why: "Show beta versions".
    data class OnShowBetaToggled(
        val isOn: Boolean,
    ) : ReleaseNotesUiEvent

    // Why: the top bar's GitHub button, every release on GitHub.
    data object OnAllReleasesClicked : ReleaseNotesUiEvent

    // Why: a version's card, its own release on GitHub.
    data class OnReleaseClicked(
        val version: AppVersion,
    ) : ReleaseNotesUiEvent
}
