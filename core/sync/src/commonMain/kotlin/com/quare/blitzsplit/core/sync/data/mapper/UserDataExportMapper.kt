package com.quare.blitzsplit.core.sync.data.mapper

import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.sync.data.dto.AccountExportDto
import com.quare.blitzsplit.core.sync.data.dto.NotificationPreferencesExportDto
import com.quare.blitzsplit.core.sync.data.dto.PaymentKeyExportDto
import com.quare.blitzsplit.core.sync.data.dto.ProfileExportDto

interface UserDataExportMapper {
    fun toDto(
        status: AccountStatus,
        email: String?,
    ): AccountExportDto

    fun toDto(profile: UserProfile): ProfileExportDto

    fun toDto(key: PaymentKey): PaymentKeyExportDto

    fun toDto(preferences: NotificationPreferences): NotificationPreferencesExportDto

    fun toIsoInstant(epochMillis: Long): String
}
