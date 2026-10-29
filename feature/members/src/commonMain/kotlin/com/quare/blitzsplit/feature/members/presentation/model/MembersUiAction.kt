package com.quare.blitzsplit.feature.members.presentation.model

sealed interface MembersUiAction {
    // Why: a personal invite (flow 3) and the group's expenses and balances (flow 6) are not built yet.
    data object ShowComingSoon : MembersUiAction

    // Why: the device couldn't save the change, which is rare enough to just say so.
    data object ShowChangeError : MembersUiAction
}
