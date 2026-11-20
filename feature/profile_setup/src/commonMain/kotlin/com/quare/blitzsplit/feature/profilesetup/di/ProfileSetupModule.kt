package com.quare.blitzsplit.feature.profilesetup.di

import com.quare.blitzsplit.feature.profilesetup.presentation.viewmodel.ProfileSetupViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val profileSetupModule = module {
    factoryOf(::ProfileSetupViewModel)
}
