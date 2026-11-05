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
            push = buildSet {
                if (entity.newExpense) add(NotificationType.BILLS)
                if (entity.paymentMarked) add(NotificationType.PAYMENTS)
                if (entity.comments) add(NotificationType.COMMENTS)
                if (entity.invites) add(NotificationType.PEOPLE_AND_GROUPS)
                if (entity.paymentRequests) add(NotificationType.PAYMENT_REQUESTS)
                if (entity.settleReminders) add(NotificationType.BALANCE_REMINDER)
            },
            inApp = buildSet {
                if (entity.inAppBills) add(NotificationType.BILLS)
                if (entity.inAppPayments) add(NotificationType.PAYMENTS)
                if (entity.inAppComments) add(NotificationType.COMMENTS)
                if (entity.inAppPeopleAndGroups) add(NotificationType.PEOPLE_AND_GROUPS)
                if (entity.inAppPaymentRequests) add(NotificationType.PAYMENT_REQUESTS)
            },
        )
    }

    override fun toEntity(
        preferences: NotificationPreferences,
        updatedAt: Long,
        ownerUserId: String?,
    ): NotificationPreferencesEntity = NotificationPreferencesEntity(
        id = SINGLE_ROW_ID,
        newExpense = NotificationType.BILLS in preferences.push,
        paymentMarked = NotificationType.PAYMENTS in preferences.push,
        settleReminders = NotificationType.BALANCE_REMINDER in preferences.push,
        invites = NotificationType.PEOPLE_AND_GROUPS in preferences.push,
        comments = NotificationType.COMMENTS in preferences.push,
        paymentRequests = NotificationType.PAYMENT_REQUESTS in preferences.push,
        inAppBills = NotificationType.BILLS in preferences.inApp,
        inAppPayments = NotificationType.PAYMENTS in preferences.inApp,
        inAppComments = NotificationType.COMMENTS in preferences.inApp,
        inAppPeopleAndGroups = NotificationType.PEOPLE_AND_GROUPS in preferences.inApp,
        inAppPaymentRequests = NotificationType.PAYMENT_REQUESTS in preferences.inApp,
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
        comments = entity.comments,
        paymentRequests = entity.paymentRequests,
        inAppBills = entity.inAppBills,
        inAppPayments = entity.inAppPayments,
        inAppComments = entity.inAppComments,
        inAppPeopleAndGroups = entity.inAppPeopleAndGroups,
        inAppPaymentRequests = entity.inAppPaymentRequests,
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
        comments = dto.comments,
        paymentRequests = dto.paymentRequests,
        inAppBills = dto.inAppBills,
        inAppPayments = dto.inAppPayments,
        inAppComments = dto.inAppComments,
        inAppPeopleAndGroups = dto.inAppPeopleAndGroups,
        inAppPaymentRequests = dto.inAppPaymentRequests,
        updatedAt = dto.updatedAt,
        pendingSync = false,
        ownerUserId = ownerUserId,
    )
}
