package com.quare.blitzsplit.core.profile.data.repository

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLinkRemoteDataSource
import com.quare.blitzsplit.core.profile.domain.model.ProfileLink
import com.quare.blitzsplit.core.profile.domain.model.SharedProfile
import com.quare.blitzsplit.core.profile.domain.repository.ProfileLinkRepository
import com.quare.blitzsplit.core.utils.suspendRunCatching

class ProfileLinkRepositoryImpl(
    private val remote: ProfileLinkRemoteDataSource,
) : ProfileLinkRepository {
    // Why: a malformed code is no profile, without a trip to the server.
    override suspend fun resolve(code: String): Result<SharedProfile?> = suspendRunCatching {
        if (!ProfileLink.isCode(code)) return@suspendRunCatching null
        remote.fetchSharedProfile(code)?.let { dto ->
            SharedProfile(
                displayName = dto.displayName,
                avatarColor = AvatarColor.fromCodeOrNull(dto.avatarColor) ?: AvatarColor.default,
                photoUrl = dto.avatarUrl,
                userId = dto.userId,
            )
        }
    }
}
