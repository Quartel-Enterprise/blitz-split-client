package com.quare.blitzsplit.feature.contacts.di

import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.AgendaViewModel
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactInviteViewModel
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactProfileViewModel
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ContactsViewModel
import com.quare.blitzsplit.feature.contacts.presentation.viewmodel.ProfileLinkViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Why: the profile, the invite and a profile link's page get their route as a parameter (parametersOf(route)).
val contactsModule = module {
    viewModelOf(::ContactsViewModel)
    viewModelOf(::ContactProfileViewModel)
    viewModelOf(::AgendaViewModel)
    viewModelOf(::ContactInviteViewModel)
    viewModelOf(::ProfileLinkViewModel)
}
