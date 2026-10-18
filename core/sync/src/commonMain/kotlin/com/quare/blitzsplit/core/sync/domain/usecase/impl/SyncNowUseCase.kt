package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.sync.SyncCoordinator
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow

class SyncNowUseCase(
    private val coordinator: SyncCoordinator,
) : SyncNow {
    override suspend fun invoke(): Result<Unit> = coordinator.syncNow()
}
