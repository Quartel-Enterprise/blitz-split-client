package com.quare.blitzsplit.feature.legal.di

import com.quare.blitzsplit.feature.legal.presentation.viewmodel.LegalDocumentViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val legalFeatureModule = module {
    viewModelOf(::LegalDocumentViewModel)
}
