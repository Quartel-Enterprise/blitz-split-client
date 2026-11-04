package com.quare.blitzsplit.core.inappupdate.di

import com.quare.blitzsplit.core.inappupdate.domain.usecase.CheckForUpdate
import com.quare.blitzsplit.core.inappupdate.domain.usecase.CompleteUpdateInstall
import com.quare.blitzsplit.core.inappupdate.domain.usecase.InstallDownloadedUpdate
import com.quare.blitzsplit.core.inappupdate.domain.usecase.ObserveUpdateDownloadState
import com.quare.blitzsplit.core.inappupdate.domain.usecase.RequestUpdatePromptIfNeeded
import com.quare.blitzsplit.core.inappupdate.domain.usecase.ShowUpdatePrompt
import com.quare.blitzsplit.core.inappupdate.domain.usecase.StartUpdate
import com.quare.blitzsplit.core.inappupdate.domain.usecase.impl.CheckForUpdateUseCase
import com.quare.blitzsplit.core.inappupdate.domain.usecase.impl.CompleteUpdateInstallUseCase
import com.quare.blitzsplit.core.inappupdate.domain.usecase.impl.InstallDownloadedUpdateUseCase
import com.quare.blitzsplit.core.inappupdate.domain.usecase.impl.ObserveUpdateDownloadStateUseCase
import com.quare.blitzsplit.core.inappupdate.domain.usecase.impl.RequestUpdatePromptIfNeededUseCase
import com.quare.blitzsplit.core.inappupdate.domain.usecase.impl.ShowUpdatePromptUseCase
import com.quare.blitzsplit.core.inappupdate.domain.usecase.impl.StartUpdateUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val inAppUpdateModule = module {
    includes(platformInAppUpdateModule)
    factoryOf(::CheckForUpdateUseCase).bind<CheckForUpdate>()
    factoryOf(::StartUpdateUseCase).bind<StartUpdate>()
    factoryOf(::ObserveUpdateDownloadStateUseCase).bind<ObserveUpdateDownloadState>()
    factoryOf(::CompleteUpdateInstallUseCase).bind<CompleteUpdateInstall>()
    // Why: a single, so a retried install in the same process isn't tracked as a new one.
    singleOf(::InstallDownloadedUpdateUseCase).bind<InstallDownloadedUpdate>()
    factoryOf(::ShowUpdatePromptUseCase).bind<ShowUpdatePrompt>()
    factoryOf(::RequestUpdatePromptIfNeededUseCase).bind<RequestUpdatePromptIfNeeded>()
}

// Why: binds this platform's UpdateMechanism and AppUpdateRepository.
internal expect val platformInAppUpdateModule: Module
