package com.quare.blitzsplit.feature.contacts.presentation.model

sealed interface ContactInviteUiAction {
    data class CopyLink(
        val link: String,
    ) : ContactInviteUiAction

    // Why: each platform writes the message around the link, with the group's name and whom it is for.
    data class ShareLink(
        val link: String,
        val groupName: String,
        val name: String?,
    ) : ContactInviteUiAction
}
