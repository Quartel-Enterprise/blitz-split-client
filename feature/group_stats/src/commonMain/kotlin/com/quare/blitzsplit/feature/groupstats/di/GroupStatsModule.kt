package com.quare.blitzsplit.feature.groupstats.di

import com.quare.blitzsplit.feature.groupstats.presentation.viewmodel.GroupStatsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Why: the route comes in as a parameter (parametersOf(route)), so each group opens its own ViewModel.
val groupStatsModule = module {
    viewModelOf(::GroupStatsViewModel)
}
