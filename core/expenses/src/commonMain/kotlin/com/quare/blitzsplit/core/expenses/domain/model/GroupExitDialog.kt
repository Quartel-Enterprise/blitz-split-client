package com.quare.blitzsplit.core.expenses.domain.model

// Why: what "Sair do grupo" and "Apagar grupo" open, from the ⋮ menu or "Todo mundo acertou".
sealed interface GroupExitDialog {
    /*
     * Why: "Acerte antes de sair": this person still owes or is owed, which their rows show. Its button is "Acertar"
     * while they owe someone (Registrar pagamento with whoever they owe the most), "Ver saldos" otherwise.
     */
    data class LeaveBlocked(
        val groupName: String,
        // Why: what they owe first, then what they're owed.
        val debts: List<DebtRow>,
        val canSettle: Boolean,
    ) : GroupExitDialog

    // Why: "Sair do Churrasco de sábado?", with a settled balance.
    data class ConfirmLeave(
        val groupName: String,
        // Why: "para os outros 8 membros".
        val otherMemberCount: Int,
        /*
         * Why: nobody else with an account is in the group, so the server deletes it once this person leaves:
         * "ele some de vez" rather than "suas despesas continuam com o seu nome".
         */
        val deletesGroup: Boolean,
    ) : GroupExitDialog

    // Why: "Ainda há saldo em aberto", with every pair still owing ("Faltam 2 acertos") and "Ver saldos".
    data class DeleteBlocked(
        val groupName: String,
        val debts: List<DebtRow>,
    ) : GroupExitDialog

    // Why: "Apagar Aniversário da Ana?", once nobody owes anything.
    data class ConfirmDelete(
        val groupName: String,
        // Why: "as 14 despesas".
        val billCount: Int,
        // Why: "para os 5 membros".
        val memberCount: Int,
    ) : GroupExitDialog
}
