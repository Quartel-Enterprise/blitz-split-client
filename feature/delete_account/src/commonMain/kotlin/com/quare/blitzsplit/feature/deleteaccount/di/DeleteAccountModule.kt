package com.quare.blitzsplit.feature.deleteaccount.di

import com.quare.blitzsplit.feature.deleteaccount.presentation.viewmodel.DeleteAccountViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val deleteAccountModule = module {
    viewModelOf(::DeleteAccountViewModel)
}
