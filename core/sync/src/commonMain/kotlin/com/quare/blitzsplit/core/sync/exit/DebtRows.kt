package com.quare.blitzsplit.core.sync.exit

import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.DebtKind
import com.quare.blitzsplit.core.expenses.domain.model.DebtRow
import com.quare.blitzsplit.core.expenses.domain.model.GroupFace
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials

fun faceOf(member: GroupMember): GroupFace = GroupFace(
    initials = Initials.firstOf(member.name),
    color = member.avatarColor,
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
