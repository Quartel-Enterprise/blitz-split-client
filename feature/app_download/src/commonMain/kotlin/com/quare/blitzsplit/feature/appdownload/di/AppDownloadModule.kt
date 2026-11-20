package com.quare.blitzsplit.feature.appdownload.di

import com.quare.blitzsplit.feature.appdownload.presentation.viewmodel.AppDownloadViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appDownloadModule = module {
    factoryOf(::AppDownloadViewModel)
}
