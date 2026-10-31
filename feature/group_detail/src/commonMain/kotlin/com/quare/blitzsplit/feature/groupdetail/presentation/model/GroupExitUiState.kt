package com.quare.blitzsplit.feature.groupdetail.presentation.model

// Why: "Sair do grupo" and "Apagar grupo", the same over Despesas and Saldos.
data class GroupExitUiState(
    // Why: both need the server, so offline the ⋮ menu shows them disabled with "Precisa de internet".
    val isOnline: Boolean,
    // Why: open while not null.
    val dialog: GroupExitDialog?,
    // Why: the confirmation's button waits while the device syncs and the server answers.
    val isRunning: Boolean,
    // Why: someone else deleted the group while it was open here; the screen stays under it until "Voltar para Grupos".
    val deletedNotice: GroupDeletedNotice?,
) {
    val canLeaveOrDelete: Boolean
        get() = isOnline && !isRunning
}
