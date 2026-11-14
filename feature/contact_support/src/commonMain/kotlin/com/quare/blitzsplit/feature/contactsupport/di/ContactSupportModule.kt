package com.quare.blitzsplit.feature.contactsupport.di

import com.quare.blitzsplit.feature.contactsupport.presentation.viewmodel.ContactSupportViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val contactSupportModule = module {
    viewModelOf(::ContactSupportViewModel)
}
