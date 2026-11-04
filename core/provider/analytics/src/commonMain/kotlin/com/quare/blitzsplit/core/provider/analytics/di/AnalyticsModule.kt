package com.quare.blitzsplit.core.provider.analytics.di

import com.quare.blitzsplit.core.provider.analytics.AnalyticsHub
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.StartScreenViews
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.impl.StartScreenViewsUseCase
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.impl.TrackEventUseCase
import com.quare.blitzsplit.core.provider.analytics.screen.ScreenViewTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val analyticsModule = module {
    singleOf(::AnalyticsHub)
    factoryOf(::TrackEventUseCase).bind<TrackEvent>()
    // Why: a single that owns its scope, following the back stack for the app's whole life, as SyncCoordinator does.
    single {
        ScreenViewTracker(
            navigator = get(),
            trackEvent = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }
    factoryOf(::StartScreenViewsUseCase).bind<StartScreenViews>()
}
