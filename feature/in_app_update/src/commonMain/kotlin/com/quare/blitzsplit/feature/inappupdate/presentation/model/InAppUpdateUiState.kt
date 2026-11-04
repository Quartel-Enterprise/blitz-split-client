package com.quare.blitzsplit.feature.inappupdate.presentation.model

data class InAppUpdateUiState(
    // Why: "Versão 2.1.0" under the title; null when the store didn't say which.
    val versionName: String?,
)
