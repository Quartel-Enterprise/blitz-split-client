package com.quare.blitzsplit.feature.releasenotes.presentation.model

sealed interface ReleaseNotesUiAction {
    // Why: a GitHub release page, which each platform opens its own way: a Custom Tab, Safari, or a new browser tab.
    data class OpenUrl(
        val url: String,
    ) : ReleaseNotesUiAction
}
