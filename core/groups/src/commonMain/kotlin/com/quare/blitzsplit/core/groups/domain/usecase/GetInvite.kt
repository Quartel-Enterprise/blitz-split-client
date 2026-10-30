package com.quare.blitzsplit.core.groups.domain.usecase

import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteTarget

fun interface GetInvite {
    // Why: null for a link that was changed or a group that was deleted; a failure when the server can't be reached.
    suspend operator fun invoke(target: InviteTarget): Result<Invite?>
}
