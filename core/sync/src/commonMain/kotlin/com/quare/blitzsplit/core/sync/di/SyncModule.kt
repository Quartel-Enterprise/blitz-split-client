package com.quare.blitzsplit.core.sync.di

import com.quare.blitzsplit.core.sync.SyncCoordinator
import com.quare.blitzsplit.core.sync.domain.usecase.CompleteSignIn
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveSyncState
import com.quare.blitzsplit.core.sync.domain.usecase.RetrySync
import com.quare.blitzsplit.core.sync.domain.usecase.StartSync
import com.quare.blitzsplit.core.sync.domain.usecase.SyncNow
import com.quare.blitzsplit.core.sync.domain.usecase.impl.CompleteSignInUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.ObserveSyncStateUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.RetrySyncUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.StartSyncUseCase
import com.quare.blitzsplit.core.sync.domain.usecase.impl.SyncNowUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val syncModule = module {
    single {
        SyncCoordinator(
            accountRepository = get(),
            profileRepository = get(),
            connectivityObserver = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }
    factoryOf(::SyncNowUseCase).bind<SyncNow>()
    factoryOf(::StartSyncUseCase).bind<StartSync>()
    factoryOf(::CompleteSignInUseCase).bind<CompleteSignIn>()
    factoryOf(::ObserveSyncStateUseCase).bind<ObserveSyncState>()
    factoryOf(::RetrySyncUseCase).bind<RetrySync>()
}
