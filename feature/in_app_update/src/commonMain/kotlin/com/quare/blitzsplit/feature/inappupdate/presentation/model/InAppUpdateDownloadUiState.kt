package com.quare.blitzsplit.feature.inappupdate.presentation.model

data class InAppUpdateDownloadUiState(
    // Why: "Baixando atualização… 42%" while Google Play downloads, null otherwise.
    val downloadPercent: Int?,
)
