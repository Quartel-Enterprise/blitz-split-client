package com.quare.blitzsplit.core.account.di

import com.quare.blitzsplit.core.account.data.datasource.AccountLocalDataSource
import com.quare.blitzsplit.core.account.data.datasource.AccountRemoteDataSource
import com.quare.blitzsplit.core.account.data.datasource.RoomAccountLocalDataSource
import com.quare.blitzsplit.core.account.data.datasource.SupabaseAccountRemoteDataSource
import com.quare.blitzsplit.core.account.data.repository.AccountRepositoryImpl
import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.account.domain.usecase.ConnectWithBrowser
import com.quare.blitzsplit.core.account.domain.usecase.ConnectWithIdToken
import com.quare.blitzsplit.core.account.domain.usecase.GetSessionUser
import com.quare.blitzsplit.core.account.domain.usecase.HandleAppPaused
import com.quare.blitzsplit.core.account.domain.usecase.HandleAppResumed
import com.quare.blitzsplit.core.account.domain.usecase.HandleBrowserCallback
import com.quare.blitzsplit.core.account.domain.usecase.IssueRecoveryCode
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAbandonedBrowserSignIn
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.account.domain.usecase.ObserveBrowserSignInReturningToProfile
import com.quare.blitzsplit.core.account.domain.usecase.ObserveReturningBrowserSignIn
import com.quare.blitzsplit.core.account.domain.usecase.RestoreWithRecoveryCode
import com.quare.blitzsplit.core.account.domain.usecase.UpdateRecoveryCodePrompt
import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithBrowserUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ConnectWithIdTokenUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.GetSessionUserUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.HandleAppPausedUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.HandleAppResumedUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.HandleBrowserCallbackUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.IssueRecoveryCodeUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAbandonedBrowserSignInUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountStatusUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveAccountUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveBrowserSignInReturningToProfileUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.ObserveReturningBrowserSignInUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.RestoreWithRecoveryCodeUseCase
import com.quare.blitzsplit.core.account.domain.usecase.impl.UpdateRecoveryCodePromptUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val accountModule = module {
    singleOf(::RoomAccountLocalDataSource).bind<AccountLocalDataSource>()
    singleOf(::SupabaseAccountRemoteDataSource).bind<AccountRemoteDataSource>()
    singleOf(::AccountRepositoryImpl).bind<AccountRepository>()
    factoryOf(::ObserveAccountStatusUseCase).bind<ObserveAccountStatus>()
    factoryOf(::ObserveAccountUseCase).bind<ObserveAccount>()
    factoryOf(::UpdateRecoveryCodePromptUseCase).bind<UpdateRecoveryCodePrompt>()
    factoryOf(::GetSessionUserUseCase).bind<GetSessionUser>()
    factoryOf(::ConnectWithIdTokenUseCase).bind<ConnectWithIdToken>()
    factoryOf(::ConnectWithBrowserUseCase).bind<ConnectWithBrowser>()
    factoryOf(::HandleBrowserCallbackUseCase).bind<HandleBrowserCallback>()
    factoryOf(::HandleAppPausedUseCase).bind<HandleAppPaused>()
    factoryOf(::HandleAppResumedUseCase).bind<HandleAppResumed>()
    factoryOf(::ObserveAbandonedBrowserSignInUseCase).bind<ObserveAbandonedBrowserSignIn>()
    factoryOf(::ObserveReturningBrowserSignInUseCase).bind<ObserveReturningBrowserSignIn>()
    factoryOf(::ObserveBrowserSignInReturningToProfileUseCase).bind<ObserveBrowserSignInReturningToProfile>()
    factoryOf(::RestoreWithRecoveryCodeUseCase).bind<RestoreWithRecoveryCode>()
    factoryOf(::IssueRecoveryCodeUseCase).bind<IssueRecoveryCode>()
}
