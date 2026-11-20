package com.quare.blitzsplit.feature.restore.di

import com.quare.blitzsplit.feature.restore.presentation.viewmodel.RestoreViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val restoreModule = module {
    factoryOf(::RestoreViewModel)
}
