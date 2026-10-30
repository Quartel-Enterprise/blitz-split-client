package com.quare.blitzsplit.feature.members.presentation.model

sealed interface MembersUiAction {
    // Why: the group's expenses and balances (flow 6) are not built yet.
    data object ShowComingSoon : MembersUiAction

    data class CopyLink(
        val link: String,
    ) : MembersUiAction

    // Why: "Compartilhar com a Carla"; each platform writes the message around the link in its own words.
    data class SharePersonalLink(
        val link: String,
        val memberName: String,
        val groupName: String,
    ) : MembersUiAction

    // Why: the device couldn't save the change, which is rare enough to just say so.
    data object ShowChangeError : MembersUiAction
}
