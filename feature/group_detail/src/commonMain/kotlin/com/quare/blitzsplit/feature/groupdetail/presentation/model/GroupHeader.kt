package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.GroupFace
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency

// Why: the top of the group's screen, the same over Despesas and Saldos.
data class GroupHeader(
    val groupName: String,
    val groupInitials: String,
    val groupColor: AvatarColor,
    val currency: GroupCurrency,
    // Why: up to four faces; the rest is "+N" (extraMemberCount).
    val faces: List<GroupFace>,
    // Why: "6 membros".
    val memberCount: Int,
    // Why: "1 ainda não entrou".
    val notJoinedCount: Int,
    // Why: "R$ 2.418,60 no total", the bills only, not the payments.
    val totalSpentMinor: Long,
    // Why: "Seu saldo neste grupo", null while there is no bill to owe anything for.
    val myBalance: MemberBalance?,
    // Why: "Todo mundo acertou", the card that offers "Apagar grupo".
    val isEveryoneSettled: Boolean,
) {
    val extraMemberCount: Int
        get() = (memberCount - faces.size).coerceAtLeast(0)
}
