package com.quare.blitzsplit.feature.invite.di

import com.quare.blitzsplit.feature.invite.presentation.viewmodel.InviteViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Why: the route comes in as a parameter (parametersOf(route)), so each group opens its own ViewModel.
val inviteModule = module {
    viewModelOf(::InviteViewModel)
}
