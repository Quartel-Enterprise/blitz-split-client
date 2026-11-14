package com.quare.blitzsplit.core.sync.di

import com.quare.blitzsplit.core.sync.AppForeground
import com.quare.blitzsplit.core.sync.FirstDownload
import com.quare.blitzsplit.core.sync.PersonalCategoryData
import com.quare.blitzsplit.core.sync.SyncCoordinator
import com.quare.blitzsplit.core.sync.connect.ProviderConnector
import com.quare.blitzsplit.core.sync.connect.ProviderConnectorImpl
import com.quare.blitzsplit.core.sync.data.datasource.RemoteChangeRemoteDataSource
import com.quare.blitzsplit.core.sync.data.datasource.SupabaseRemoteChangeRemoteDataSource
import com.quare.blitzsplit.core.sync.data.mapper.UserDataExportMapper
import com.quare.blitzsplit.core.sync.data.mapper.UserDataExportMapperImpl
import com.quare.blitzsplit.core.sync.data.repository.RemoteChangeRepositoryImpl
import com.quare.blitzsplit.core.sync.domain.repository.RemoteChangeRepository
import com.quare.blitzsplit.core.sync.domain.usecase.CancelSignOutWhenSynced
import com.quare.blitzsplit.core.sync.domain.usecase.ChangeInviteLink
import com.quare.blitzsplit.core.sync.domain.usecase.ClaimGuestAsMe
import com.quare.blitzsplit.core.sync.domain.usecase.CompleteSignIn
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteAccount
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteGroup
import com.quare.blitzsplit.core.sync.domain.usecase.ExportUserData
import com.quare.blitzsplit.core.sync.domain.usecase.JoinGroup
import com.quare.blitzsplit.core.sync.domain.usecase.LeaveGroup
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveAppForeground
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveShowsPix
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.ReportAppForeground
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.SignOut
import com.quare.blitzsplit.core.sync.domain.usecase.SignOutWhenSynced
import com.quare.blitzsplit.core.sync.domain.usecase.StartSync
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.sync.domain.usecase.impl.CancelSignOutWhenSyncedUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.ChangeInviteLinkUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.ClaimGuestAsMeUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.CompleteSignInUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.DeleteAccountUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.DeleteGroupUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.ExportUserDataUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.JoinGroupUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.LeaveGroupUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.ObserveAppForegroundUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.ObserveFirstDownloadUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.ObserveShowsPixUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.ObserveSyncStateUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.ReportAppForegroundUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.RetrySyncUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.SignOutUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.SignOutWhenSyncedUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.StartSyncUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.SyncNowUseCase
import com.quare.blitzsplit.core.sync.signin.ProviderSignIn
import com.quare.blitzsplit.core.sync.signin.ProviderSignInImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val syncModule = module {
    single { FirstDownload() }
    single { AppForeground() }
    singleOf(::SupabaseRemoteChangeRemoteDataSource).bind<RemoteChangeRemoteDataSource>()
    singleOf(::RemoteChangeRepositoryImpl).bind<RemoteChangeRepository>()
    single {
        SyncCoordinator(
            accountRepository = get(),
            profileRepository = get(),
            paymentKeyRepository = get(),
            notificationPreferencesRepository = get(),
            appInviteRepository = get(),
            groupRepository = get(),
            groupPinRepository = get(),
            groupIncomeRepository = get(),
            expenseRepository = get(),
            settlementRepository = get(),
            categoryRepository = get(),
            personalCategoryData = PersonalCategoryData(get(), get()),
            payingMethodRepository = get(),
            billCommentRepository = get(),
            billAttachmentRepository = get(),
            activityRepository = get(),
            hiddenContactRepository = get(),
            pushDeviceRepository = get(),
            remoteChangeRepository = get(),
            firstDownload = get(),
            appForeground = get(),
            connectivityObserver = get(),
            crashReporter = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }
    // Why: factories, so each screen keeps its own link or sign in under way.
    factoryOf(::ProviderConnectorImpl).bind<ProviderConnector>()
    factoryOf(::ProviderSignInImpl).bind<ProviderSignIn>()
    factoryOf(::SyncNowUseCase).bind<SyncNow>()
    factoryOf(::StartSyncUseCase).bind<StartSync>()
    factoryOf(::CompleteSignInUseCase).bind<CompleteSignIn>()
    factoryOf(::ObserveSyncStateUseCase).bind<ObserveSyncState>()
    factoryOf(::ObserveFirstDownloadUseCase).bind<ObserveFirstDownload>()
    factoryOf(::ObserveShowsPixUseCase).bind<ObserveShowsPix>()
    factoryOf(::RetrySyncUseCase).bind<RetrySync>()
    factoryOf(::ReportAppForegroundUseCase).bind<ReportAppForeground>()
    factoryOf(::ObserveAppForegroundUseCase).bind<ObserveAppForeground>()
    factoryOf(::SignOutUseCase).bind<SignOut>()
    factoryOf(::SignOutWhenSyncedUseCase).bind<SignOutWhenSynced>()
    factoryOf(::CancelSignOutWhenSyncedUseCase).bind<CancelSignOutWhenSynced>()
    factoryOf(::DeleteAccountUseCase).bind<DeleteAccount>()
    factoryOf(::UserDataExportMapperImpl).bind<UserDataExportMapper>()
    factoryOf(::ExportUserDataUseCase).bind<ExportUserData>()
    factoryOf(::JoinGroupUseCase).bind<JoinGroup>()
    factoryOf(::ChangeInviteLinkUseCase).bind<ChangeInviteLink>()
    factoryOf(::LeaveGroupUseCase).bind<LeaveGroup>()
    factoryOf(::DeleteGroupUseCase).bind<DeleteGroup>()
    factoryOf(::ClaimGuestAsMeUseCase).bind<ClaimGuestAsMe>()
}
