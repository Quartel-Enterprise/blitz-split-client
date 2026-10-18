package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.sync.SyncCoordinator
import com.quare.blitzsplit.core.sync.domain.usecase.StartSync

class StartSyncUseCase(
    private val coordinator: SyncCoordinator,
) : StartSync {
    override fun invoke() {
        coordinator.start()
    }
}
