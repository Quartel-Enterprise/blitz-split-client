package com.quare.blitzsplit.core.profile.data.mapper

import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.profile.data.dto.NotificationPreferencesDto
import com.quare.blitzsplit.core.provider.room.entity.NotificationPreferencesEntity
import com.quare.blitzsplit.core.provider.room.entity.SINGLE_ROW_ID

class NotificationPreferencesMapperImpl : NotificationPreferencesMapper {
    override fun toDomain(entity: NotificationPreferencesEntity?): NotificationPreferences {
        if (entity == null) return NotificationPreferences.DEFAULT
        return NotificationPreferences(
            enabledTypes = buildSet {
                if (entity.newExpense) add(NotificationType.NEW_EXPENSE)
                if (entity.paymentMarked) add(NotificationType.PAYMENT_MARKED)
                if (entity.settleReminders) add(NotificationType.SETTLE_REMINDERS)
                if (entity.invites) add(NotificationType.INVITES)
            },
        )
    }

    override fun toEntity(
        preferences: NotificationPreferences,
        updatedAt: Long,
        ownerUserId: String?,
    ): NotificationPreferencesEntity = NotificationPreferencesEntity(
        id = SINGLE_ROW_ID,
        newExpense = NotificationType.NEW_EXPENSE in preferences.enabledTypes,
        paymentMarked = NotificationType.PAYMENT_MARKED in preferences.enabledTypes,
        settleReminders = NotificationType.SETTLE_REMINDERS in preferences.enabledTypes,
        invites = NotificationType.INVITES in preferences.enabledTypes,
        updatedAt = updatedAt,
        pendingSync = true,
        ownerUserId = ownerUserId,
    )

    override fun toDto(
        entity: NotificationPreferencesEntity,
        userId: String,
    ): NotificationPreferencesDto = NotificationPreferencesDto(
        userId = userId,
        newExpense = entity.newExpense,
        paymentMarked = entity.paymentMarked,
        settleReminders = entity.settleReminders,
        invites = entity.invites,
        updatedAt = entity.updatedAt,
    )

    override fun toEntity(
        dto: NotificationPreferencesDto,
        ownerUserId: String,
    ): NotificationPreferencesEntity = NotificationPreferencesEntity(
        id = SINGLE_ROW_ID,
        newExpense = dto.newExpense,
        paymentMarked = dto.paymentMarked,
        settleReminders = dto.settleReminders,
        invites = dto.invites,
        updatedAt = dto.updatedAt,
        pendingSync = false,
        ownerUserId = ownerUserId,
    )
}
