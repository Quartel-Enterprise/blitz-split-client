package com.quare.blitzsplit.core.contacts.domain.usecase

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge

fun interface RequestGuestMerge {
    suspend operator fun invoke(contact: Contact): Result<GuestMerge>
}
