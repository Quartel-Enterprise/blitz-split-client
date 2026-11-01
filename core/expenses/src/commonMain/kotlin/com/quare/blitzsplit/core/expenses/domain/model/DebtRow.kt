package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.groups.domain.model.Group

// Why: a pair in Saldos, already netted: "Você deve a Bruno", "Ana te deve" or "Diego deve a Bruno".
data class DebtRow(
    val fromMemberId: String,
    val toMemberId: String,
    // Why: null for someone no longer in the group ("Ex-membro"), whose face is a blank avatar.
    val fromName: String?,
    val fromFace: GroupFace?,
    val toName: String?,
    val toFace: GroupFace?,
    val amountMinor: Long,
    val kind: DebtKind,
)

// Why: a pair as Saldos and the dialogs of "Sair do grupo" and "Apagar grupo" show it.
fun debtRowOf(
    debt: Debt,
    group: Group,
    meId: String?,
): DebtRow {
    val from = group.members.firstOrNull { it.id == debt.fromMemberId }
    val to = group.members.firstOrNull { it.id == debt.toMemberId }
    return DebtRow(
        fromMemberId = debt.fromMemberId,
        toMemberId = debt.toMemberId,
        fromName = from?.name,
        fromFace = from?.let(::faceOf),
        toName = to?.name,
        toFace = to?.let(::faceOf),
        amountMinor = debt.amountMinor,
        kind = when (meId) {
            debt.fromMemberId -> DebtKind.YOU_OWE
            debt.toMemberId -> DebtKind.OWES_YOU
            else -> DebtKind.BETWEEN_OTHERS
        },
    )
}
