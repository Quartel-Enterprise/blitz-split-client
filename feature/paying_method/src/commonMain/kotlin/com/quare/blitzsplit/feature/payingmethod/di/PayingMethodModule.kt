package com.quare.blitzsplit.feature.payingmethod.di

import com.quare.blitzsplit.feature.payingmethod.presentation.viewmodel.PayingMethodViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Why: the route comes in as a parameter (parametersOf(route)), so each method opens its own ViewModel.
val payingMethodModule = module {
    viewModelOf(::PayingMethodViewModel)
}
