package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor

data class GroupsUiState(
    val avatarInitial: String,
    val avatarColor: AvatarColor,
    val syncBadge: SyncBadge?,
    val showsRecoveryCodePrompt: Boolean,
) {
    val canRetrySync: Boolean
        get() = syncBadge == SyncBadge.FAILED
}
