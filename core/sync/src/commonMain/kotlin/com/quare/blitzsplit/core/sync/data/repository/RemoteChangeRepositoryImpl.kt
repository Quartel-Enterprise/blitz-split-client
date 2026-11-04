package com.quare.blitzsplit.core.sync.data.repository

import com.quare.blitzsplit.core.activity.domain.model.ActivityKind
import com.quare.blitzsplit.core.sync.data.datasource.RemoteChangeRemoteDataSource
import com.quare.blitzsplit.core.sync.data.dto.RemoteChangeDto
import com.quare.blitzsplit.core.sync.domain.model.RemoteChange
import com.quare.blitzsplit.core.sync.domain.repository.RemoteChangeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RemoteChangeRepositoryImpl(
    private val remote: RemoteChangeRemoteDataSource,
) : RemoteChangeRepository {
    override fun observeChanges(): Flow<RemoteChange> = remote.observeChanges().map { dto ->
        when (dto) {
            RemoteChangeDto.Joined -> RemoteChange.Joined

            is RemoteChangeDto.ActivityInsert -> RemoteChange.GroupEvent(
                groupId = dto.groupId,
                kind = ActivityKind.fromCodeOrNull(dto.kind),
            )

            is RemoteChangeDto.ProfileUpdate -> RemoteChange.ProfileChanged(userId = dto.id)
        }
    }
}
