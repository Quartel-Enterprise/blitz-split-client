package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.sync.SyncCoordinator
import com.quare.blitzsplit.core.sync.domain.usecase.SignOut

class SignOutUseCase(
    private val coordinator: SyncCoordinator,
) : SignOut {
    override suspend fun invoke(): Result<Unit> = coordinator.signOut()
}
