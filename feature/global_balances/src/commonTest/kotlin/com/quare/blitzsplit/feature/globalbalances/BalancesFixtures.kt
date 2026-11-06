package com.quare.blitzsplit.feature.globalbalances

import com.quare.blitzsplit.core.activity.domain.model.ChargeSent
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType

// Why: the people Saldos and Cobrar are tested on, the canvas's, shared so both tests read the same balances.
internal const val TRIP = "trip"
internal const val ME_TRIP = "me-trip"
internal const val BRUNO_TRIP = "bruno-trip"
internal const val DIEGO_TRIP = "diego-trip"
internal const val FLORIPA = "floripa"
internal const val APE = "ape"
internal const val CHURRASCO = "churrasco"
internal const val ME_FLORIPA = "me-floripa"
internal const val ME_APE = "me-ape"
internal const val ME_CHURRASCO = "me-churrasco"
internal const val ANA_FLORIPA = "ana-floripa"
internal const val BRUNO_FLORIPA = "bruno-floripa"
internal const val CARLA_FLORIPA = "carla-floripa"
internal const val CARLA_APE = "carla-ape"
internal const val CARLA_CHURRASCO = "carla-churrasco"
internal const val DIEGO_CHURRASCO = "diego-churrasco"

internal val pixKey = PaymentKey(
    "key-1",
    PaymentKeyType.EMAIL,
    "pierre@email.com",
    isDefault = true,
    addedAt = 0L,
)

internal val bitcoinKey =
    PaymentKey(
        "key-2",
        PaymentKeyType.LIGHTNING,
        "pierre@ln.com",
        isDefault = true,
        addedAt = 1L,
    )

// Why: the canvas's people, a little simpler: Bruno you owe, Ana, Diego (a guest) and Carla (across three groups).
internal val defaultBills = listOf(
    owes(FLORIPA, ME_FLORIPA, BRUNO_FLORIPA, 12_600L, id = "e1"),
    owes(FLORIPA, ANA_FLORIPA, ME_FLORIPA, 16_230L, id = "e2"),
    owes(FLORIPA, CARLA_FLORIPA, ME_FLORIPA, 9_840L, id = "e3"),
    owes(APE, ME_APE, CARLA_APE, 9_800L, id = "e4"),
    owes(CHURRASCO, CARLA_CHURRASCO, ME_CHURRASCO, 3_250L, id = "e5"),
    owes(
        CHURRASCO,
        DIEGO_CHURRASCO,
        ME_CHURRASCO,
        4_500L,
        id = "e6",
        description = "Carne",
    ),
)

internal val defaultGroups = listOf(
    group(
        FLORIPA,
        "Viagem Floripa",
        me(ME_FLORIPA),
        joined(ANA_FLORIPA, "Ana", "ana"),
        joined(BRUNO_FLORIPA, "Bruno", "bruno"),
        joined(CARLA_FLORIPA, "Carla", "carla"),
    ),
    group(APE, "Apê 302", me(ME_APE), joined(CARLA_APE, "Carla", "carla")),
    group(
        CHURRASCO,
        "Churrasco de sábado",
        me(ME_CHURRASCO),
        joined(CARLA_CHURRASCO, "Carla", "carla"),
        GroupMember(
            DIEGO_CHURRASCO,
            "Diego",
            AvatarColor.GREEN,
            isMe = false,
            hasJoined = false,
            userId = null,
            inviteCode = null,
            photoUrl = null,
        ),
    ),
)

internal fun me(id: String): GroupMember = GroupMember(
    id,
    "Pierre",
    AvatarColor.BLUE,
    isMe = true,
    hasJoined = true,
    userId = "pierre",
    inviteCode = null,
    photoUrl = null,
)

internal fun joined(
    id: String,
    name: String,
    userId: String,
): GroupMember = GroupMember(
    id,
    name,
    AvatarColor.LILAC,
    isMe = false,
    hasJoined = true,
    userId = userId,
    inviteCode = null,
    photoUrl = null,
)

internal fun group(
    id: String,
    name: String,
    vararg members: GroupMember,
    currency: GroupCurrency = GroupCurrency.BRL,
): Group = Group(
    id = id,
    name = name,
    type = GroupType.OTHER,
    color = AvatarColor.PEACH,
    shape = GroupShape.SQUARE,
    photo = null,
    currency = currency,
    members = members.toList(),
    isPending = false,
    inviteCode = null,
    isCreatedByMe = true,
    defaultSplit = null,
    updatedAt = 0L,
    simplifiesDebts = false,
    isSimplifyDebtsPending = false,
    simplifyDebtsChangedBy = null,
    simplifyDebtsConflictBy = null,
)

// Why: a bill only one member paid, all of it on another, so the second owes the first that much.
internal fun owes(
    groupId: String,
    fromMemberId: String,
    toMemberId: String,
    amountMinor: Long,
    id: String = "bill-$groupId",
    description: String = "Bill",
): Expense = Expense(
    id = id,
    groupId = groupId,
    description = description,
    currency = GroupCurrency.BRL,
    amountMinor = amountMinor,
    splitMode = SplitMode.AMOUNT,
    payers = listOf(ExpensePayer(toMemberId, amountMinor)),
    shares = listOf(ExpenseShare(fromMemberId, amountMinor, amountMinor)),
    items = null,
    categoryId = null,
    spentAt = 0L,
    isPending = false,
    updatedAt = 0L,
    updatedByMemberId = null,
)

// Why: Carla owes you in Floripa and the Churrasco, while you owe her in the Apê.
internal val carlaCharges = listOf(
    ChargeSent(FLORIPA, CARLA_FLORIPA, "Carla", 9_840L),
    ChargeSent(CHURRASCO, CARLA_CHURRASCO, "Carla", 3_250L),
)

internal val simplifiedTripBills = listOf(
    owes(TRIP, DIEGO_TRIP, BRUNO_TRIP, 6_400L, id = "bill-diego"),
    owes(TRIP, BRUNO_TRIP, ME_TRIP, 6_400L, id = "bill-bruno"),
)

// Why: Diego owes Bruno and Bruno owes you the same, which simplified is Diego paying you.
internal fun simplifiedTrip(simplifies: Boolean): Group = group(
    TRIP,
    "Viagem",
    me(ME_TRIP),
    joined(BRUNO_TRIP, "Bruno", "bruno"),
    joined(DIEGO_TRIP, "Diego", "diego"),
).copy(simplifiesDebts = simplifies)
