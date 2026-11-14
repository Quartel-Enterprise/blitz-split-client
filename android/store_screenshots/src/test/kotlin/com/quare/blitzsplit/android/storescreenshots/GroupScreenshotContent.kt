package com.quare.blitzsplit.android.storescreenshots

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.quare.blitzsplit.android.designsystem.layout.previewInitialsOf
import com.quare.blitzsplit.android.feature.groupdetail.GroupDetailContent
import com.quare.blitzsplit.core.expenses.domain.model.DebtKind
import com.quare.blitzsplit.core.expenses.domain.model.DebtRow
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.model.toStackedFaces
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRowPayer
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiState
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupHeader
import com.quare.blitzsplit.feature.groupdetail.presentation.model.PaymentRow
import com.quare.blitzsplit.shared.storescreenshots.SampleTrip

@Composable
internal fun GroupScreenshotContent(trip: SampleTrip) {
    GroupDetailContent(
        state = trip.groupDetailState(GroupTab.EXPENSES),
        snackbarHostState = remember { SnackbarHostState() },
        membersTab = {},
        onEvent = {},
    )
}

@Composable
internal fun BalancesScreenshotContent(trip: SampleTrip) {
    GroupDetailContent(
        state = trip.groupDetailState(GroupTab.BALANCES),
        snackbarHostState = remember { SnackbarHostState() },
        membersTab = {},
        onEvent = {},
    )
}

internal fun SampleTrip.groupHeader(): GroupHeader {
    val myId = memberIdOf(me)
    return GroupHeader(
        groupName = groupName,
        groupInitials = previewInitialsOf(groupName),
        groupColor = tripColor,
        groupPhotoUrl = null,
        currency = currency,
        currencies = listOf(currency, foreignCurrency),
        faces = people.indices.toList().toStackedFaces().map { faceOf(it) },
        memberCount = people.size,
        notJoinedCount = 0,
        totalSpentMinor = totalSpentMinor,
        myBalance = MemberBalance(
            receivesMinor = debts.filter { it.toMemberId == myId }.sumOf { it.amountMinor },
            owesMinor = debts.filter { it.fromMemberId == myId }.sumOf { it.amountMinor },
        ),
        isEveryoneSettled = false,
        isPinned = true,
    )
}

internal fun SampleTrip.groupDetailState(tab: GroupTab): GroupDetailUiState {
    val debtRows = debts.map { debt ->
        val from = debt.fromMemberId.toInt()
        val to = debt.toMemberId.toInt()
        DebtRow(
            fromMemberId = debt.fromMemberId,
            toMemberId = debt.toMemberId,
            fromName = people[from],
            fromFace = faceOf(from),
            toName = people[to],
            toFace = faceOf(to),
            amountMinor = debt.amountMinor,
            currency = currency,
            kind = when (me) {
                from -> DebtKind.YOU_OWE
                to -> DebtKind.OWES_YOU
                else -> DebtKind.BETWEEN_OTHERS
            },
            isSimplified = false,
        )
    }
    return GroupDetailUiState(
        header = groupHeader(),
        tab = tab,
        days = expenseDays(),
        paymentToView = null,
        paymentToDelete = null,
        billToDelete = null,
        isDeletingBill = false,
        myDebts = debtRows.filter { it.kind != DebtKind.BETWEEN_OTHERS },
        otherDebts = debtRows.filter { it.kind == DebtKind.BETWEEN_OTHERS },
        hasBills = true,
        recordedPayment = null,
        showsBillDeletedNotice = false,
        exit = GroupExitState(
            isOnline = true,
            dialog = null,
            isRunning = false,
            deletedNotice = null,
        ),
        isLoaded = true,
        awaitsFullSync = false,
        isWaitingForInternet = false,
        teach = null,
        homeTab = HomeTab.GROUPS,
        joinedAs = null,
        isAccountUnprotected = false,
        simplification = null,
        simplifyConflict = null,
        showsExportFormats = false,
    )
}

private fun SampleTrip.expenseDays(): List<ExpenseDay> {
    val billEntries = bills.mapIndexed { index, bill ->
        bill.daysAgo to ExpenseRow(
            id = "bill-$index",
            description = bill.description,
            spentAt = momentOf(bill.daysAgo, bill.minuteOfDay),
            payers = bill.payers.map {
                ExpenseRowPayer(
                    name = people[it.person],
                    isMe = it.person == me,
                )
            },
            totalMinor = bill.totalMinor,
            currency = bill.currency,
            myBalanceMinor = balanceOf(bill, me),
            category = categoryOf(bill.categoryKey),
            isPending = false,
        )
    }
    val paymentEntry = payment.daysAgo to PaymentRow(
        id = "payment",
        paidAt = momentOf(payment.daysAgo, payment.minuteOfDay),
        paidDay = relativeDayOf(payment.daysAgo),
        fromName = people[payment.fromPerson],
        fromIsMe = payment.fromPerson == me,
        toName = people[payment.toPerson],
        toIsMe = payment.toPerson == me,
        amountMinor = payment.amountMinor,
        currency = currency,
        method = payment.method,
        isPending = false,
    )
    return (billEntries + paymentEntry)
        .groupBy({ it.first }, { it.second })
        .toSortedMap()
        .map { (daysAgo, entries) ->
            ExpenseDay(
                dayStartMillis = startOfDayOf(daysAgo),
                relativeDay = relativeDayOf(daysAgo),
                entries = entries.sortedByDescending { entry ->
                    when (entry) {
                        is ExpenseRow -> entry.spentAt
                        is PaymentRow -> entry.paidAt
                        else -> 0L
                    }
                },
            )
        }
}
