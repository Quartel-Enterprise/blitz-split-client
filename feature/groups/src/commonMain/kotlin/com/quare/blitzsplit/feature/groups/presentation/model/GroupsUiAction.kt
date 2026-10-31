package com.quare.blitzsplit.feature.groups.presentation.model

sealed interface GroupsUiAction {
    /*
     * Why: the other tabs, the global Saldos that "A receber" and "A pagar" open (flow 8), and "Rachar conta" picking
     * a group (7.1i) are not built yet.
     */
    data object ShowComingSoon : GroupsUiAction
}
