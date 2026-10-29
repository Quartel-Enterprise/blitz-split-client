package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.sync.domain.usecase.ExportUserData
import com.quare.blitzsplit.core.sync.export.AccountExportDto
import com.quare.blitzsplit.core.sync.export.PaymentKeyExportDto
import com.quare.blitzsplit.core.sync.export.ProfileExportDto
import com.quare.blitzsplit.core.sync.export.UserDataExportDto
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlin.time.Instant

class ExportUserDataUseCase(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val paymentKeyRepository: PaymentKeyRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : ExportUserData {
    private val json = Json { prettyPrint = true }

    override suspend fun invoke(): String {
        val account = accountRepository.observeAccount().first()
        val export = UserDataExportDto(
            format = FORMAT,
            version = VERSION,
            exportedAt = toIsoInstant(currentTimeMillis()),
            account = toAccount(account.status, account.email),
            profile = profileRepository.observeProfile().first()?.let(::toProfile),
            paymentKeys = paymentKeyRepository.observeKeys().first().map(::toPaymentKey),
        )
        return json.encodeToString(UserDataExportDto.serializer(), export)
    }

    private fun toAccount(
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

    private fun toProfile(profile: UserProfile): ProfileExportDto = ProfileExportDto(
        displayName = profile.displayName.trim(),
        avatarColor = profile.avatarColor.code,
        photoUrl = (profile.photo as? ProfilePhoto.Remote)?.url,
    )

    private fun toPaymentKey(key: PaymentKey): PaymentKeyExportDto = PaymentKeyExportDto(
        method = key.type.method.name
            .lowercase(),
        type = key.type.name.lowercase(),
        value = key.value,
        isDefault = key.isDefault,
        addedAt = toIsoInstant(key.addedAt),
    )

    private fun toIsoInstant(epochMillis: Long): String = Instant.fromEpochMilliseconds(epochMillis).toString()

    private companion object {
        const val FORMAT = "blitz-split-user-data"
        const val VERSION = 1
    }
}
