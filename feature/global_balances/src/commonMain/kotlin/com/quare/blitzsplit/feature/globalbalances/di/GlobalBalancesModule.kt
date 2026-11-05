package com.quare.blitzsplit.feature.globalbalances.di

import com.quare.blitzsplit.feature.globalbalances.presentation.viewmodel.ChargeViewModel
import com.quare.blitzsplit.feature.globalbalances.presentation.viewmodel.GlobalBalancesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Why: the route comes in as a parameter (parametersOf(route)): the filter Saldos opens on, the person Cobrar is for.
val globalBalancesModule = module {
    viewModelOf(::GlobalBalancesViewModel)
    viewModelOf(::ChargeViewModel)
}
