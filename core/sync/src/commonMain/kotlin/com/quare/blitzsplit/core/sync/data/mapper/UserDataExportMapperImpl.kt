package com.quare.blitzsplit.core.sync.data.mapper

import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.sync.data.dto.AccountExportDto
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
            newExpense = NotificationType.NEW_EXPENSE in preferences.enabledTypes,
            paymentMarked = NotificationType.PAYMENT_MARKED in preferences.enabledTypes,
            settleReminders = NotificationType.SETTLE_REMINDERS in preferences.enabledTypes,
            invites = NotificationType.INVITES in preferences.enabledTypes,
        )

    override fun toIsoInstant(epochMillis: Long): String = Instant.fromEpochMilliseconds(epochMillis).toString()
}
