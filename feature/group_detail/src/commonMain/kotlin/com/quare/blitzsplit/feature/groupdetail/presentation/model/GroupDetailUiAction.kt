package com.quare.blitzsplit.feature.groupdetail.presentation.model

sealed interface GroupDetailUiAction {
    // Why: "Saldos" and a bill's own screen are not built yet.
    data object ShowComingSoon : GroupDetailUiAction
}
