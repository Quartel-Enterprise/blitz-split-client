package com.quare.blitzsplit.core.contacts.domain.model

import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.groups.domain.model.Group

/*
 * Why: "Histórico com Carla": the bills that move something between this device's user and the person, as Saldos
 * counts them (GroupBalances.findDebtsWithin), and the payments between the two, the most recent first.
 */
object ContactHistory {
    fun of(
        contact: Contact,
        groups: List<Group>,
        money: Map<String, GroupMoney>,
    ): List<ContactHistoryItem> = contact.groups
        .flatMap { part ->
            val group = groups.firstOrNull { it.id == part.groupId } ?: return@flatMap emptyList()
            val ofGroup = money[part.groupId] ?: GroupMoney.NONE
            billsOf(group, ofGroup, part) + paymentsOf(ofGroup, part)
        }.sortedWith(compareByDescending<ContactHistoryItem> { it.at }.thenBy { it.id })

    private fun billsOf(
        group: Group,
        money: GroupMoney,
        part: ContactGroup,
    ): List<ContactHistoryItem> = money.expenses.mapNotNull { expense ->
        val debts = GroupBalances.findDebtsWithin(expense)
        val effect = debts
            .filter { it.fromMemberId == part.memberId && it.toMemberId == part.meMemberId }
            .sumOf { it.amountMinor } -
            debts
                .filter { it.fromMemberId == part.meMemberId && it.toMemberId == part.memberId }
                .sumOf { it.amountMinor }
        if (effect == 0L) return@mapNotNull null
        val payer = expense.payers.singleOrNull()
        ContactHistoryItem.Bill(
            id = expense.id,
            groupName = part.groupName,
            currency = part.currency,
            at = expense.spentAt,
            description = expense.description,
            payerName = payer?.let { paid -> group.members.firstOrNull { it.id == paid.memberId }?.name },
            isPaidByMe = payer?.memberId == part.meMemberId,
            payerCount = expense.payers.size,
            totalMinor = expense.amountMinor,
            effectMinor = effect,
        )
    }

    private fun paymentsOf(
        money: GroupMoney,
        part: ContactGroup,
    ): List<ContactHistoryItem> = money.settlements
        .filter { settlement ->
            (settlement.fromMemberId == part.meMemberId && settlement.toMemberId == part.memberId) ||
                (settlement.fromMemberId == part.memberId && settlement.toMemberId == part.meMemberId)
        }.map { settlement ->
            ContactHistoryItem.Payment(
                id = settlement.id,
                groupName = part.groupName,
                currency = part.currency,
                at = settlement.paidAt,
                isFromMe = settlement.fromMemberId == part.meMemberId,
                amountMinor = settlement.amountMinor,
                method = settlement.method,
            )
        }
}
