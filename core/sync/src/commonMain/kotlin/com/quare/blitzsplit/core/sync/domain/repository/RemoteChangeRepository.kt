package com.quare.blitzsplit.core.sync.domain.repository

import com.quare.blitzsplit.core.sync.domain.model.RemoteChange
import kotlinx.coroutines.flow.Flow

interface RemoteChangeRepository {
    // Why: listens while collected, online only; Joined first once listening, and again each time the channel joins back.
    fun observeChanges(): Flow<RemoteChange>
}
