package com.quare.blitzsplit.feature.welcome.di

import com.quare.blitzsplit.feature.welcome.presentation.viewmodel.WelcomeViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val welcomeModule = module {
    factoryOf(::WelcomeViewModel)
}
