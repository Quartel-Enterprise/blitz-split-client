package com.quare.blitzsplit.feature.splitbill.di

import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.BillCategoryUseCases
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Why: the route comes in as a parameter (parametersOf(route)), so each group opens its own ViewModel.
val splitBillModule = module {
    factoryOf(::BillCategoryUseCases)
    viewModelOf(::SplitBillViewModel)
}
