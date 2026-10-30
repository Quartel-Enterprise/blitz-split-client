package com.quare.blitzsplit.core.groups.domain.repository

import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.JoinChoice
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome
import com.quare.blitzsplit.core.model.AvatarColor

// Why: online only. Whoever opens a link isn't in the group yet, so the device has nothing of it to show offline.
interface InviteRepository {
    // Why: null for a link that was changed or a group that was deleted.
    suspend fun fetchInvite(code: String): Result<Invite?>

    // Why: the member takes avatarColor, so each person has the same color in every group.
    suspend fun joinGroup(
        code: String,
        choice: JoinChoice,
        avatarColor: AvatarColor,
    ): Result<JoinOutcome>
}
