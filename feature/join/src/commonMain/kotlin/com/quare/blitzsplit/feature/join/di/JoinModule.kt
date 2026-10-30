package com.quare.blitzsplit.feature.join.di

import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinInviteViewModel
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinPickViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Why: the route comes in as a parameter (parametersOf(route)), so each link opens its own ViewModels.
val joinModule = module {
    viewModelOf(::JoinInviteViewModel)
    viewModelOf(::JoinPickViewModel)
}
