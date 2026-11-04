package com.quare.blitzsplit.feature.inappupdate.presentation.model

sealed interface InAppUpdateDownloadUiAction {
    // Why: "Não deu para baixar a atualização. Tente de novo mais tarde."
    data object ShowDownloadFailed : InAppUpdateDownloadUiAction
}
