package com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.registerpayment.presentation.model.NotePerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentTarget
import com.quare.blitzsplit.feature.registerpayment.presentation.model.SimplifiedPairNote
import com.quare.blitzsplit.feature.registerpayment.presentation.model.SimplifyChangedNotice

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

/*
 * Why: 13.5c: the pair owes nothing only because someone turned "Simplificar dívidas" the other way since the form last
 * showed it (shownSimplifies, shownDebtMinor); a debt paid elsewhere, or one the form never showed, closes it instead.
 */
internal fun simplifyChangedNoticeOrNull(
    target: PaymentTarget?,
    shownSimplifies: Boolean?,
    shownDebtMinor: Long,
): SimplifyChangedNotice? {
    val simplifies = target?.simplifiesDebts ?: return null
    if (shownSimplifies == null || target.debtMinor != 0L || simplifies == shownSimplifies || shownDebtMinor == 0L) {
        return null
    }
    return SimplifyChangedNotice(
        changedByName = target.simplifyChangedByName,
        simplifiesDebts = simplifies,
        groupName = target.groupName.orEmpty(),
        previousDebtMinor = shownDebtMinor,
    )
}
