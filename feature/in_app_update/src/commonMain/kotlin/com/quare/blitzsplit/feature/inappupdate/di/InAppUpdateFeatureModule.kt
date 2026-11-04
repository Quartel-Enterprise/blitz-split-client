package com.quare.blitzsplit.feature.inappupdate.di

import com.quare.blitzsplit.feature.inappupdate.presentation.viewmodel.InAppUpdateDownloadViewModel
import com.quare.blitzsplit.feature.inappupdate.presentation.viewmodel.InAppUpdateViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val inAppUpdateFeatureModule = module {
    viewModelOf(::InAppUpdateViewModel)
    viewModelOf(::InAppUpdateDownloadViewModel)
}
