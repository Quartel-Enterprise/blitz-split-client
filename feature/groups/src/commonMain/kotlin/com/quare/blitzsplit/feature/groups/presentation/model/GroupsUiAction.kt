package com.quare.blitzsplit.feature.groups.presentation.model

sealed interface GroupsUiAction {
    /*
     * Why: the other tabs, the global Saldos that "A receber" and "A pagar" open (flow 8), and "Rachar conta" picking
     * a group (7.1i) are not built yet.
     */
    data object ShowComingSoon : GroupsUiAction

    // Why: the device couldn't sync or reach the server, so nobody left; trying again may work.
    data object ShowLeaveError : GroupsUiAction

    // Why: the same for "Apagar grupo": the group is still there.
    data object ShowDeleteError : GroupsUiAction
}
