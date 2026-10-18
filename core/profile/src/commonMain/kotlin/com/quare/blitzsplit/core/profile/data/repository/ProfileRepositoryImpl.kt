package com.quare.blitzsplit.core.profile.data.repository

import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfileRemoteDataSource
import com.quare.blitzsplit.core.profile.data.mapper.ProfileMapper
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(
    private val local: ProfileLocalDataSource,
    private val remote: ProfileRemoteDataSource,
    private val mapper: ProfileMapper,
    private val currentTimeMillis: CurrentTimeMillis,
) : ProfileRepository {
    override fun observeProfile(): Flow<UserProfile?> = local.observe().map { entity -> entity?.let(mapper::toDomain) }

    override suspend fun saveProfile(profile: UserProfile) {
        local.upsert(mapper.toEntity(profile = profile, updatedAt = currentTimeMillis(), pendingSync = true))
    }

    override suspend fun uploadPendingProfile(userId: String): Result<Unit> = suspendRunCatching {
        val pending = local.get()?.takeIf { it.pendingSync } ?: return@suspendRunCatching
        remote.upsert(mapper.toDto(entity = pending, userId = userId))
        local.markSynced(syncedUpdatedAt = pending.updatedAt)
    }

    override suspend fun downloadProfile(userId: String): Result<Unit> = suspendRunCatching {
        val remoteProfile = remote.fetch(userId) ?: return@suspendRunCatching
        local.applyRemote(mapper.toEntity(remoteProfile))
    }
}
