package com.quare.blitzsplit.feature.groupdetail.presentation.model

sealed interface GroupDetailUiAction {
    // Why: "Sair do grupo" and "Apagar grupo" are not built yet.
    data object ShowComingSoon : GroupDetailUiAction
}
