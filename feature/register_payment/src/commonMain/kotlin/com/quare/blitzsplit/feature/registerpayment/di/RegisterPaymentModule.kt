package com.quare.blitzsplit.feature.registerpayment.di

import com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel.RegisterPaymentViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Why: the route comes in as a parameter (parametersOf(route)), so each pair opens its own ViewModel.
val registerPaymentModule = module {
    viewModelOf(::RegisterPaymentViewModel)
}
