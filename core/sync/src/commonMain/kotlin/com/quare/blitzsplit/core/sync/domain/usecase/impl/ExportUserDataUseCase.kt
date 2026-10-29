package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.profile.domain.repository.NotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.sync.data.dto.UserDataExportDto
import com.quare.blitzsplit.core.sync.data.mapper.UserDataExportMapper
import com.quare.blitzsplit.core.sync.domain.usecase.ExportUserData
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class ExportUserDataUseCase(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val paymentKeyRepository: PaymentKeyRepository,
    private val notificationPreferencesRepository: NotificationPreferencesRepository,
    private val mapper: UserDataExportMapper,
    private val currentTimeMillis: CurrentTimeMillis,
) : ExportUserData {
    private val json = Json { prettyPrint = true }

    override suspend fun invoke(): Result<String> = suspendRunCatching {
        val account = accountRepository.observeAccount().first()
        val export = UserDataExportDto(
            format = FORMAT,
            version = VERSION,
            exportedAt = mapper.toIsoInstant(currentTimeMillis()),
            account = mapper.toDto(account.status, account.email),
            profile = profileRepository.observeProfile().first()?.let(mapper::toDto),
            paymentKeys = paymentKeyRepository.observeKeys().first().map(mapper::toDto),
            notifications = mapper.toDto(notificationPreferencesRepository.observePreferences().first()),
        )
        json.encodeToString(UserDataExportDto.serializer(), export)
    }

    private companion object {
        const val FORMAT = "blitz-split-user-data"
        // Why: 2 added the notification preferences.
        const val VERSION = 2
    }
}
