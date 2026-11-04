package com.quare.blitzsplit.core.sync.data.datasource

import com.quare.blitzsplit.core.sync.data.dto.RemoteChangeDto
import kotlinx.coroutines.flow.Flow

interface RemoteChangeRemoteDataSource {
    fun observeChanges(): Flow<RemoteChangeDto>
}
