package com.quare.blitzsplit.feature.contacts

import com.quare.blitzsplit.core.contacts.domain.usecase.ObserveContactBook
import com.quare.blitzsplit.core.contacts.domain.usecase.impl.ObserveContactBookUseCase
import com.quare.blitzsplit.core.contacts.testing.FakeHiddenContactRepository
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeExpenseRepository
import com.quare.blitzsplit.core.expenses.testing.FakeSettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.usecase.impl.ObserveGroupsUseCase
import com.quare.blitzsplit.core.groups.testing.FakeGroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType

/*
 * Test data the Contatos tests share. Three groups, most recently active first: Viagem Floripa (Carla, Bruno and
 * Diego, a guest), Apê 302 (Carla again) and Futebol de quinta (Júlia, settled). Carla owes you 50,00, you owe
 * Bruno 20,00, Diego owes you 45,00.
 */

internal const val FLORIPA = "floripa"
internal const val APE = "ape"
internal const val FUTEBOL = "futebol"
internal const val CARLA_KEY = "user:carla"
internal const val BRUNO_KEY = "user:bruno"
internal const val DIEGO_KEY = "name:diego"
internal const val JULIA_KEY = "user:julia"

internal fun meId(groupId: String): String = "me-$groupId"

internal fun memberId(
    name: String,
    groupId: String,
): String = "$name-$groupId"

internal fun member(
    id: String,
    name: String,
    userId: String?,
    inviteCode: String? = null,
): GroupMember = GroupMember(
    id = id,
    name = name,
    avatarColor = AvatarColor.BLUE,
    isMe = false,
    hasJoined = userId != null,
    userId = userId,
    inviteCode = inviteCode,
)

internal fun me(
    groupId: String,
    inviteCode: String? = null,
): GroupMember = GroupMember(
    id = meId(groupId),
    name = "Pierre",
    avatarColor = AvatarColor.LILAC,
    isMe = true,
    hasJoined = true,
    userId = "pierre",
    inviteCode = inviteCode,
)

internal fun group(
    id: String,
    name: String,
    vararg members: GroupMember,
    updatedAt: Long = 0L,
    inviteCode: String? = null,
): Group = Group(
    id = id,
    name = name,
    type = GroupType.OTHER,
    color = AvatarColor.GREEN,
    currency = GroupCurrency.BRL,
    members = members.toList(),
    isPending = false,
    inviteCode = inviteCode,
    isCreatedByMe = true,
    defaultSplit = null,
    updatedAt = updatedAt,
)

internal fun floripa(
    inviteCode: String? = "Floripa1",
    withDiego: Boolean = true,
): Group = group(
    FLORIPA,
    "Viagem Floripa",
    *listOfNotNull(
        me(FLORIPA, inviteCode = "pierr1"),
        member(memberId("carla", FLORIPA), "Carla", "carla"),
        member(memberId("bruno", FLORIPA), "Bruno", "bruno"),
        member(
            memberId("diego", FLORIPA),
            "Diego",
            userId = null,
            inviteCode = "diego1",
        ).takeIf { withDiego },
    ).toTypedArray(),
    updatedAt = 3_000L,
    inviteCode = inviteCode,
)

internal fun ape(inviteCode: String? = "ApeCode2"): Group = group(
    APE,
    "Apê 302",
    me(APE),
    member(memberId("carla", APE), "Carla", "carla"),
    updatedAt = 2_000L,
    inviteCode = inviteCode,
)

internal fun futebol(inviteCode: String? = null): Group = group(
    FUTEBOL,
    "Futebol de quinta",
    me(FUTEBOL),
    member(memberId("julia", FUTEBOL), "Júlia", "julia"),
    updatedAt = 1_000L,
    inviteCode = inviteCode,
)

internal fun defaultGroups(): List<Group> = listOf(floripa(), ape(), futebol())

// Why: a bill one member paid, all of it on another, so the second owes the first that much.
internal fun owes(
    groupId: String,
    fromMemberId: String,
    toMemberId: String,
    amountMinor: Long,
    id: String,
    spentAt: Long = 0L,
    updatedAt: Long = 0L,
): Expense = Expense(
    id = id,
    groupId = groupId,
    description = id,
    amountMinor = amountMinor,
    splitMode = SplitMode.AMOUNT,
    payers = listOf(
        ExpensePayer(
            memberId = toMemberId,
            amountMinor = amountMinor,
        ),
    ),
    shares = listOf(
        ExpenseShare(
            memberId = fromMemberId,
            amountMinor = amountMinor,
            weight = amountMinor,
        ),
    ),
    items = null,
    spentAt = spentAt,
    isPending = false,
    updatedAt = updatedAt,
    updatedByMemberId = null,
)

internal fun defaultBills(): List<Expense> = listOf(
    owes(FLORIPA, memberId("carla", FLORIPA), meId(FLORIPA), 5_000L, id = "dinner"),
    owes(FLORIPA, meId(FLORIPA), memberId("bruno", FLORIPA), 2_000L, id = "taxi"),
    owes(FLORIPA, memberId("diego", FLORIPA), meId(FLORIPA), 4_500L, id = "meat"),
)

// Why: the real book over the fakes, so hiding someone or a sync reaches the screen as it would from Room.
internal fun observeContactBookOf(
    groups: FakeGroupRepository,
    expenses: FakeExpenseRepository,
    settlements: FakeSettlementRepository,
    hidden: FakeHiddenContactRepository,
): ObserveContactBook = ObserveContactBookUseCase(
    observeGroups = ObserveGroupsUseCase(groups),
    observeGroupsMoney = ObserveGroupsMoneyUseCase(expenses, settlements),
    hiddenContacts = hidden,
)
