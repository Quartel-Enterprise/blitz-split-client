package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteTarget
import com.quare.blitzsplit.core.groups.domain.repository.InviteRepository
import com.quare.blitzsplit.core.groups.domain.usecase.GetInvite

class GetInviteUseCase(
    private val repository: InviteRepository,
) : GetInvite {
    override suspend fun invoke(target: InviteTarget): Result<Invite?> = repository.fetchInvite(target)
}
