package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.sync.domain.model.RemoteChange
import com.quare.blitzsplit.core.sync.domain.repository.RemoteChangeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/*
 * The changes a test sends, as the server's real time would; `listeners` counts the channels listening right now and
 * `joinCount` every attempt to listen. The next `failingListens` attempts fail, as a channel the server refuses.
 */
internal class FakeRemoteChangeRepository : RemoteChangeRepository {
    val changes = MutableSharedFlow<RemoteChange>(extraBufferCapacity = 16)
    var listeners = 0
        private set
    var joinCount = 0
        private set
    var failingListens = 0

    override fun observeChanges(): Flow<RemoteChange> = flow {
        joinCount++
        if (failingListens > 0) {
            failingListens--
            throw IllegalStateException("channel refused")
        }
        listeners++
        try {
            emitAll(changes)
        } finally {
            listeners--
        }
    }
}
