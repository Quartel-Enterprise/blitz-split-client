package com.quare.blitzsplit.core.support.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.model.AccountProvider
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.profile.domain.model.ClientPlatform
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveProfileShareLink
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveAppLanguage
import com.quare.blitzsplit.core.settings.domain.usecase.ObserveSpokenLanguage
import com.quare.blitzsplit.core.support.domain.model.SupportAccount
import com.quare.blitzsplit.core.support.domain.model.SupportDiagnostics
import com.quare.blitzsplit.core.support.domain.model.SupportPlatform
import com.quare.blitzsplit.core.support.domain.usecase.ObserveSupportDiagnostics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveSupportDiagnosticsUseCase(
    private val appVersion: String,
    private val platform: ClientPlatform,
    private val observeAppLanguage: ObserveAppLanguage,
    private val observeSpokenLanguage: ObserveSpokenLanguage,
    private val observeAccountStatus: ObserveAccountStatus,
    private val observeProfileShareLink: ObserveProfileShareLink,
) : ObserveSupportDiagnostics {
    override fun invoke(): Flow<SupportDiagnostics> = combine(
        observeAppLanguage(),
        observeSpokenLanguage(),
        observeAccountStatus(),
        observeProfileShareLink(),
    ) { language, spokenLanguage, status, profileLink ->
        SupportDiagnostics(
            appVersion = appVersion,
            platform = platform.toSupportPlatform(),
            language = language,
            spokenLanguage = spokenLanguage,
            account = status.toSupportAccount(),
            profileLink = profileLink,
        )
    }

    private fun ClientPlatform.toSupportPlatform(): SupportPlatform = when (this) {
        ClientPlatform.ANDROID -> SupportPlatform.ANDROID
        ClientPlatform.IOS -> SupportPlatform.IOS
        ClientPlatform.MACOS -> SupportPlatform.MACOS
        ClientPlatform.WEB -> SupportPlatform.WEB
    }

    private fun AccountStatus.toSupportAccount(): SupportAccount = when (this) {
        AccountStatus.LocalOnly -> SupportAccount.LOCAL_ONLY

        is AccountStatus.Anonymous -> SupportAccount.ANONYMOUS

        is AccountStatus.Connected -> when (provider) {
            AccountProvider.GOOGLE -> SupportAccount.GOOGLE
            AccountProvider.APPLE -> SupportAccount.APPLE
        }
    }
}
