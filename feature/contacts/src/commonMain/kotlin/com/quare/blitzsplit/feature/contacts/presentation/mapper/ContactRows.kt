package com.quare.blitzsplit.feature.contacts.presentation.mapper

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.feature.contacts.presentation.model.ContactRow

internal fun rowOf(contact: Contact): ContactRow = ContactRow(
    key = contact.key,
    name = contact.name,
    initials = Initials.of(contact.name),
    color = contact.color,
    net = contact.net,
    groupCount = contact.groups.size,
    firstGroupName = contact.groups.first().groupName,
    isGuest = contact.isGuest,
    match = null,
)
