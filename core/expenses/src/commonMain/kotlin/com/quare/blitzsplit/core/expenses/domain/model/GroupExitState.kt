package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: where "Sair do grupo" and "Apagar grupo" stand, the same over Despesas and Saldos and on a row of the home.
 * Not a screen's UiState: GroupExitController produces it and each screen's UiState, in its feature, embeds it.
 */
data class GroupExitState(
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

    companion object {
        // Why: online and with no dialog open, until the device says otherwise.
        val idle = GroupExitState(
            isOnline = true,
            dialog = null,
            isRunning = false,
            deletedNotice = null,
        )
    }
}
