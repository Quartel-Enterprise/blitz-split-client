package com.quare.blitzsplit.feature.appdownload.di

import com.quare.blitzsplit.feature.appdownload.presentation.viewmodel.AppDownloadViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appDownloadModule = module {
    viewModelOf(::AppDownloadViewModel)
}
