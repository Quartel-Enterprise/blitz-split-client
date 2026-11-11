package com.quare.blitzsplit.android.storescreenshots

import com.quare.blitzsplit.android.designsystem.layout.previewInitialsOf
import com.quare.blitzsplit.core.expenses.domain.model.BillItemsSplit
import com.quare.blitzsplit.core.expenses.domain.model.BillSplit
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.Debt
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.expenses.domain.model.GroupFace
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.shared.storescreenshots.SampleBill
import com.quare.blitzsplit.shared.storescreenshots.SampleTrip
import com.quare.blitzsplit.shared.storescreenshots.SampleTrips

// Friday 23 October 2026, 00:00 UTC: the day the Play shots are "taken". The iOS capture seeds relative to now.
internal const val TODAY_MILLIS = 1_792_713_600_000L
internal const val DAY_MILLIS = 86_400_000L
internal const val MINUTE_MILLIS = 60_000L

/** Member ids are the people's indexes, as in SampleTrip's items. */
internal fun memberIdOf(person: Int): String = person.toString()

internal fun avatarColorOf(person: Int): AvatarColor = SampleTrips.avatarColorOf(person)

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

/**
 * Each person's part of [bill], by index: the items' totals for a bill split by items, an equal fifth otherwise, its
 * cents left over handed out as the app's own split does.
 */
internal fun SampleTrip.sharesOf(bill: SampleBill): Map<Int, Long> {
    val items = bill.items
    val payerIds = bill.payers.map { memberIdOf(it.person) }
    if (items != null) {
        return BillItemsSplit
            .breakdownOf(items, payerIds)
            .people
            .associate { it.memberId.toInt() to it.totalMinor }
    }
    val shares = BillSplit.split(
        totalMinor = bill.totalMinor,
        mode = SplitMode.EQUAL,
        entries = people.indices.map {
            SplitEntry(
                memberId = memberIdOf(it),
                value = 1,
            )
        },
        payerIds = payerIds,
    )
    return checkNotNull(shares) { "${bill.description} doesn't split" }.associate {
        it.memberId.toInt() to it.amountMinor
    }
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

/**
 * Who owes whom in the group's currency, bill by bill, as Balances shows a group with "Simplify debts" off, the way
 * the iOS seed creates it: the app's own GroupBalances works it out from the trip's bills and payment.
 */
internal val SampleTrip.debts: List<Debt>
    get() {
        val expenses = bills.mapIndexed { index, bill ->
            Expense(
                id = "bill-$index",
                groupId = "trip",
                description = bill.description,
                amountMinor = bill.totalMinor,
                currency = bill.currency,
                splitMode = if (bill.items == null) SplitMode.EQUAL else SplitMode.ITEMS,
                payers = bill.payers.map { ExpensePayer(memberIdOf(it.person), it.amountMinor) },
                shares = sharesOf(bill).map { (person, amount) -> ExpenseShare(memberIdOf(person), amount, amount) },
                items = bill.items,
                categoryId = bill.categoryKey,
                spentAt = momentOf(bill.daysAgo, bill.minuteOfDay),
                isPending = false,
                updatedAt = momentOf(bill.daysAgo, bill.minuteOfDay),
                updatedByMemberId = null,
            )
        }
        val settlement = Settlement(
            id = "payment",
            groupId = "trip",
            fromMemberId = memberIdOf(payment.fromPerson),
            toMemberId = memberIdOf(payment.toPerson),
            amountMinor = payment.amountMinor,
            currency = currency,
            method = payment.method,
            paidAt = momentOf(payment.daysAgo, payment.minuteOfDay),
            isPending = false,
            updatedAt = momentOf(payment.daysAgo, payment.minuteOfDay),
        )
        return GroupBalances.createUnsimplified(expenses, listOf(settlement), currency).debts
    }

internal val SampleTrip.me: Int
    get() = SampleTrips.ME.toInt()

/** A sample person's face: an AI-generated portrait (scripts/store-screenshots/faces), which [SampleFacesComponent] loads. */
internal fun photoUrlOf(person: Int): String = "$FACE_SCHEME$person"

internal const val FACE_SCHEME = "face://person"
