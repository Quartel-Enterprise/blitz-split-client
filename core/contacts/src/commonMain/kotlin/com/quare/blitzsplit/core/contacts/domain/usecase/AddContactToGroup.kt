package com.quare.blitzsplit.core.contacts.domain.usecase

import com.quare.blitzsplit.core.contacts.domain.model.Contact

fun interface AddContactToGroup {
    /*
     * Why: someone who uses the app joins straight in as their account, with no part in the old bills; a guest goes in
     * as a name nobody joined as. On the device first, offline too; the sync takes it up.
     */
    suspend operator fun invoke(
        contact: Contact,
        groupId: String,
    ): Result<Unit>
}
