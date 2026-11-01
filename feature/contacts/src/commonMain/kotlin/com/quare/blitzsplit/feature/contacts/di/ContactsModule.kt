package com.quare.blitzsplit.feature.contacts.di

import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.AgendaViewModel
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactInviteViewModel
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactProfileViewModel
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Why: the profile and the invite get their route as a parameter (parametersOf(route)).
val contactsModule = module {
    viewModelOf(::ContactsViewModel)
    viewModelOf(::ContactProfileViewModel)
    viewModelOf(::AgendaViewModel)
    viewModelOf(::ContactInviteViewModel)
}
