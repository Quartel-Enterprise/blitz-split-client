package com.quare.blitzsplit.feature.watchpairing.di

import com.quare.blitzsplit.feature.watchpairing.presentation.viewmodel.WatchPairingViewModel
import com.quare.blitzsplit.feature.watchpairing.presentation.viewmodel.WatchRootViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val watchPairingModule = module {
    factoryOf(::WatchPairingViewModel)
    factoryOf(::WatchRootViewModel)
}
