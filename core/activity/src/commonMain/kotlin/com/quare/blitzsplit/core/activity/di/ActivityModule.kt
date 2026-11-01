package com.quare.blitzsplit.core.activity.di

import com.quare.blitzsplit.core.activity.data.datasource.ActivityLocalDataSource
import com.quare.blitzsplit.core.activity.data.datasource.ActivityRemoteDataSource
import com.quare.blitzsplit.core.activity.data.datasource.RoomActivityLocalDataSource
import com.quare.blitzsplit.core.activity.data.datasource.SupabaseActivityRemoteDataSource
import com.quare.blitzsplit.core.activity.data.mapper.ActivityMapper
import com.quare.blitzsplit.core.activity.data.mapper.ActivityMapperImpl
import com.quare.blitzsplit.core.activity.data.repository.ActivityRepositoryImpl
import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.activity.domain.usecase.LoadOlderActivity
import com.quare.blitzsplit.core.activity.domain.usecase.MarkActivityRead
import com.quare.blitzsplit.core.activity.domain.usecase.MarkAllActivityRead
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityFeed
import com.quare.blitzsplit.core.activity.domain.usecase.ObserveActivityUnreadCount
import com.quare.blitzsplit.core.activity.domain.usecase.RecordCharges
import com.quare.blitzsplit.core.activity.domain.usecase.impl.LoadOlderActivityUseCase
import com.quare.blitzsplit.core.activity.domain.usecase.impl.MarkActivityReadUseCase
import com.quare.blitzsplit.core.activity.domain.usecase.impl.MarkAllActivityReadUseCase
import com.quare.blitzsplit.core.activity.domain.usecase.impl.ObserveActivityFeedUseCase
import com.quare.blitzsplit.core.activity.domain.usecase.impl.ObserveActivityUnreadCountUseCase
import com.quare.blitzsplit.core.activity.domain.usecase.impl.RecordChargesUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val activityModule = module {
    singleOf(::RoomActivityLocalDataSource).bind<ActivityLocalDataSource>()
    singleOf(::SupabaseActivityRemoteDataSource).bind<ActivityRemoteDataSource>()
    singleOf(::ActivityMapperImpl).bind<ActivityMapper>()
    // Why: one for the whole app, since the older pages it loaded live in memory.
    singleOf(::ActivityRepositoryImpl).bind<ActivityRepository>()
    factoryOf(::ObserveActivityFeedUseCase).bind<ObserveActivityFeed>()
    factoryOf(::ObserveActivityUnreadCountUseCase).bind<ObserveActivityUnreadCount>()
    factoryOf(::MarkActivityReadUseCase).bind<MarkActivityRead>()
    factoryOf(::MarkAllActivityReadUseCase).bind<MarkAllActivityRead>()
    factoryOf(::RecordChargesUseCase).bind<RecordCharges>()
    factoryOf(::LoadOlderActivityUseCase).bind<LoadOlderActivity>()
}
