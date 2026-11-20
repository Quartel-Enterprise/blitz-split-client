package com.quare.blitzsplit.feature.watchbalances

import com.quare.blitzsplit.core.expenses.domain.model.GroupFace
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.expenses.domain.model.PersonBalance
import com.quare.blitzsplit.core.expenses.domain.model.PersonGroupBalance
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency

internal const val FLORIPA = "floripa"
internal const val APE = "ape"
internal const val CHURRASCO = "churrasco"

internal fun meIn(groupId: String): String = "me-$groupId"

internal fun nameOf(groupId: String): String = when (groupId) {
    FLORIPA -> "Floripa"
    APE -> "Apê"
    else -> "Churrasco"
}

internal fun colorOf(groupId: String): AvatarColor = when (groupId) {
    FLORIPA -> AvatarColor.BLUE
    APE -> AvatarColor.GREEN
    else -> AvatarColor.PEACH
}

// Why: one group's pair between you and someone, positive when they owe you.
internal fun pair(
    groupId: String,
    memberId: String,
    amountMinor: Long,
): PersonGroupBalance = PersonGroupBalance(
    groupId = groupId,
    groupName = nameOf(groupId),
    groupInitials = nameOf(groupId).take(2).uppercase(),
    groupColor = colorOf(groupId),
    groupPhotoUrl = "https://photos/$groupId.jpg",
    memberId = memberId,
    meMemberId = meIn(groupId),
    amountMinor = amountMinor,
    isSimplified = false,
    insteadOfNames = emptyList(),
)

internal fun person(
    name: String?,
    vararg groups: PersonGroupBalance,
    currency: GroupCurrency = GroupCurrency.BRL,
    face: GroupFace? = name?.let {
        GroupFace(
            initials = it.take(1),
            color = AvatarColor.PEACH,
            photoUrl = null,
        )
    },
): PersonBalance = PersonBalance(
    key = name?.let { "name:${it.lowercase()}" } ?: "gone:${groups.first().memberId}",
    name = name,
    face = face,
    currency = currency,
    netMinor = groups.sumOf { it.amountMinor },
    groups = groups.toList(),
    isGuest = false,
    guestBills = emptyList(),
    guestPaidMinor = null,
)

internal fun total(
    currency: GroupCurrency,
    toReceiveMinor: Long,
    toPayMinor: Long,
): PeopleTotal = PeopleTotal(
    currency = currency,
    netMinor = toReceiveMinor - toPayMinor,
    toReceiveMinor = toReceiveMinor,
    toPayMinor = toPayMinor,
    toReceiveCount = if (toReceiveMinor > 0) 1 else 0,
    toPayCount = if (toPayMinor > 0) 1 else 0,
)

internal fun balances(
    vararg people: PersonBalance,
    totals: List<PeopleTotal> = emptyList(),
): PeopleBalances = PeopleBalances(
    people = people.toList(),
    totals = totals,
    me = null,
)

internal fun settlement(
    groupId: String,
    isPending: Boolean,
    currency: GroupCurrency = GroupCurrency.BRL,
): Settlement = Settlement(
    id = "settlement-$groupId-${currency.code}-$isPending",
    groupId = groupId,
    fromMemberId = meIn(groupId),
    toMemberId = "someone",
    amountMinor = 1_000L,
    currency = currency,
    method = SettlementMethod.OTHER,
    paidAt = 1L,
    isPending = isPending,
    updatedAt = 1L,
)

internal fun moneyWith(vararg settlements: Settlement): GroupMoney = GroupMoney(
    expenses = emptyList(),
    settlements = settlements.toList(),
    pendingDeletionCount = 0,
)
