package com.quare.blitzsplit.feature.registerpayment.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.SettlementLeg
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentPerson
import com.quare.blitzsplit.feature.registerpayment.presentation.model.PaymentTarget
import kotlin.math.abs

// Why: a debt with someone removed from the group stays until it is paid, "Ex-membro" on the form.
internal fun pairTargetOf(
    route: Route.RegisterPayment,
    group: Group,
    balances: GroupBalances,
): PaymentTarget {
    val debtMinor = balances.debtOf(route.fromMemberId, route.toMemberId)
    val receiver = group.members.firstOrNull { it.id == route.toMemberId }
    return PaymentTarget(
        currency = group.currency,
        from = group.members.firstOrNull { it.id == route.fromMemberId }?.let(::toPerson),
        to = receiver?.let(::toPerson),
        toUserId = receiver?.userId,
        debtMinor = debtMinor,
        groupName = group.name,
        legs = listOf(
            SettlementLeg(
                groupId = route.groupId,
                fromMemberId = route.fromMemberId,
                toMemberId = route.toMemberId,
                amountMinor = debtMinor,
            ),
        ),
        groupParts = emptyList(),
        simplifiedNote = simplifiedPairNoteOrNull(route, group, balances),
        simplifiesDebts = group.simplifiesDebts,
        simplifyChangedByName = group.simplifyDebtsChangedBy?.let { userId ->
            group.members.firstOrNull { it.userId == userId }?.name
        },
    )
}

// Why: the net decides who pays whom; each group's payment goes the way that group's pair owes.
internal fun personTargetOf(
    route: Route.SettleUp,
    people: PeopleBalances,
): PaymentTarget? {
    val person = people.findPersonOrNull(route.personKey, route.currency.code) ?: return null
    val me = people.me?.let(::toPerson)
    val other = PaymentPerson(
        id = person.groups.first().memberId,
        name = person.name.orEmpty(),
        initials = person.name?.let(Initials::of).orEmpty(),
        avatarColor = person.face?.color ?: AvatarColor.default,
        photoUrl = person.face?.photoUrl,
        isMe = false,
        personKey = person.key,
    ).takeIf { person.name != null }
    val owesMe = person.netMinor > 0
    return PaymentTarget(
        currency = person.currency,
        from = if (owesMe) other else me,
        to = if (owesMe) me else other,
        toUserId = if (owesMe) null else PeopleBalances.userIdOf(person.key),
        debtMinor = abs(person.netMinor),
        groupName = person.groups.singleOrNull()?.groupName,
        legs = person.legs,
        groupParts = person.groups,
        simplifiedNote = null,
        simplifiesDebts = null,
        simplifyChangedByName = null,
    )
}

// Why: a member as the form and "Quem pagou a quem?" draw them.
internal fun toPerson(member: GroupMember): PaymentPerson = PaymentPerson(
    id = member.id,
    name = member.name,
    initials = Initials.ofMember(member.name, member.isMe),
    avatarColor = member.avatarColor,
    photoUrl = member.photoUrl,
    isMe = member.isMe,
    personKey = PeopleBalances.keyOf(member),
)
