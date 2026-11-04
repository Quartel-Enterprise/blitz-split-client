package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: a face of Início's "Saldos com pessoas": someone you have an open balance with, once per currency, netted
 * across the groups as Saldos (flow 8) nets them.
 */
data class HomePerson(
    // Why: PersonBalance.key, the profile tapping the face opens (Route.ContactProfile), or with the currency what it settles up.
    val key: String,
    // Why: null for someone no longer in the group, whom each UI calls "Ex-membro".
    val name: String?,
    val initials: String,
    val color: AvatarColor?,
    // Why: the profile photo over the initials, null for someone with none or nobody joined as.
    val photoUrl: String?,
    val currency: GroupCurrency,
    // Why: positive when they owe you ("te deve"), negative when you owe them ("você deve").
    val netMinor: Long,
    // Why: "convidado sem app": nobody joined as them, so they get no notice; the face wears the crossed-out phone.
    val isGuest: Boolean,
)
