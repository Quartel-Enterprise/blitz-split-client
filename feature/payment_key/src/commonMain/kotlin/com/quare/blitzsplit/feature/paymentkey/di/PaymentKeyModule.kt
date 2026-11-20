package com.quare.blitzsplit.feature.paymentkey.di

import com.quare.blitzsplit.feature.paymentkey.presentation.viewmodel.PaymentKeyViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

// Why: the route comes in as a parameter (parametersOf(route)), so each key opens its own ViewModel.
val paymentKeyModule = module {
    factoryOf(::PaymentKeyViewModel)
}
