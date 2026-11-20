package com.quare.blitzsplit.feature.connectwatch.di

import com.quare.blitzsplit.feature.connectwatch.presentation.viewmodel.ConnectWatchViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val connectWatchModule = module {
    factoryOf(::ConnectWatchViewModel)
}
