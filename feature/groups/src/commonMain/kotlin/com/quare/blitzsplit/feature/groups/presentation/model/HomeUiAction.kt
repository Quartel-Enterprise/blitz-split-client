package com.quare.blitzsplit.feature.groups.presentation.model

sealed interface HomeUiAction {
    // Why: "Ler cupom fiscal" (7.1e) and the tabs Contatos and Atividade are not built yet.
    data object ShowComingSoon : HomeUiAction

    // Why: the device couldn't delete the payment "Desfazer" undoes; it still counts.
    data object ShowChangeError : HomeUiAction
}
