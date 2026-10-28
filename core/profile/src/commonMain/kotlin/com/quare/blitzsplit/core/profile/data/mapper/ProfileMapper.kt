package com.quare.blitzsplit.core.profile.data.mapper

import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.data.dto.ProfileDto
import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity

interface ProfileMapper {
    fun toDomain(entity: ProfileEntity): UserProfile

    fun toEntity(
        profile: UserProfile,
        updatedAt: Long,
        pendingSync: Boolean,
        ownerUserId: String?,
    ): ProfileEntity

    fun toDto(
        entity: ProfileEntity,
        userId: String,
    ): ProfileDto

    fun toEntity(
        dto: ProfileDto,
        ownerUserId: String,
    ): ProfileEntity
}
