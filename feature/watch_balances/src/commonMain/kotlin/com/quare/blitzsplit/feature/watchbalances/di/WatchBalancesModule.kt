package com.quare.blitzsplit.feature.watchbalances.di

import com.quare.blitzsplit.feature.watchbalances.presentation.viewmodel.WatchBalancesViewModel
import com.quare.blitzsplit.feature.watchbalances.presentation.viewmodel.WatchGroupViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Why: the group's route comes in as a parameter (parametersOf(route)).
val watchBalancesModule = module {
    viewModelOf(::WatchBalancesViewModel)
    viewModelOf(::WatchGroupViewModel)
}
