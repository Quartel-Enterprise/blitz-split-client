package com.quare.blitzsplit.core.support.di

import com.quare.blitzsplit.core.support.domain.usecase.BuildSupportEmailLink
import com.quare.blitzsplit.core.support.domain.usecase.ObserveSupportDiagnostics
import com.quare.blitzsplit.core.support.domain.usecase.impl.BuildSupportEmailLinkUseCase
import com.quare.blitzsplit.core.support.domain.usecase.impl.ObserveSupportDiagnosticsUseCase
import com.quare.blitzsplit.core.support.generated.SupportConfig
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val supportModule = module {
    factory<ObserveSupportDiagnostics> {
        ObserveSupportDiagnosticsUseCase(
            appVersion = SupportConfig.APP_VERSION,
            platform = get(),
            observeAppLanguage = get(),
            observeSpokenLanguage = get(),
            observeAccountStatus = get(),
            observeProfileShareLink = get(),
        )
    }
    factoryOf(::BuildSupportEmailLinkUseCase).bind<BuildSupportEmailLink>()
}
