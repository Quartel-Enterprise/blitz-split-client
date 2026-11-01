package com.quare.blitzsplit.feature.groups.di

import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import com.quare.blitzsplit.feature.groups.presentation.viewmodel.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val groupsModule = module {
    viewModelOf(::GroupsViewModel)
    viewModelOf(::HomeViewModel)
}
