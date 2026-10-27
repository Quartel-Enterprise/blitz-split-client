package com.quare.blitzsplit.core.sync.domain.usecase

import com.quare.blitzsplit.core.model.SyncState
import kotlinx.coroutines.flow.Flow

fun interface ObserveSyncState {
    operator fun invoke(): Flow<SyncState>
}
