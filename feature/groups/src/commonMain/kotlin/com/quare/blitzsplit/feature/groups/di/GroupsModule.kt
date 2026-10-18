package com.quare.blitzsplit.feature.groups.di

import com.quare.blitzsplit.feature.groups.presentation.viewmodel.GroupsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val groupsModule = module {
    viewModelOf(::GroupsViewModel)
}
