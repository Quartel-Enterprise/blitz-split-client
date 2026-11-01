package com.quare.blitzsplit.feature.contacts.presentation.model

/*
 * Why: someone in the device's agenda, as the platform read it (Android's ContactsContract, Apple's Contacts). Only on
 * the device: no number goes to the server.
 */
data class AgendaContact(
    // Why: the platform's own id, stable while the agenda doesn't change.
    val id: String,
    val name: String,
    // Why: "(11) 98765-4321" as the agenda has it, null for a contact with no number.
    val phone: String?,
)
