package com.quare.blitzsplit.feature.welcome.di

import com.quare.blitzsplit.feature.welcome.presentation.viewmodel.WelcomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val welcomeModule = module {
    viewModelOf(::WelcomeViewModel)
}
