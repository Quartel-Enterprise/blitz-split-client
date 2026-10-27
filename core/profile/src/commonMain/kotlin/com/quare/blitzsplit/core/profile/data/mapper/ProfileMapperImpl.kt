package com.quare.blitzsplit.core.profile.data.mapper

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.data.dto.ProfileDto
import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID

class ProfileMapperImpl : ProfileMapper {
    override fun toDomain(entity: ProfileEntity): UserProfile = UserProfile(
        displayName = entity.displayName,
        avatarColor = AvatarColor.fromCodeOrNull(entity.avatarColor) ?: AvatarColor.default,
        photo = entity.pendingPhoto?.let(ProfilePhoto::Pending) ?: entity.photoUrl?.let(ProfilePhoto::Remote),
    )

    override fun toEntity(
        profile: UserProfile,
        updatedAt: Long,
        pendingSync: Boolean,
    ): ProfileEntity = ProfileEntity(
        id = SINGLE_ROW_ID,
        displayName = profile.displayName,
        avatarColor = profile.avatarColor.code,
        photoUrl = (profile.photo as? ProfilePhoto.Remote)?.url,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        pendingPhoto = (profile.photo as? ProfilePhoto.Pending)?.jpeg,
    )

    override fun toDto(
        entity: ProfileEntity,
        userId: String,
    ): ProfileDto = ProfileDto(
        id = userId,
        displayName = entity.displayName,
        avatarColor = entity.avatarColor,
        avatarUrl = entity.photoUrl,
        updatedAt = entity.updatedAt,
    )

    override fun toEntity(dto: ProfileDto): ProfileEntity = ProfileEntity(
        id = SINGLE_ROW_ID,
        displayName = dto.displayName,
        avatarColor = dto.avatarColor,
        photoUrl = dto.avatarUrl,
        updatedAt = dto.updatedAt,
        pendingSync = false,
        pendingPhoto = null,
    )
}
