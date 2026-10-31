package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.sync.SyncCoordinator
import com.quare.blitzsplit.core.sync.domain.usecase.ChangeInviteLink

class ChangeInviteLinkUseCase(
    private val coordinator: SyncCoordinator,
) : ChangeInviteLink {
    override suspend fun invoke(groupId: String): Result<Unit> = coordinator.rotateInviteCode(groupId)
}
