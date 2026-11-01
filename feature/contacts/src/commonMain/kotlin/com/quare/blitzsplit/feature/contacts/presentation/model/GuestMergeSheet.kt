package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.contacts.domain.model.GuestMerge
import com.quare.blitzsplit.core.groups.domain.model.InviteLink

/*
 * Why: the personal link of the request, made by the server as the sheet opens. Offline it says "Gerar o convite
 * pessoal precisa de internet." and makes it once back online.
 */
data class GuestMergeSheet(
    val merge: GuestMerge?,
    val isRequesting: Boolean,
    val hasFailed: Boolean,
) {
    val displayLink: String?
        get() = merge?.link?.let(InviteLink::displayOf)
}
