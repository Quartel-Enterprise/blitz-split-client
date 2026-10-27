package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.sync.SyncCoordinator
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import kotlinx.coroutines.flow.Flow

class ObserveSyncStateUseCase(
    private val coordinator: SyncCoordinator,
) : ObserveSyncState {
    override fun invoke(): Flow<SyncState> = coordinator.observeState()
}
