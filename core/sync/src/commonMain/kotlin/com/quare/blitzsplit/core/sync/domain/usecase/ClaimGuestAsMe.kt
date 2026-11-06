package com.quare.blitzsplit.core.sync.domain.usecase

import com.quare.blitzsplit.core.contacts.domain.model.Contact

fun interface ClaimGuestAsMe {
    /*
     * Why: "Eu sou Diego": everything in the guest's name passes to the user's own member in every group, for good, and
     * the answer is how many members moved (0 when the guest only exists on this device, or another device claimed it
     * first). The device has pushed what it has before, the server is asked under the sync lock and a sync follows, all
     * of it surviving the screen, so a guest the server took leaves the device too.
     */
    suspend operator fun invoke(contact: Contact): Result<Int>
}
