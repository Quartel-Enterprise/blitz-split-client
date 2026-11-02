package com.quare.blitzsplit.feature.categorymap.di

import com.quare.blitzsplit.feature.categorymap.presentation.viewmodel.CategoryMapViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val categoryMapModule = module {
    viewModelOf(::CategoryMapViewModel)
}
