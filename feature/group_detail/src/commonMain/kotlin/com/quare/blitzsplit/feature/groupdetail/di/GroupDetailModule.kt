package com.quare.blitzsplit.feature.groupdetail.di

import com.quare.blitzsplit.feature.groupdetail.presentation.viewmodel.GroupDetailViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Why: the route comes in as a parameter (parametersOf(route)), so each group opens its own ViewModel.
val groupDetailModule = module {
    viewModelOf(::GroupDetailViewModel)
}
