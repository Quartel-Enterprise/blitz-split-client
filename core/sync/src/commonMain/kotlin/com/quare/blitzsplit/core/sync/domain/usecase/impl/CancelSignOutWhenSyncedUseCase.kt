package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.sync.SyncCoordinator
import com.quare.blitzsplit.core.sync.domain.usecase.CancelSignOutWhenSynced

class CancelSignOutWhenSyncedUseCase(
    private val coordinator: SyncCoordinator,
) : CancelSignOutWhenSynced {
    override suspend fun invoke() {
        coordinator.cancelSignOutWhenSynced()
    }
}
