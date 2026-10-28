package com.quare.blitzsplit.feature.profile.di

import com.quare.blitzsplit.feature.profile.presentation.viewmodel.ProfileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule = module {
    viewModelOf(::ProfileViewModel)
}
