package com.quare.blitzsplit.feature.groups.presentation.model

sealed interface GroupsUiAction {
    // Why: the tabs Contatos and Atividade are not built yet.
    data object ShowComingSoon : GroupsUiAction

    // Why: the device couldn't sync or reach the server, so nobody left; trying again may work.
    data object ShowLeaveError : GroupsUiAction

    // Why: the same for "Apagar grupo": the group is still there.
    data object ShowDeleteError : GroupsUiAction

    // Why: the device couldn't delete the payment "Desfazer" undoes, which is rare enough to just say so; it still counts.
    data object ShowChangeError : GroupsUiAction
}
