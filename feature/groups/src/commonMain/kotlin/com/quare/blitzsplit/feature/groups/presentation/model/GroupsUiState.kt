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
    // Why: the most recently changed first. The empty home ("Crie seu primeiro grupo") only once they were read.
    val groups: List<GroupRow>,
    val hasLoadedGroups: Boolean,
    /*
     * Why: the snackbar of a group someone else deleted, which a sync took off this device. State rather than an
     * action, so it waits until the home is drawn and on top.
     */
    val deletedGroupNotice: DeletedGroupNotice?,
) {
    val showsEmptyState: Boolean
        get() = hasLoadedGroups && groups.isEmpty()

    val canRetrySync: Boolean
        get() = syncBadge == SyncBadge.FAILED
}
