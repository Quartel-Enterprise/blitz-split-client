package com.quare.blitzsplit.core.navigation.di

import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.domain.usecase.StartScreenViews
import com.quare.blitzsplit.core.navigation.domain.usecase.impl.StartScreenViewsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val navigationModule = module {
    single<Navigator> { BackStackNavigator(startRoute = Route.Welcome) }
    // Why: a single, which starts once and follows the back stack for the app's whole life.
    single<StartScreenViews> {
        StartScreenViewsUseCase(
            navigator = get(),
            trackEvent = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }
}
