package com.quare.blitzsplit.core.provider.analytics.di

import com.quare.blitzsplit.core.provider.analytics.AnalyticsHub
import com.quare.blitzsplit.core.provider.analytics.TrackEvent
import com.quare.blitzsplit.core.provider.analytics.TrackEventUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val analyticsModule = module {
    singleOf(::AnalyticsHub)
    factoryOf(::TrackEventUseCase).bind<TrackEvent>()
}
