package com.quare.blitzsplit.core.profile.data.repository

import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfilePhotoRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfileRemoteDataSource
import com.quare.blitzsplit.core.profile.data.mapper.ProfileMapper
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(
    private val local: ProfileLocalDataSource,
    private val remote: ProfileRemoteDataSource,
    private val photoRemote: ProfilePhotoRemoteDataSource,
    private val mapper: ProfileMapper,
    private val currentTimeMillis: CurrentTimeMillis,
) : ProfileRepository {
    override fun observeProfile(): Flow<UserProfile?> = local.observe().map { entity -> entity?.let(mapper::toDomain) }

    override fun observeHasPendingChanges(): Flow<Boolean> = local
        .observe()
        .map { entity -> entity?.pendingSync == true }
        .distinctUntilChanged()

    override suspend fun saveProfile(profile: UserProfile) {
        local.upsert(mapper.toEntity(profile = profile, updatedAt = currentTimeMillis(), pendingSync = true))
    }

    override suspend fun uploadPendingProfile(userId: String): Result<Unit> = suspendRunCatching {
        val pending = local.get()?.takeIf { it.pendingSync } ?: return@suspendRunCatching
        val photoUrl = pending.pendingPhoto?.let { jpeg -> uploadPhoto(userId, jpeg, pending.updatedAt) }
            ?: pending.photoUrl
        remote.upsert(mapper.toDto(entity = pending.copy(photoUrl = photoUrl), userId = userId))
        local.markSynced(syncedUpdatedAt = pending.updatedAt, photoUrl = photoUrl)
    }

    override suspend fun downloadProfile(userId: String): Result<Unit> = suspendRunCatching {
        val remoteProfile = remote.fetch(userId) ?: return@suspendRunCatching
        local.applyRemote(mapper.toEntity(remoteProfile))
    }

    override suspend fun clear() {
        local.clear()
    }

    // Why: the URL of a new photo is the same path, so the version makes every cache fetch it again.
    private suspend fun uploadPhoto(
        userId: String,
        jpeg: ByteArray,
        updatedAt: Long,
    ): String = "${photoRemote.upload(userId = userId, jpeg = jpeg)}?v=$updatedAt"
}
