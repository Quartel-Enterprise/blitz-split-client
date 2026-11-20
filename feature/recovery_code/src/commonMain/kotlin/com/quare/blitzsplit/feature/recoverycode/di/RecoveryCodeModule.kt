package com.quare.blitzsplit.feature.recoverycode.di

import com.quare.blitzsplit.feature.recoverycode.presentation.viewmodel.RecoveryCodeViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val recoveryCodeModule = module {
    factoryOf(::RecoveryCodeViewModel)
}
