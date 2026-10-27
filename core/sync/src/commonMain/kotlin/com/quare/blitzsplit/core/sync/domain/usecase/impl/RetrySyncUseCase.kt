package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.sync.SyncCoordinator
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync

class RetrySyncUseCase(
    private val coordinator: SyncCoordinator,
) : RetrySync {
    override fun invoke() {
        coordinator.retry()
    }
}
