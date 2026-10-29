package com.quare.blitzsplit.core.profile.data.mapper

import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.profile.data.dto.NotificationPreferencesDto
import com.quare.blitzsplit.core.provider.room.entity.NotificationPreferencesEntity

interface NotificationPreferencesMapper {
    // Why: no row yet means the person never changed a toggle, so the defaults.
    fun toDomain(entity: NotificationPreferencesEntity?): NotificationPreferences

    fun toEntity(
        preferences: NotificationPreferences,
        updatedAt: Long,
        ownerUserId: String?,
    ): NotificationPreferencesEntity

    fun toDto(
        entity: NotificationPreferencesEntity,
        userId: String,
    ): NotificationPreferencesDto

    fun toEntity(
        dto: NotificationPreferencesDto,
        ownerUserId: String,
    ): NotificationPreferencesEntity
}
