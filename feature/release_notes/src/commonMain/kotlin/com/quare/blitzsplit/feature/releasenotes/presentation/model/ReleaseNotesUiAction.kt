package com.quare.blitzsplit.feature.releasenotes.presentation.model

sealed interface ReleaseNotesUiAction {
    // Why: a GitHub release page, which each platform opens its own way: the browser, or a new tab on the web.
    data class OpenUrl(
        val url: String,
    ) : ReleaseNotesUiAction
}
