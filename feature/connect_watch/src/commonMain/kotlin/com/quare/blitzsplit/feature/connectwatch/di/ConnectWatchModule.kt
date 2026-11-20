package com.quare.blitzsplit.feature.connectwatch.di

import com.quare.blitzsplit.feature.connectwatch.presentation.viewmodel.ConnectWatchViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val connectWatchModule = module {
    viewModelOf(::ConnectWatchViewModel)
}
