package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

data class ProfileLinkUiState(
    val stage: ProfileLinkStage,
    // Why: the person's, only once STRANGER.
    val name: String?,
    val initials: String?,
    val avatarColor: AvatarColor?,
    val photoUrl: String?,
    // Why: "Invite Ana to a group" needs a profile on this device, which a link opened over the welcome hasn't.
    val canInvite: Boolean,
) {
    companion object {
        val LOADING = ProfileLinkUiState(
            stage = ProfileLinkStage.LOADING,
            name = null,
            initials = null,
            avatarColor = null,
            photoUrl = null,
            canInvite = false,
        )
    }
}
