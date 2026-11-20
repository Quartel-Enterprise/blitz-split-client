package com.quare.blitzsplit.feature.contactsupport.di

import com.quare.blitzsplit.feature.contactsupport.presentation.viewmodel.ContactSupportViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val contactSupportModule = module {
    factoryOf(::ContactSupportViewModel)
}
