package com.quare.blitzsplit.core.contacts.domain.model

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.utils.TextFolding

/*
 * Why: Contatos, worked out from the device's data, offline too: everyone in this device's user's groups, guests
 * included, each once (PeopleBalances.keyOf), with the same nets as Saldos (8) and the home's Grupos (5.1).
 */
object ContactBook {
    /*
     * Why: groups in the order of their last activity, so a person is named as in the group they were last active in
     * and "Grupos em comum" lists that group first. A to Z, accents folded, as people look a name up.
     */
    fun of(
        groups: List<Group>,
        money: Map<String, GroupMoney>,
        hidden: List<HiddenContact>,
    ): List<Contact> {
        val hiddenByKey = hidden.filter { it.isHidden }.associateBy { it.personKey }
        return groups
            .flatMap { group -> partsOf(group, money[group.id] ?: GroupMoney.NONE) }
            .groupBy { it.key }
            .map { (key, parts) -> contactOf(key, parts, hiddenByKey[key]) }
            .sortedWith(compareBy<Contact> { TextFolding.toFolded(it.name) }.thenBy { it.key })
    }

    private fun partsOf(
        group: Group,
        money: GroupMoney,
    ): List<Part> {
        val me = group.members.firstOrNull { it.isMe } ?: return emptyList()
        val balances = GroupBalances.of(money.expenses, money.settlements)
        return group.members
            .filterNot { it.isMe }
            .map { member ->
                Part(
                    key = PeopleBalances.keyOf(member),
                    member = member,
                    group = ContactGroup(
                        groupId = group.id,
                        groupName = group.name,
                        groupInitials = Initials.of(group.name),
                        groupColor = group.color,
                        currency = group.currency,
                        memberId = member.id,
                        meMemberId = me.id,
                        groupInviteCode = group.inviteCode,
                        memberInviteCode = member.inviteCode,
                        meInviteCode = me.inviteCode,
                        amountMinor = balances.debtOf(member.id, me.id) - balances.debtOf(me.id, member.id),
                    ),
                    lastSharedBillAt = money.expenses
                        .filter { it.isInvolving(member.id) && it.isInvolving(me.id) }
                        .maxOfOrNull { it.updatedAt },
                )
            }
    }

    private fun contactOf(
        key: String,
        parts: List<Part>,
        hidden: HiddenContact?,
    ): Contact {
        val named = parts.first().member
        val nets = parts
            .groupBy { it.group.currency }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<GroupCurrency, List<Part>>> { it.value.size }.thenBy { it.key.code },
            ).map { (currency, ofCurrency) -> ContactNet(currency, ofCurrency.sumOf { it.group.amountMinor }) }
        val lastSharedBillAt = parts.mapNotNull { it.lastSharedBillAt }.maxOrNull()
        val isSettled = nets.all { it.amountMinor == 0L }
        return Contact(
            key = key,
            name = named.name,
            color = named.avatarColor,
            userId = parts.firstNotNullOfOrNull { it.member.userId },
            groups = parts.map { it.group },
            nets = nets,
            // Why: a new bill with them, or a balance open again, brings them back by themselves.
            isHidden = hidden != null && isSettled && (lastSharedBillAt == null || lastSharedBillAt <= hidden.hiddenAt),
        )
    }

    private fun Expense.isInvolving(memberId: String): Boolean =
        payers.any { it.memberId == memberId } || shares.any { it.memberId == memberId }

    private class Part(
        val key: String,
        val member: GroupMember,
        val group: ContactGroup,
        val lastSharedBillAt: Long?,
    )
}
