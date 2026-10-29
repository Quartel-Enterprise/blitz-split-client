package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.sync.SyncCoordinator
import com.quare.blitzsplit.core.sync.domain.usecase.UnlinkAccount

class UnlinkAccountUseCase(
    private val coordinator: SyncCoordinator,
) : UnlinkAccount {
    override suspend fun invoke(): Result<Unit> = coordinator.removeSocialIdentity()
}
