package com.quare.blitzsplit.feature.inappupdate.presentation.model

sealed interface InAppUpdateUiAction {
    // Why: the App Store's page of the app (the Mac App Store's on the Mac), which each platform opens.
    data class OpenStore(
        val url: String,
    ) : InAppUpdateUiAction
}
