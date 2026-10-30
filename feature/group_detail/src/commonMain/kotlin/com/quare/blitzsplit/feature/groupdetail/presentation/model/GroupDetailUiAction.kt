package com.quare.blitzsplit.feature.groupdetail.presentation.model

sealed interface GroupDetailUiAction {
    // Why: "Saldos", "Sair do grupo" and "Apagar grupo" are not built yet.
    data object ShowComingSoon : GroupDetailUiAction
}
