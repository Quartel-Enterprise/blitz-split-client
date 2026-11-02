package com.quare.blitzsplit.feature.billdetail.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.PayerOrder
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPayer
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillShareRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.MyPartNote

// Why: in PayerOrder's order; someone who left the group has no name and is drawn as "Ex-membro".
internal fun payersOf(
    group: Group,
    expense: Expense,
    membersById: Map<String, GroupMember>,
): List<BillPayer> = PayerOrder(group.members).sort(expense.payers.map { it.memberId }).map { id ->
    val member = membersById[id]
    BillPayer(
        name = member?.name,
        initials = member?.let { Initials.ofMember(it.name, it.isMe) }.orEmpty(),
        avatarColor = member?.avatarColor ?: AvatarColor.entries.first(),
        photoUrl = member?.photoUrl,
        isMe = member?.isMe == true,
    )
}

internal fun rowsOf(
    group: Group,
    expense: Expense,
): List<BillShareRow> {
    val paidById = expense.payers.groupBy { it.memberId }.mapValues { (_, payers) ->
        payers.sumOf { it.amountMinor }
    }
    val shareById = expense.shares.associate { it.memberId to it.amountMinor }
    val memberRows = group.members.map { member ->
        BillShareRow(
            memberId = member.id,
            name = member.name,
            initials = Initials.ofMember(member.name, member.isMe),
            avatarColor = member.avatarColor,
            photoUrl = member.photoUrl,
            isMe = member.isMe,
            hasJoined = member.hasJoined,
            paidMinor = paidById[member.id],
            shareMinor = shareById[member.id],
        )
    }
    val memberIds = group.members.map { it.id }.toSet()
    val formerIds = (expense.payers.map { it.memberId } + expense.shares.map { it.memberId })
        .distinct()
        .filterNot { it in memberIds }
    val formerRows = formerIds.map { id ->
        BillShareRow(
            memberId = id,
            name = null,
            initials = "",
            avatarColor = AvatarColor.entries.first(),
            photoUrl = null,
            isMe = false,
            hasJoined = true,
            paidMinor = paidById[id],
            shareMinor = shareById[id],
        )
    }
    return memberRows + formerRows
}

/*
 * Why: who owes whom within the bill, as Saldos counts it: with one payer, everyone else in the split owes them;
 * with several, whoever paid less than their part owes whoever paid more. Nothing to say for someone whose part
 * settles nothing: who paid only their own part, or got a zero part of a cent; nor for a debt only to people who
 * left the group.
 */
internal fun myPartNoteOf(
    expense: Expense,
    me: GroupMember,
    membersById: Map<String, GroupMember>,
): MyPartNote? {
    val balance = expense.balanceOf(me.id)
    if (balance == null || balance == 0L) return null
    val debts = GroupBalances.findDebtsWithin(expense)
    if (balance < 0) {
        val names = debts.filter { it.fromMemberId == me.id }.map { membersById[it.toMemberId]?.name }
        return names.takeIf { it.any { name -> name != null } }?.let(MyPartNote::OwesPayers)
    }
    val debtors = debts.filter { it.toMemberId == me.id }
    if (debtors.isEmpty()) return null
    val amounts = debtors.map { it.amountMinor }.distinct()
    return MyPartNote.OwedBy(
        names = debtors.map { membersById[it.fromMemberId]?.name },
        eachMinor = amounts.singleOrNull(),
        totalMinor = debtors.sumOf { it.amountMinor },
    )
}
