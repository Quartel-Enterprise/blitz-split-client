package com.quare.blitzsplit.android.storescreenshots

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.previewInitialsOf
import com.quare.blitzsplit.android.feature.groups.HomeContent
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.model.PayerOrder
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.FaceStack
import com.quare.blitzsplit.core.model.toStackedFaces
import com.quare.blitzsplit.feature.groups.presentation.model.ActivityAge
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.HomeBill
import com.quare.blitzsplit.feature.groups.presentation.model.HomeBillDay
import com.quare.blitzsplit.feature.groups.presentation.model.HomeGroup
import com.quare.blitzsplit.feature.groups.presentation.model.HomePerson
import com.quare.blitzsplit.feature.groups.presentation.model.HomeSummary
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState
import com.quare.blitzsplit.feature.groups.presentation.model.LastActivity
import com.quare.blitzsplit.feature.groups.presentation.model.MemberFace
import com.quare.blitzsplit.feature.groups.presentation.model.SearchBillPayer
import com.quare.blitzsplit.shared.storescreenshots.SampleSideGroup
import com.quare.blitzsplit.shared.storescreenshots.SampleTrip
import com.quare.blitzsplit.shared.storescreenshots.SampleTrips
import software.quare.loadable.toLoaded
import kotlin.math.abs

private const val UNREAD_ACTIVITY = 3
private const val MINUTES_PER_DAY = 1_440
private const val MINUTES_NOW = 13 * 60
private const val MINUTES_SINCE_PAYMENT = 3 * 60

/** Início with the trip and the two other groups pinned, the people who owe or are owed, and yesterday's bills. */
@Composable
internal fun HomeScreenshotContent(trip: SampleTrip) {
    HomeContent(
        state = trip.homeState(),
        onEvent = {},
    )
}

private fun SampleTrip.homeState(): HomeUiState {
    val nets = peopleNets()
    val owed = nets.values.filter { it > 0 }
    val owing = nets.values.filter { it < 0 }
    val tripNet = netBalances.getValue(me)
    val groups = listOf(
        groupRow(
            id = "trip",
            name = groupName,
            color = tripColor,
            members = people.indices.toList(),
            netMinor = tripNet,
            minutesAgo = MINUTES_SINCE_PAYMENT,
        ),
    ) + sideGroups.mapIndexed { index, group ->
        groupRow(
            id = "side-$index",
            name = group.name,
            color = SampleTrips.sideGroupColors[index % SampleTrips.sideGroupColors.size],
            members = group.people,
            netMinor = myNetOf(group),
            minutesAgo = group.daysAgo * MINUTES_PER_DAY,
        )
    }
    val yesterday = bills.filter { it.daysAgo == 1 }.sortedByDescending { it.minuteOfDay }
    // Why: whoever paid, in the app's order: me first, then the group's order.
    val payerOrder = PayerOrder(
        meId = memberIdOf(me),
        memberIds = people.indices.map(::memberIdOf),
    )
    return HomeUiState(
        displayName = people[me],
        avatarInitial = initialsOf(me),
        avatarColor = avatarColorOf(me),
        photoUrl = photoUrlOf(me),
        accountProvider = null,
        syncBadge = null,
        pendingChangeCount = 0,
        firstDownload = null,
        isOnline = true,
        isLoaded = true,
        groupCount = groups.size,
        people = nets.entries
            .filter { it.value != 0L }
            .sortedByDescending { abs(it.value) }
            .map { (person, net) ->
                HomePerson(
                    key = memberIdOf(person),
                    name = people[person],
                    initials = initialsOf(person),
                    color = avatarColorOf(person),
                    photoUrl = photoUrlOf(person),
                    currency = currency,
                    netMinor = net,
                    isGuest = false,
                )
            }.toLoaded(),
        summary = HomeSummary(
            totals = listOf(
                PeopleTotal(
                    currency = currency,
                    netMinor = owed.sum() + owing.sum(),
                    toReceiveMinor = owed.sum(),
                    toPayMinor = -owing.sum(),
                    toReceiveCount = owed.size,
                    toPayCount = owing.size,
                ),
            ),
        ).toLoaded(),
        groups = groups.map {
            HomeGroup(
                row = it,
                isPinned = true,
            )
        },
        billDays = listOf(
            HomeBillDay(
                dayStartMillis = startOfDayOf(1),
                relativeDay = relativeDayOf(1),
                bills = yesterday.mapIndexed { index, bill ->
                    val payers = payerOrder.sort(bill.payers.map { memberIdOf(it.person) }).map(String::toInt)
                    HomeBill(
                        id = "home-bill-$index",
                        groupId = "trip",
                        groupName = groupName,
                        groupInitials = previewInitialsOf(groupName),
                        groupColor = tripColor,
                        groupPhotoUrl = null,
                        description = bill.description,
                        totalMinor = bill.totalMinor,
                        currency = bill.currency,
                        payers = payers.map { SearchBillPayer(people[it], isMe = it == me) },
                        payerFaces = payers.toStackedFaces().map(this::memberFaceOf),
                        extraPayerCount = FaceStack.extraCountOf(payers.size),
                        myBalanceMinor = balanceOf(bill, me),
                        category = categoryOf(bill.categoryKey),
                        isPending = false,
                    )
                },
            ),
        ),
        showsRecoveryCodeCard = false,
        showsRecoveryCodePrompt = false,
        recordedPayment = null,
        deletedGroupNotice = null,
        isRefreshing = false,
        activityUnreadCount = UNREAD_ACTIVITY,
        notificationPermission = null,
        canAskQuietly = false,
        hasAskedQuietly = false,
        isNotificationPromptSnoozed = true,
        appInvite = null,
        hasBills = true,
        isAppBannerDismissed = true,
        showsReceiptSourceChoice = false,
        receiptSource = null,
        billToDelete = null,
        isDeletingBill = false,
    )
}

