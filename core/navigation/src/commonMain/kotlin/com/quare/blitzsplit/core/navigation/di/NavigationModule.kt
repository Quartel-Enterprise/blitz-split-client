package com.quare.blitzsplit.core.navigation.di

import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import org.koin.dsl.module

val navigationModule = module {
    single<Navigator> { BackStackNavigator(startRoute = Route.Welcome) }
}
