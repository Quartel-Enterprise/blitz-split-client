package com.quare.blitzsplit.core.sync.domain.usecase

import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.JoinChoice
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome

fun interface JoinGroup {
    /*
     * Why: once joined, the group is on the device before this returns, so the group's screen can open right away. A
     * failure leaves nothing to undo: trying again answers AlreadyMember if the join itself went through.
     */
    suspend operator fun invoke(
        invite: Invite,
        choice: JoinChoice,
    ): Result<JoinOutcome>
}
