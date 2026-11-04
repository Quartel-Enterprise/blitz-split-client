package com.quare.blitzsplit.core.provider.analytics.di

import com.quare.blitzsplit.core.provider.analytics.AnalyticsHub
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.impl.TrackEventUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val analyticsModule = module {
    singleOf(::AnalyticsHub)
    factoryOf(::TrackEventUseCase).bind<TrackEvent>()
}
