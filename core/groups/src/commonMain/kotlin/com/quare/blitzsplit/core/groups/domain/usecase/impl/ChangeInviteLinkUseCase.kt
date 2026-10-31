package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.ChangeInviteLink

class ChangeInviteLinkUseCase(
    private val repository: GroupRepository,
) : ChangeInviteLink {
    override suspend fun invoke(groupId: String): Result<Unit> = repository.rotateInviteCode(groupId)
}
