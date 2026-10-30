package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface GroupDetailUiEvent : UiEvent {
    data object OnBackClicked : GroupDetailUiEvent

    // Why: "Rachar conta", the button and the empty state's.
    data object OnSplitBillClicked : GroupDetailUiEvent

    // Why: "Convidar pessoas", the top bar's and the empty state's.
    data object OnInviteClicked : GroupDetailUiEvent

    // Why: the "Membros" tab and the faces under the name.
    data object OnMembersClicked : GroupDetailUiEvent

    // Why: the "Saldos" tab.
    data object OnBalancesTabClicked : GroupDetailUiEvent

    // Why: a bill on the list, whose screen (7.3) is not built yet.
    data class OnExpenseClicked(
        val expenseId: String,
    ) : GroupDetailUiEvent
}
