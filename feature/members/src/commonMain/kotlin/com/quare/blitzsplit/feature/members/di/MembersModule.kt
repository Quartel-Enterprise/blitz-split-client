package com.quare.blitzsplit.feature.members.di

import com.quare.blitzsplit.feature.members.presentation.viewmodel.MembersViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Why: the group screen's route comes in as a parameter (parametersOf(route)), so each group has its own ViewModel.
val membersModule = module {
    viewModelOf(::MembersViewModel)
}
