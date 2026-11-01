package com.quare.blitzsplit.core.contacts.domain.usecase

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge

fun interface FindPendingGuestMerge {
    suspend operator fun invoke(contact: Contact): Result<GuestMerge?>
}
