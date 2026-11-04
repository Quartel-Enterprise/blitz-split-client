package com.quare.blitzsplit.core.inappupdate.domain.model

// Why: Google Play's flexible update, downloaded while the app stays in use; only Android has one.
sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState

    data class Downloading(
        val percent: Int,
    ) : UpdateDownloadState

    data object Downloaded : UpdateDownloadState

    data object Failed : UpdateDownloadState
}
