package com.quare.blitzsplit.feature.groupdetail.presentation.model

sealed interface GroupDetailUiAction {
    // Why: "Saldos" is not built yet.
    data object ShowComingSoon : GroupDetailUiAction
}
