package com.quare.blitzsplit.feature.appdownload.presentation.model

sealed interface AppDownloadUiAction {
    // Why: the platform's clipboard, then its "Link copied".
    data class CopyLink(
        val link: String,
    ) : AppDownloadUiAction
}
