package com.quare.blitzsplit.core.contacts.domain.usecase

import com.quare.blitzsplit.core.contacts.domain.model.Contact

fun interface ClaimGuestAsMe {
    suspend operator fun invoke(contact: Contact): Result<Unit>
}
