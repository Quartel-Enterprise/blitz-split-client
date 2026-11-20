package com.quare.blitzsplit.feature.editgroup.di

import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.DefaultSplitViewModel
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.EditGroupViewModel
import com.quare.blitzsplit.feature.editgroup.presentation.viewmodel.GroupPhotoViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

// Why: the route comes in as a parameter (parametersOf(route)), so each group opens its own ViewModel.
val editGroupModule = module {
    factoryOf(::EditGroupViewModel)
    factoryOf(::DefaultSplitViewModel)
    factoryOf(::GroupPhotoViewModel)
}
