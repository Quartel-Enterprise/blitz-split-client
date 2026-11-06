package com.quare.blitzsplit.feature.editgroup.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.CurrencyBalances
import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.DebtShift
import com.quare.blitzsplit.core.expenses.domain.model.DebtShifts
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyAmount
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyCurrencyChange
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyDebtsConfirmation
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyName
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyPersonChange
import com.quare.blitzsplit.feature.editgroup.presentation.model.SimplifyQueuedPayment

/*
 * Why: the confirmation of the switch, from the pairs of each currency as its bills make them (raw) and the payments
 * on the device, those still in the queue included (13.3d). Each currency simplifies on its own and is never netted
 * with another. Turning off names only this device's user and counts the others (13.3b).
 */
internal fun simplifyConfirmationOf(
    group: Group,
    raws: List<CurrencyBalances>,
    settlements: List<Settlement>,
): SimplifyDebtsConfirmation {
    val order = group.members.map { it.id }
    val meId = group.members.firstOrNull { it.isMe }?.id
    val isTurningOn = !group.simplifiesDebts
    val perCurrency = raws.map { (currency, raw) ->
        val simplified = raw.toSimplified(order)
        val before = if (isTurningOn) raw.debts else simplified.debts
        val after = if (isTurningOn) simplified.debts else raw.debts
        val shifts = DebtShifts.shiftsOf(
            before = before,
            after = after,
            memberOrder = order,
            meId = meId,
        )
        Triple(SimplifyCurrencyChange(currency, before.size, after.size), currency, shifts)
    }
    val shown = perCurrency.flatMap { (_, currency, shifts) ->
        (if (isTurningOn) shifts else shifts.filter { it.memberId == meId }).map { currency to it }
    }
    return SimplifyDebtsConfirmation(
        isTurningOn = isTurningOn,
        groupName = group.name,
        currencies = perCurrency.map { it.first },
        people = shown.map { (currency, shift) -> toPersonChange(shift, currency, group) },
        othersChangingCount = if (isTurningOn) {
            0
        } else {
            perCurrency
                .flatMap { it.third }
                .map { it.memberId }
                .filter { it != meId }
                .distinct()
                .size
        },
        queuedPayments = settlements
            .filter { it.isPending }
            .map {
                SimplifyQueuedPayment(
                    from = nameOf(it.fromMemberId, group),
                    to = nameOf(it.toMemberId, group),
                    amountMinor = it.amountMinor,
                    currency = it.currency,
                )
            },
    )
}

private fun toPersonChange(
    shift: DebtShift,
    currency: GroupCurrency,
    group: Group,
): SimplifyPersonChange {
    val member = group.members.firstOrNull { it.id == shift.memberId }
    return SimplifyPersonChange(
        memberId = shift.memberId,
        currency = currency,
        person = nameOf(shift.memberId, group),
        initials = member?.let { Initials.ofMember(it.name, it.isMe) }.orEmpty(),
        avatarColor = member?.avatarColor ?: AvatarColor.default,
        photoUrl = member?.photoUrl,
        isGuest = member != null && !member.hasJoined,
        stopsPaying = shift.stopsPaying.map { toAmount(it, group) },
        pays = shift.pays.map { toAmount(it, group) },
        stopsReceivingFrom = shift.stopsReceivingFrom.map { nameOf(it, group) },
        receivesFrom = shift.receivesFrom.map { nameOf(it, group) },
    )
}

private fun toAmount(
    debt: Debt,
    group: Group,
): SimplifyAmount = SimplifyAmount(
    to = nameOf(debt.toMemberId, group),
    amountMinor = debt.amountMinor,
)

private fun nameOf(
    memberId: String,
    group: Group,
): SimplifyName {
    val member: GroupMember? = group.members.firstOrNull { it.id == memberId }
    return SimplifyName(
        name = member?.name,
        isMe = member?.isMe == true,
    )
}
