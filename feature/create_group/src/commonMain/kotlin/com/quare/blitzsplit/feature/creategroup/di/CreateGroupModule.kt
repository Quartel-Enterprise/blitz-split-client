package com.quare.blitzsplit.feature.creategroup.di

import com.quare.blitzsplit.feature.creategroup.presentation.viewmodel.CreateGroupViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val createGroupModule = module {
    viewModelOf(::CreateGroupViewModel)
}