private fun SampleTrip.groupRow(
    id: String,
    name: String,
    color: AvatarColor,
    members: List<Int>,
    netMinor: Long,
    minutesAgo: Int,
): GroupRow = GroupRow(
    id = id,
    name = name,
    initials = previewInitialsOf(name),
    color = color,
    photoUrl = null,
    faces = members.toStackedFaces().map(this::memberFaceOf),
    extraMemberCount = FaceStack.extraCountOf(members.size),
    memberCount = members.size,
    memberNames = members.map { people[it] },
    isPending = false,
    pendingBillDescription = null,
    currency = currency,
    balanceKind = when {
        netMinor > 0 -> GroupBalanceKind.TO_RECEIVE
        netMinor < 0 -> GroupBalanceKind.YOU_OWE
        else -> GroupBalanceKind.YOU_ARE_SETTLED
    },
    netMinor = netMinor,
    receivesMinor = netMinor.coerceAtLeast(0),
    owesMinor = (-netMinor).coerceAtLeast(0),
    lastActivity = LastActivity(
        epochMillis = TODAY_MILLIS + MINUTES_NOW * MINUTE_MILLIS - minutesAgo * MINUTE_MILLIS,
        age = if (minutesAgo < MINUTES_PER_DAY) ActivityAge.TODAY else ActivityAge.DAYS,
        count = 1,
    ),
)

// Why: one letter in a face stack's bubble, as the app's MemberFace draws it.
private fun SampleTrip.memberFaceOf(person: Int): MemberFace = MemberFace(
    initial = Initials.firstOf(people[person]),
    color = avatarColorOf(person),
    photoUrl = photoUrlOf(person),
)

/** What a side group's one bill leaves me with: the payer gets the others' parts back. */
private fun SampleTrip.myNetOf(group: SampleSideGroup): Long {
    val share = group.billTotalMinor / group.people.size
    return if (group.billPayer == me) group.billTotalMinor - share else -share
}

/** Each other person's net with me across the trip's debts and the side groups: positive when they owe me. */
private fun SampleTrip.peopleNets(): Map<Int, Long> {
    val nets = people.indices
        .filter { it != me }
        .associateWith { 0L }
        .toMutableMap()
    debts.forEach { debt ->
        val from = debt.fromMemberId.toInt()
        val to = debt.toMemberId.toInt()
        if (to == me) nets[from] = nets.getValue(from) + debt.amountMinor
        if (from == me) nets[to] = nets.getValue(to) - debt.amountMinor
    }
    sideGroups.forEach { group ->
        val share = group.billTotalMinor / group.people.size
        if (group.billPayer == me) {
            group.people.filter { it != me }.forEach { nets[it] = nets.getValue(it) + share }
        } else {
            nets[group.billPayer] = nets.getValue(group.billPayer) - share
        }
    }
    return nets
}
