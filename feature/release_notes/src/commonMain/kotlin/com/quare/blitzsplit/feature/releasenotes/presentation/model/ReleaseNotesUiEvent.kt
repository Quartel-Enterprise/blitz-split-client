package com.quare.blitzsplit.feature.releasenotes.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

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
        }

    data object OnBackClicked : ReleaseNotesUiEvent

    data object OnRetryClicked : ReleaseNotesUiEvent

    // Why: "Show beta versions".
    data class OnShowBetaToggled(
        val isOn: Boolean,
    ) : ReleaseNotesUiEvent
}
