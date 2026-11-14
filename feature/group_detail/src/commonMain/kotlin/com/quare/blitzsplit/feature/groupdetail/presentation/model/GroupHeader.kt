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
    // Why: drawn in place of the color and the initials, null for none.
    val groupPhotoUrl: String?,
    // Why: the currency of the amounts under the header, the chip picked (the group's default until one is).
    val currency: GroupCurrency,
    // Why: the chips, drawn only with more than one: the currencies the group has money in, its default first.
    val currencies: List<GroupCurrency>,
    // Why: up to four faces; the rest is "+N" (extraMemberCount).
    val faces: List<GroupFace>,
    // Why: "6 membros".
    val memberCount: Int,
    // Why: "1 ainda não entrou".
    val notJoinedCount: Int,
    // Why: "R$ 2.418,60 no total", the bills in the currency picked only, not the payments.
    val totalSpentMinor: Long,
    // Why: "Seu saldo neste grupo" in the currency picked, null while there is no bill to owe anything for, and while the copy isn't whole.
    val myBalance: MemberBalance?,
    // Why: "Todo mundo acertou", the card that offers "Apagar grupo", with every currency settled; false while the account's copy isn't whole.
    val isEveryoneSettled: Boolean,
    // Why: the ⋮ menu's "Fixar no Início" or "Desafixar do Início": this device's user's pin, synced with the account.
    val isPinned: Boolean,
) {
    val extraMemberCount: Int
        get() = (memberCount - faces.size).coerceAtLeast(0)
}
