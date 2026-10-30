package com.quare.blitzsplit.feature.groupdetail.presentation.model

sealed interface GroupDetailUiAction {
    // Why: "Saldos" is not built yet.
    data object ShowComingSoon : GroupDetailUiAction

    // Why: "Esta conta foi apagada", once a bill someone else deleted closed while it was open.
    data object ShowBillDeleted : GroupDetailUiAction
}
