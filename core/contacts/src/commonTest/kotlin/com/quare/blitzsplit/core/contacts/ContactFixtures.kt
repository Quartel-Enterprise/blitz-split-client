package com.quare.blitzsplit.core.contacts

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.model.ContactGroup
import com.quare.blitzsplit.core.contacts.domain.model.ContactNet
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType

// Test data the contacts tests share: groups, their members, bills and payments, and contacts.

internal fun member(
    id: String,
    name: String,
    userId: String? = null,
    isMe: Boolean = false,
    color: AvatarColor = AvatarColor.BLUE,
    inviteCode: String? = null,
): GroupMember = GroupMember(
    id = id,
    name = name,
    avatarColor = color,
    isMe = isMe,
    hasJoined = isMe || userId != null,
    userId = userId,
    inviteCode = inviteCode,
)

// Why: this device's user is a member of their own in each group, as the device has them.
internal fun me(
    groupId: String,
    inviteCode: String? = null,
): GroupMember = member(
    id = meId(groupId),
    name = "Pierre",
    userId = "pierre",
    isMe = true,
    color = AvatarColor.LILAC,
    inviteCode = inviteCode,
)

internal fun meId(groupId: String): String = "me-$groupId"

internal fun group(
    id: String,
    vararg members: GroupMember,
    name: String = id,
    currency: GroupCurrency = GroupCurrency.BRL,
    updatedAt: Long = 0L,
    inviteCode: String? = null,
    color: AvatarColor = AvatarColor.GREEN,
): Group = Group(
    id = id,
    name = name,
    type = GroupType.OTHER,
    color = color,
    currency = currency,
    members = members.toList(),
    isPending = false,
    inviteCode = inviteCode,
    isCreatedByMe = true,
    defaultSplit = null,
    updatedAt = updatedAt,
)

internal fun bill(
    id: String,
    groupId: String,
    payers: List<ExpensePayer>,
    shares: List<ExpenseShare>,
    spentAt: Long = 0L,
    updatedAt: Long = 0L,
    description: String = id,
): Expense = Expense(
    id = id,
    groupId = groupId,
    description = description,
    amountMinor = payers.sumOf { it.amountMinor },
    splitMode = SplitMode.AMOUNT,
    payers = payers,
    shares = shares,
    items = null,
    spentAt = spentAt,
    isPending = false,
    updatedAt = updatedAt,
    updatedByMemberId = null,
)

// Why: a bill one member paid, all of it on another, so the second owes the first that much.
internal fun owes(
    groupId: String,
    fromMemberId: String,
    toMemberId: String,
    amountMinor: Long,
    id: String = "bill-$groupId",
    spentAt: Long = 0L,
    updatedAt: Long = 0L,
): Expense = bill(
    id = id,
    groupId = groupId,
    payers = listOf(payer(toMemberId, amountMinor)),
    shares = listOf(share(fromMemberId, amountMinor)),
    spentAt = spentAt,
    updatedAt = updatedAt,
)

internal fun payer(
    memberId: String,
    amountMinor: Long,
): ExpensePayer = ExpensePayer(
    memberId = memberId,
    amountMinor = amountMinor,
)

internal fun share(
    memberId: String,
    amountMinor: Long,
): ExpenseShare = ExpenseShare(
    memberId = memberId,
    amountMinor = amountMinor,
    weight = amountMinor,
)

internal fun payment(
    groupId: String,
    fromMemberId: String,
    toMemberId: String,
    amountMinor: Long,
    id: String = "payment-$groupId",
    paidAt: Long = 0L,
    updatedAt: Long = 0L,
    method: SettlementMethod = SettlementMethod.PIX,
): Settlement = Settlement(
    id = id,
    groupId = groupId,
    fromMemberId = fromMemberId,
    toMemberId = toMemberId,
    amountMinor = amountMinor,
    method = method,
    paidAt = paidAt,
    isPending = false,
    updatedAt = updatedAt,
)

internal fun moneyOf(
    expenses: List<Expense> = emptyList(),
    settlements: List<Settlement> = emptyList(),
): GroupMoney = GroupMoney(
    expenses = expenses,
    settlements = settlements,
    pendingDeletionCount = 0,
)

internal fun contactGroup(
    groupId: String,
    memberId: String,
    groupName: String = groupId,
    currency: GroupCurrency = GroupCurrency.BRL,
    amountMinor: Long = 0L,
): ContactGroup = ContactGroup(
    groupId = groupId,
    groupName = groupName,
    groupInitials = "",
    groupColor = AvatarColor.GREEN,
    currency = currency,
    memberId = memberId,
    meMemberId = meId(groupId),
    groupInviteCode = null,
    memberInviteCode = null,
    meInviteCode = null,
    amountMinor = amountMinor,
)

internal fun contact(
    key: String,
    name: String,
    userId: String? = null,
    groups: List<ContactGroup> = emptyList(),
    nets: List<ContactNet> = listOf(ContactNet(GroupCurrency.BRL, 0L)),
    color: AvatarColor = AvatarColor.BLUE,
    isHidden: Boolean = false,
): Contact = Contact(
    key = key,
    name = name,
    color = color,
    userId = userId,
    groups = groups,
    nets = nets,
    isHidden = isHidden,
)
