package com.quare.blitzsplit.feature.groupdetail.presentation.model

sealed interface GroupDetailUiAction {
    // Why: the device couldn't sync or reach the server, so nobody left; trying again may work.
    data object ShowLeaveError : GroupDetailUiAction

    // Why: the same for "Apagar grupo": the group is still there.
    data object ShowDeleteError : GroupDetailUiAction

    // Why: the device couldn't delete the payment, which is rare enough to just say so; it still counts.
    data object ShowChangeError : GroupDetailUiAction
}
