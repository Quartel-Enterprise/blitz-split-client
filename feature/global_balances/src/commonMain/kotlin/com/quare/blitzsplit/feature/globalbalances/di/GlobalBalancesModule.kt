package com.quare.blitzsplit.feature.globalbalances.di

import com.quare.blitzsplit.feature.globalbalances.presentation.viewmodel.GlobalBalancesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Why: the route comes in as a parameter (parametersOf(route)), with the filter Saldos opens on.
val globalBalancesModule = module {
    viewModelOf(::GlobalBalancesViewModel)
}
