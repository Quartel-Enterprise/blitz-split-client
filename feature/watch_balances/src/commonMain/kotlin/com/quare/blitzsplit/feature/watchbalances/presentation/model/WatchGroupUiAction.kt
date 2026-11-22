package com.quare.blitzsplit.feature.watchbalances.presentation.model

sealed interface WatchGroupUiAction {
    // Why: the platform's own confirmation (ConfirmationDialog on Wear OS), which answers with OnRecordedShown.
    data object ShowPaymentRecorded : WatchGroupUiAction
}
