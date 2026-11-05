package com.quare.blitzsplit.core.sync.data.mapper

import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.sync.data.dto.AccountExportDto
import com.quare.blitzsplit.core.sync.data.dto.InAppNotificationsExportDto
import com.quare.blitzsplit.core.sync.data.dto.NotificationPreferencesExportDto
import com.quare.blitzsplit.core.sync.data.dto.PaymentKeyExportDto
import com.quare.blitzsplit.core.sync.data.dto.ProfileExportDto
import kotlin.time.Instant

class UserDataExportMapperImpl : UserDataExportMapper {
    override fun toDto(
        status: AccountStatus,
        email: String?,
    ): AccountExportDto = when (status) {
        AccountStatus.LocalOnly -> AccountExportDto(
            type = "none",
            userId = null,
            email = null,
        )

        is AccountStatus.Anonymous -> AccountExportDto(
            type = "anonymous",
            userId = status.userId,
            email = null,
        )

        is AccountStatus.Connected -> AccountExportDto(
            type = status.provider.code,
            userId = status.userId,
            email = email,
        )
    }

    override fun toDto(profile: UserProfile): ProfileExportDto = ProfileExportDto(
        displayName = profile.displayName.trim(),
        avatarColor = profile.avatarColor.code,
        photoUrl = (profile.photo as? ProfilePhoto.Remote)?.url,
    )

    override fun toDto(key: PaymentKey): PaymentKeyExportDto = PaymentKeyExportDto(
        method = key.type.method.name
            .lowercase(),
        type = key.type.name.lowercase(),
        value = key.value,
        isDefault = key.isDefault,
        addedAt = toIsoInstant(key.addedAt),
    )

    override fun toDto(preferences: NotificationPreferences): NotificationPreferencesExportDto =
        NotificationPreferencesExportDto(
            newExpense = NotificationType.BILLS in preferences.push,
            paymentMarked = NotificationType.PAYMENTS in preferences.push,
            settleReminders = NotificationType.BALANCE_REMINDER in preferences.push,
            invites = NotificationType.PEOPLE_AND_GROUPS in preferences.push,
            comments = NotificationType.COMMENTS in preferences.push,
            paymentRequests = NotificationType.PAYMENT_REQUESTS in preferences.push,
            inApp = InAppNotificationsExportDto(
                bills = NotificationType.BILLS in preferences.inApp,
                payments = NotificationType.PAYMENTS in preferences.inApp,
                comments = NotificationType.COMMENTS in preferences.inApp,
                peopleAndGroups = NotificationType.PEOPLE_AND_GROUPS in preferences.inApp,
                paymentRequests = NotificationType.PAYMENT_REQUESTS in preferences.inApp,
            ),
        )

    override fun toIsoInstant(epochMillis: Long): String = Instant.fromEpochMilliseconds(epochMillis).toString()
}
