package com.quare.blitzsplit.core.profile.data.repository

import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfilePhotoRemoteDataSource
import com.quare.blitzsplit.core.profile.data.datasource.ProfileRemoteDataSource
import com.quare.blitzsplit.core.profile.data.dto.ProfileDto
import com.quare.blitzsplit.core.profile.data.mapper.ProfileMapper
import com.quare.blitzsplit.core.profile.domain.model.ProfileCodeTakenException
import com.quare.blitzsplit.core.profile.domain.model.ProfileLink
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.RandomCode
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
    private val randomCode: RandomCode,
) : ProfileRepository {
    override fun observeProfile(): Flow<UserProfile?> = local.observe().map { entity -> entity?.let(mapper::toDomain) }

    override fun observeHasPendingChanges(): Flow<Boolean> = local
        .observe()
        .map { entity -> entity?.pendingSync == true }
        .distinctUntilChanged()

    // Why: only a copy an account owns has a link; a profile made before its account has none until a sync names it.
    override fun observeShareCode(): Flow<String?> = local
        .observe()
        .map { entity -> entity?.takeIf { it.ownerUserId != null }?.shareCode }
        .distinctUntilChanged()

    // Why: an edit keeps the copy its owner's, so one made to another user's copy still gives way to the account.
    override suspend fun saveProfile(profile: UserProfile) {
        local.upsert(
            mapper.toEntity(
                profile = profile,
                updatedAt = currentTimeMillis(),
                pendingSync = true,
                ownerUserId = local.get()?.ownerUserId,
            ),
        )
    }

    override suspend fun uploadPendingProfile(userId: String): Result<Unit> = suspendRunCatching {
        val pending = local.get()?.takeIf { it.pendingSync && it.ownerUserId == userId } ?: return@suspendRunCatching
        val photoUrl = pending.pendingPhoto?.let { jpeg -> uploadPhoto(userId, jpeg, pending.updatedAt) }
            ?: pending.photoUrl
        upsertWithFreeShareCode(
            entity = pending.copy(photoUrl = photoUrl),
            userId = userId,
        )
        local.markSynced(
            syncedUpdatedAt = pending.updatedAt,
            photoUrl = photoUrl,
        )
    }

    /*
     * Why: the copy is checked against the user this sync runs under, not a switch seen in memory, so a sync
     * already in flight, a switch the app died in or a failed pull can't push another user's copy over it.
     */
    override suspend fun downloadProfile(userId: String): Result<Unit> = suspendRunCatching {
        val remoteProfile = remote.fetch(userId)
        val stored = local.get()
        val owner = stored?.ownerUserId
        if (owner != null && owner != userId) {
            replaceOtherUsersCopy(
                stored = stored,
                userId = userId,
                remoteProfile = remoteProfile,
            )
        } else {
            local.claimUnowned(userId)
            remoteProfile?.let {
                local.applyRemote(
                    mapper.toEntity(
                        dto = it,
                        ownerUserId = userId,
                    ),
                )
            }
            remoteProfile?.shareCode?.let {
                local.applyRemoteShareCode(
                    ownerUserId = userId,
                    shareCode = it,
                )
            }
        }
        // Why: the account's link exists from its first sync on, before the profile goes up or any answer comes back.
        val current = local.get()
        if (current?.ownerUserId == userId && current.shareCode == null) {
            local.createShareCode(
                ownerUserId = userId,
                shareCode = randomCode(ProfileLink.CODE_LENGTH),
            )
        }
    }

    /*
     * Why: another profile may have the code this device made, which the server refuses with the whole upsert; a clash
     * is so rare (about one in 10^17) that a few tries always find a free one. Only a profile the server doesn't have
     * yet can clash, since one already up keeps the server's code, which the download brought here. After the last
     * refusal the code changes too, so the next sync doesn't start with a taken one.
     */
    private suspend fun upsertWithFreeShareCode(
        entity: ProfileEntity,
        userId: String,
    ) {
        var toPush = entity
        var attempt = 1
        while (true) {
            try {
                return remote.upsert(
                    mapper.toDto(
                        entity = toPush,
                        userId = userId,
                    ),
                )
            } catch (taken: ProfileCodeTakenException) {
                val newCode = randomCode(ProfileLink.CODE_LENGTH)
                local.replaceShareCode(
                    ownerUserId = userId,
                    shareCode = newCode,
                )
                toPush = toPush.copy(shareCode = newCode)
                if (attempt == SHARE_CODE_ATTEMPTS) throw taken
                attempt++
            }
        }
    }

    /*
     * Why: an uploaded photo is the previous user's object, so the account starts without it; another picture stays.
     * The previous user's link never becomes the account's: it takes the server's, or a new one.
     */
    private suspend fun replaceOtherUsersCopy(
        stored: ProfileEntity,
        userId: String,
        remoteProfile: ProfileDto?,
    ) {
        local.applyRemoteShareCode(
            ownerUserId = checkNotNull(stored.ownerUserId),
            shareCode = remoteProfile?.shareCode,
        )
        if (remoteProfile != null) {
            local.replaceOtherUsersCopy(
                mapper.toEntity(
                    dto = remoteProfile,
                    ownerUserId = userId,
                ),
            )
        } else {
            local.reclaimOtherUsersCopy(
                ownerUserId = userId,
                updatedAt = currentTimeMillis(),
                photoUrl = stored.photoUrl?.takeUnless(photoRemote::isUploaded),
            )
        }
    }

    override suspend fun clear() {
        local.clear()
    }

    // Why: the URL of a new photo is the same path, so the version makes every cache fetch it again.
    private suspend fun uploadPhoto(
        userId: String,
        jpeg: ByteArray,
        updatedAt: Long,
    ): String = "${photoRemote.upload(
        userId = userId,
        jpeg = jpeg,
    )}?v=$updatedAt"

    private companion object {
        const val SHARE_CODE_ATTEMPTS = 3
    }
}
