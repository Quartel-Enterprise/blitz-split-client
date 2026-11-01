package com.quare.blitzsplit.core.contacts.domain.model

import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: someone this device's user shares a group with (Contatos, flow 11), once whatever the groups: an account in
 * every group they joined, a name nobody joined as wherever it is written the same, as Saldos counts them.
 */
data class Contact(
    // Why: PeopleBalances.keyOf, the same person Saldos nets, what Route.ContactProfile and "Ocultar" keep.
    val key: String,
    // Why: as the group most recently active names them.
    val name: String,
    val color: AvatarColor,
    // Why: null for a guest, a name nobody joined as ("convidado sem app").
    val userId: String?,
    // Why: "Grupos em comum", the most recently active first, those with nothing open included.
    val groups: List<ContactGroup>,
    // Why: the net with them in each currency of those groups, the currency with the most groups first.
    val nets: List<ContactNet>,
    // Why: Ocultos, at the end of the list with "Mostrar"; never while something is open with them.
    val isHidden: Boolean,
) {
    val isGuest: Boolean
        get() = userId == null

    // Why: what the row and the profile show: the first currency with something open, else the main one.
    val net: ContactNet
        get() = nets.firstOrNull { it.amountMinor != 0L } ?: nets.first()

    // Why: "Ocultar de Contatos" only at zero, in every currency.
    val isSettled: Boolean
        get() = nets.all { it.amountMinor == 0L }
}
