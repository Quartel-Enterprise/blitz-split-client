package com.quare.blitzsplit.feature.invite.presentation.model

sealed interface InviteUiAction {
    data class CopyLink(
        val link: String,
    ) : InviteUiAction

    // Why: each platform writes the message around the link in its own words, with the group's name.
    data class ShareLink(
        val link: String,
        val groupName: String,
    ) : InviteUiAction

    // Why: the change failed; a sync then brings whichever code the server holds, in case it changed anyway.
    data object ShowChangeLinkFailed : InviteUiAction
}
