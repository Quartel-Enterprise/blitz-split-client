package com.quare.blitzsplit.core.watch.di

import com.quare.blitzsplit.core.watch.data.datasource.SupabaseWatchPairingRemoteDataSource
import com.quare.blitzsplit.core.watch.data.datasource.WatchPairingRemoteDataSource
import com.quare.blitzsplit.core.watch.data.repository.WatchPairingRepositoryImpl
import com.quare.blitzsplit.core.watch.domain.model.WatchSignOutNotice
import com.quare.blitzsplit.core.watch.domain.repository.WatchPairingRepository
import com.quare.blitzsplit.core.watch.domain.usecase.ApproveNearbyWatch
import com.quare.blitzsplit.core.watch.domain.usecase.ApproveWatchPairing
import com.quare.blitzsplit.core.watch.domain.usecase.AwaitWatchPairing
import com.quare.blitzsplit.core.watch.domain.usecase.DismissSignedOutOnPhone
import com.quare.blitzsplit.core.watch.domain.usecase.EndWatchSessionFromPhone
import com.quare.blitzsplit.core.watch.domain.usecase.ObserveAccountSignOuts
import com.quare.blitzsplit.core.watch.domain.usecase.ObserveSignedOutOnPhone
import com.quare.blitzsplit.core.watch.domain.usecase.RequestWatchPairing
import com.quare.blitzsplit.core.watch.domain.usecase.SignInWithWatchPairing
import com.quare.blitzsplit.core.watch.domain.usecase.impl.ApproveNearbyWatchUseCase
import com.quare.blitzsplit.core.watch.domain.usecase.impl.ApproveWatchPairingUseCase
import com.quare.blitzsplit.core.watch.domain.usecase.impl.AwaitWatchPairingUseCase
import com.quare.blitzsplit.core.watch.domain.usecase.impl.DismissSignedOutOnPhoneUseCase
import com.quare.blitzsplit.core.watch.domain.usecase.impl.EndWatchSessionFromPhoneUseCase
import com.quare.blitzsplit.core.watch.domain.usecase.impl.ObserveAccountSignOutsUseCase
import com.quare.blitzsplit.core.watch.domain.usecase.impl.ObserveSignedOutOnPhoneUseCase
import com.quare.blitzsplit.core.watch.domain.usecase.impl.RequestWatchPairingUseCase
import com.quare.blitzsplit.core.watch.domain.usecase.impl.SignInWithWatchPairingUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val watchModule = module {
    singleOf(::SupabaseWatchPairingRemoteDataSource).bind<WatchPairingRemoteDataSource>()
    singleOf(::WatchPairingRepositoryImpl).bind<WatchPairingRepository>()
    single { WatchSignOutNotice() }
    factoryOf(::RequestWatchPairingUseCase).bind<RequestWatchPairing>()
    factoryOf(::AwaitWatchPairingUseCase).bind<AwaitWatchPairing>()
    factoryOf(::SignInWithWatchPairingUseCase).bind<SignInWithWatchPairing>()
    factoryOf(::ApproveWatchPairingUseCase).bind<ApproveWatchPairing>()
    factoryOf(::ApproveNearbyWatchUseCase).bind<ApproveNearbyWatch>()
    factoryOf(::ObserveAccountSignOutsUseCase).bind<ObserveAccountSignOuts>()
    factoryOf(::EndWatchSessionFromPhoneUseCase).bind<EndWatchSessionFromPhone>()
    factoryOf(::ObserveSignedOutOnPhoneUseCase).bind<ObserveSignedOutOnPhone>()
    factoryOf(::DismissSignedOutOnPhoneUseCase).bind<DismissSignedOutOnPhone>()
}
