package com.quare.blitzsplit.feature.groupdetail.presentation.model

sealed interface BalancesUiAction {
    // Why: "Sair do grupo" and "Apagar grupo" are not built yet.
    data object ShowComingSoon : BalancesUiAction

    // Why: the device couldn't delete the payment, which is rare enough to just say so; it still counts.
    data object ShowChangeError : BalancesUiAction
}
