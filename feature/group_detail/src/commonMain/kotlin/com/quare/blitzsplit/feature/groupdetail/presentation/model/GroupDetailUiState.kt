package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency

data class GroupDetailUiState(
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
    // Why: "R$ 2.418,60 no total".
    val totalSpentMinor: Long,
    // Why: the most recent day first.
    val days: List<ExpenseDay>,
    /*
     * Why: "Esta conta foi apagada", once a bill someone else deleted closed while it was open. State rather than an
     * action, so a screen drawn again only after it is back on top still shows it.
     */
    val showsBillDeletedNotice: Boolean,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
) {
    val extraMemberCount: Int
        get() = (memberCount - faces.size).coerceAtLeast(0)

    // Why: "Nenhuma despesa ainda", with "Rachar conta" and "Convidar pessoas" in it.
    val isEmpty: Boolean
        get() = isLoaded && days.isEmpty()

    // Why: the empty state has its own "Rachar conta".
    val showsSplitBillButton: Boolean
        get() = isLoaded && days.isNotEmpty()
}
