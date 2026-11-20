package com.quare.blitzsplit.feature.watchbalances.di

import com.quare.blitzsplit.feature.watchbalances.presentation.navigation.OpenWatchGroup
import com.quare.blitzsplit.feature.watchbalances.presentation.viewmodel.WatchBalancesViewModel
import com.quare.blitzsplit.feature.watchbalances.presentation.viewmodel.WatchGroupViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

// Why: the group's route comes in as a parameter (parametersOf(route)).
val watchBalancesModule = module {
    factoryOf(::WatchBalancesViewModel)
    factoryOf(::WatchGroupViewModel)
    factoryOf(::OpenWatchGroup)
}
