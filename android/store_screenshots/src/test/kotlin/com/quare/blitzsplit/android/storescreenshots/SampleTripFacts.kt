package com.quare.blitzsplit.android.storescreenshots

import com.quare.blitzsplit.android.designsystem.layout.previewInitialsOf
import com.quare.blitzsplit.core.expenses.domain.model.BillItemsSplit
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.GroupFace
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.shared.storescreenshots.SampleBill
import com.quare.blitzsplit.shared.storescreenshots.SampleTrip
import com.quare.blitzsplit.shared.storescreenshots.SampleTrips

// Friday 23 October 2026, 00:00 UTC: the day the Play shots are "taken". The iOS capture seeds relative to now.
internal const val TODAY_MILLIS = 1_792_713_600_000L
internal const val DAY_MILLIS = 86_400_000L
internal const val MINUTE_MILLIS = 60_000L

private val avatarColors =
    listOf(AvatarColor.BLUE, AvatarColor.PEACH, AvatarColor.GREEN, AvatarColor.LILAC, AvatarColor.SAND)

/** Member ids are the people's indexes, as in SampleTrip's items. */
internal fun memberIdOf(person: Int): String = person.toString()

internal fun avatarColorOf(person: Int): AvatarColor = avatarColors[person % avatarColors.size]

internal fun SampleTrip.faceOf(person: Int): GroupFace = GroupFace(
    initials = previewInitialsOf(people[person]),
    color = avatarColorOf(person),
    photoUrl = photoUrlOf(person),
)

internal fun SampleTrip.initialsOf(person: Int): String = previewInitialsOf(people[person])

internal fun categoryOf(key: String): Category = CategoryTree.appOnly.resolve(key)

internal fun startOfDayOf(daysAgo: Int): Long = TODAY_MILLIS - daysAgo * DAY_MILLIS

internal fun momentOf(
    daysAgo: Int,
    minuteOfDay: Int,
): Long = startOfDayOf(daysAgo) + minuteOfDay * MINUTE_MILLIS

internal fun relativeDayOf(daysAgo: Int): RelativeDay = when (daysAgo) {
    0 -> RelativeDay.TODAY
    1 -> RelativeDay.YESTERDAY
    else -> RelativeDay.OTHER
}

/** Each person's part of [bill], by index: the items' totals for a bill split by items, an equal fifth otherwise. */
internal fun SampleTrip.sharesOf(bill: SampleBill): Map<Int, Long> {
    val items = bill.items
    if (items != null) {
        val payerIds = bill.payers.map { memberIdOf(it.person) }
        return BillItemsSplit
            .breakdownOf(items, payerIds)
            .people
            .associate { it.memberId.toInt() to it.totalMinor }
    }
    val base = bill.totalMinor / people.size
    val leftover = bill.totalMinor - base * people.size
    return people.indices.associateWith { if (it < leftover) base + 1 else base }
}

/** What [person] is owed (positive) or owes (negative) for [bill]. */
internal fun SampleTrip.balanceOf(
    bill: SampleBill,
    person: Int,
): Long {
    val paid = bill.payers.filter { it.person == person }.sumOf { it.amountMinor }
    return paid - (sharesOf(bill)[person] ?: 0L)
}

/** Everyone's net in the group's currency, the payment included. */
internal val SampleTrip.netBalances: Map<Int, Long>
    get() = people.indices.associateWith { person ->
        val bills = bills.filter { it.currency == currency }.sumOf { balanceOf(it, person) }
        val payment = when (person) {
            payment.fromPerson -> payment.amountMinor
            payment.toPerson -> -payment.amountMinor
            else -> 0L
        }
        bills + payment
    }

internal val SampleTrip.totalSpentMinor: Long
    get() = bills.filter { it.currency == currency }.sumOf { it.totalMinor }

/** The fewest payments that settle the group's currency: the greatest debtor pays the greatest creditor. */
internal val SampleTrip.debts: List<Debt>
    get() {
        val nets = netBalances.toMutableMap()
        val debts = mutableListOf<Debt>()
        while (true) {
            val debtor = nets.minBy { it.value }
            val creditor = nets.maxBy { it.value }
            if (debtor.value >= 0 || creditor.value <= 0) break
            val amount = minOf(-debtor.value, creditor.value)
            debts +=
                Debt(
                    fromMemberId = memberIdOf(debtor.key),
                    toMemberId = memberIdOf(creditor.key),
                    amountMinor = amount,
                )
            nets[debtor.key] = debtor.value + amount
            nets[creditor.key] = creditor.value - amount
        }
        return debts.sortedByDescending { it.amountMinor }
    }

internal val SampleTrip.me: Int
    get() = SampleTrips.ME.toInt()

/** A sample person's face: an AI-generated portrait (scripts/store-screenshots/faces), which [SampleFacesComponent] loads. */
internal fun photoUrlOf(person: Int): String = "$FACE_SCHEME$person"

internal const val FACE_SCHEME = "face://person"
