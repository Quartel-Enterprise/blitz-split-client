package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.sync.domain.model.RemoteChange
import com.quare.blitzsplit.core.sync.domain.repository.RemoteChangeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

// The changes a test sends, as the server's real time would; `listeners` counts the channels listening right now.
class FakeRemoteChangeRepository : RemoteChangeRepository {
    val changes = MutableSharedFlow<RemoteChange>(extraBufferCapacity = 16)
    var listeners = 0
        private set
    var joinCount = 0
        private set

    override fun observeChanges(): Flow<RemoteChange> = changes
        .onStart {
            listeners++
            joinCount++
        }.onCompletion { listeners-- }
}
