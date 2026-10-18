package com.quare.blitzsplit.feature.recoverycode.di

import com.quare.blitzsplit.feature.recoverycode.presentation.viewmodel.RecoveryCodeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val recoveryCodeModule = module {
    viewModelOf(::RecoveryCodeViewModel)
}
