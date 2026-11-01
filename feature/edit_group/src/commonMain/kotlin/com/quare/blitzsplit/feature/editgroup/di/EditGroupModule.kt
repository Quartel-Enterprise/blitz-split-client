package com.quare.blitzsplit.feature.editgroup.di

import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.DefaultSplitViewModel
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.EditGroupViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Why: the route comes in as a parameter (parametersOf(route)), so each group opens its own ViewModel.
val editGroupModule = module {
    viewModelOf(::EditGroupViewModel)
    viewModelOf(::DefaultSplitViewModel)
}
