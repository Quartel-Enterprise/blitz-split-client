package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AvatarColor

data class GroupsUiState(
    val displayName: String,
    val avatarInitial: String,
    val avatarColor: AvatarColor,
    // Why: a photo picked offline is a data URI until its upload.
    val photoUrl: String?,
    // Why: null while there is no Google or Apple account, both before the first sync and while anonymous.
    val accountProvider: AccountProvider?,
    val syncBadge: SyncBadge?,
    val showsRecoveryCodePrompt: Boolean,
) {
    val canRetrySync: Boolean
        get() = syncBadge == SyncBadge.FAILED
}
