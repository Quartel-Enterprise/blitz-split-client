package com.quare.blitzsplit.core.support.di

import com.quare.blitzsplit.core.support.data.ReviewMomentInbox
import com.quare.blitzsplit.core.support.data.datasource.ReviewPromptLocalDataSource
import com.quare.blitzsplit.core.support.data.datasource.RoomReviewPromptLocalDataSource
import com.quare.blitzsplit.core.support.data.repository.ReviewPromptRepositoryImpl
import com.quare.blitzsplit.core.support.domain.repository.ReviewPromptRepository
import com.quare.blitzsplit.core.support.domain.usecase.BuildSupportEmail
import com.quare.blitzsplit.core.support.domain.usecase.FindRateAppLink
import com.quare.blitzsplit.core.support.domain.usecase.NoteReviewMoment
import com.quare.blitzsplit.core.support.domain.usecase.ObserveReviewMoments
import com.quare.blitzsplit.core.support.domain.usecase.ObserveSupportDiagnostics
import com.quare.blitzsplit.core.support.domain.usecase.RequestReviewIfNeeded
import com.quare.blitzsplit.core.support.domain.usecase.impl.BuildSupportEmailUseCase
import com.quare.blitzsplit.core.support.domain.usecase.impl.FindRateAppLinkUseCase
import com.quare.blitzsplit.core.support.domain.usecase.impl.NoteReviewMomentUseCase
import com.quare.blitzsplit.core.support.domain.usecase.impl.ObserveReviewMomentsUseCase
import com.quare.blitzsplit.core.support.domain.usecase.impl.ObserveSupportDiagnosticsUseCase
import com.quare.blitzsplit.core.support.domain.usecase.impl.RequestReviewIfNeededUseCase
import com.quare.blitzsplit.core.support.generated.SupportConfig
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
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
    factoryOf(::BuildSupportEmailUseCase).bind<BuildSupportEmail>()
    singleOf(::RoomReviewPromptLocalDataSource).bind<ReviewPromptLocalDataSource>()
    singleOf(::ReviewPromptRepositoryImpl).bind<ReviewPromptRepository>()
    singleOf(::ReviewMomentInbox)
    factoryOf(::NoteReviewMomentUseCase).bind<NoteReviewMoment>()
    factoryOf(::ObserveReviewMomentsUseCase).bind<ObserveReviewMoments>()
    factoryOf(::FindRateAppLinkUseCase).bind<FindRateAppLink>()
    factory<RequestReviewIfNeeded> {
        RequestReviewIfNeededUseCase(
            appVersion = SupportConfig.APP_VERSION,
            platform = get(),
            navigator = get(),
            repository = get(),
            observeGroupBalances = get(),
            currentTimeMillis = get(),
            trackEvent = get(),
        )
    }
}
