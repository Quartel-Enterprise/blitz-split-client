package com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.registerpayment.presentation.model.NotePerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.SimplifiedPairNote

// Why: a pair as the bills made it says nothing; one the simplification made or grew says whom it carries instead.
internal fun simplifiedPairNoteOrNull(
    route: Route.RegisterPayment,
    group: Group,
    balances: GroupBalances,
): SimplifiedPairNote? {
    val insteadOf = balances.insteadOfOrNull(route.fromMemberId, route.toMemberId) ?: return null
    return SimplifiedPairNote(
        isNewPair = balances.unsimplifiedDebts.orEmpty().none {
            it.fromMemberId == route.fromMemberId && it.toMemberId == route.toMemberId
        },
        insteadOf = insteadOf.map { id ->
            val member = group.members.firstOrNull { it.id == id }
            NotePerson(
                name = member?.name,
                isMe = member?.isMe == true,
            )
        },
    )
}
