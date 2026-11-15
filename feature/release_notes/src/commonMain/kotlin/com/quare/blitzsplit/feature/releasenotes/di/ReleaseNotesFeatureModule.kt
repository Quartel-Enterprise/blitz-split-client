package com.quare.blitzsplit.feature.releasenotes.di

import com.quare.blitzsplit.feature.releasenotes.presentation.viewmodel.ReleaseNotesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val releaseNotesFeatureModule = module {
    viewModelOf(::ReleaseNotesViewModel)
}
