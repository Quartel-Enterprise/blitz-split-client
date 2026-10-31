package com.quare.blitzsplit.feature.groupdetail.presentation.model

sealed interface BalancesUiAction {
    // Why: "Sair do grupo" and "Apagar grupo" are not built yet.
    data object ShowComingSoon : BalancesUiAction
}
