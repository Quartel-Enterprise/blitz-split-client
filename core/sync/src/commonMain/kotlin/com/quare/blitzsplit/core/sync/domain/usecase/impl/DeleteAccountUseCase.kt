package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.sync.SyncCoordinator
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteAccount

class DeleteAccountUseCase(
    private val coordinator: SyncCoordinator,
) : DeleteAccount {
    override suspend fun invoke(): Result<Unit> = coordinator.deleteAccount()
}
