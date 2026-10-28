package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.sync.SyncCoordinator
import com.quare.blitzsplit.core.sync.domain.usecase.SignOutWhenSynced

class SignOutWhenSyncedUseCase(
    private val coordinator: SyncCoordinator,
) : SignOutWhenSynced {
    override fun invoke() {
        coordinator.signOutWhenSynced()
    }
}
