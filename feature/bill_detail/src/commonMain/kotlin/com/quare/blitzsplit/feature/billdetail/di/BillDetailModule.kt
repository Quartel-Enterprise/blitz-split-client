package com.quare.blitzsplit.feature.billdetail.di

import com.quare.blitzsplit.feature.billdetail.presentation.viewmodel.BillDetailViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Why: the route comes in as a parameter (parametersOf(route)), so each bill opens its own ViewModel.
val billDetailModule = module {
    viewModelOf(::BillDetailViewModel)
}
