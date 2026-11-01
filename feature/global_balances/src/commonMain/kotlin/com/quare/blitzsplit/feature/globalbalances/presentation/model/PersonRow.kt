package com.quare.blitzsplit.feature.globalbalances.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.PersonGroupBalance
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency

// Why: one person in Saldos, netted across the groups of one currency.
data class PersonRow(
    // Why: the person and the currency, unique in the list.
    val id: String,
    // Why: null for someone no longer in the group, whom each UI calls "Ex-membro".
    val name: String?,
    val initials: String,
    val color: AvatarColor?,
    val currency: GroupCurrency,
    // Why: positive when they owe you, negative when you owe them.
    val netMinor: Long,
    // Why: "Por grupo", the most recently active group first.
    val groups: List<PersonGroupBalance>,
    // Why: "· convidado": nobody joined as them, so they only learn about it from Cobrar's message.
    val isGuest: Boolean,
    // Why: "Por grupo" open, by a tap or because they are alone in the filter.
    val isExpanded: Boolean,
) {
    // Why: "Recebi" and "Cobrar"; otherwise "Acertar".
    val owesYou: Boolean
        get() = netMinor > 0

    // Why: "Viagem Floripa" for one group, "3 grupos" for more.
    val groupCount: Int
        get() = groups.size

    // Why: "Líquido, já compensado" closes a breakdown of several groups, "Total" one of a single group.
    val isNetOfSeveralGroups: Boolean
        get() = groups.size > 1
}
