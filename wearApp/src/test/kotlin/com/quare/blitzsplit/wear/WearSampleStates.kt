package com.quare.blitzsplit.wear

import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchBalancesUiState
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchDebt
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchGroupRow
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchGroupUiState
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchTotal
import com.quare.blitzsplit.shared.storescreenshots.SampleTrip
import com.quare.blitzsplit.shared.storescreenshots.SampleTrips
import kotlin.math.abs
import kotlin.math.roundToLong

/*
 * Why: what the watch's ViewModels would hold for a sample trip (SampleTrips, which the iPhone's captures seed), written
 * by hand since the seed lives in :shared, out of reach of this module: the README's watch (ReadmeHeroWatch, English)
 * and the store listing's (WearStoreScreenshots, every language) draw from the same states, so a change to the trip's
 * amounts is made here once. The English amounts (+$128.08 in all, the soccer's $20.00, the boat tour's EUR 24) are
 * scaled to the language's currency by the ratio of its soccer court to the English one, since the seed's own
 * conversion is private; the boat tour is in the other currency, at the same amount in every language, as the trip has it.
 */
internal object WearSampleStates {
    private const val ROOMMATES = 0
    private const val SOCCER = 1

    // Why: the paying person of Friday soccer, the one you owe.
    fun createSoccerDebt(
        trip: SampleTrip,
        photoOf: (String) -> String? = { null },
    ): WatchDebt {
        val soccer = trip.sideGroups[SOCCER]
        val payer = soccer.billPayer
        return WatchDebt(
            memberId = "person$payer",
            name = trip.people[payer],
            initials = Initials.firstOf(trip.people[payer]),
            color = SampleTrips.avatarColorOf(payer),
            photoUrl = photoOf("faces/person$payer.jpg"),
            // Why: the court is split by the group's people, so your share is a part of it
            amountMinor = -soccer.billTotalMinor / soccer.people.size,
        )
    }

    fun createGroup(
        trip: SampleTrip,
        photoOf: (String) -> String? = { null },
    ): WatchGroupUiState {
        val debt = createSoccerDebt(trip, photoOf)
        return WatchGroupUiState(
            isLoaded = true,
            groupName = trip.sideGroups[SOCCER].name,
            currency = trip.currency,
            netMinor = debt.amountMinor,
            debts = listOf(debt),
            confirming = null,
            isRecording = false,
            isQueued = false,
        )
    }

    fun createBalances(
        trip: SampleTrip,
        photoOf: (String) -> String? = { null },
    ): WatchBalancesUiState {
        val soccer = trip.sideGroups[SOCCER]
        val roommates = trip.sideGroups[ROOMMATES]
        val groups = listOf(
            createGroupRow(
                "1",
                trip.groupName,
                SampleTrips.TRIP_PHOTO,
                AvatarColor.PEACH,
                trip.currency,
                photoOf,
                createScaledAmount(trip, 8_408),
            ),
            createGroupRow(
                "2",
                roommates.name,
                roommates.photo,
                AvatarColor.BLUE,
                trip.currency,
                photoOf,
                createScaledAmount(trip, 6_400),
            ),
            createGroupRow(
                "1",
                trip.groupName,
                SampleTrips.TRIP_PHOTO,
                AvatarColor.PEACH,
                trip.foreignCurrency,
                photoOf,
                -2_400,
            ),
            createGroupRow(
                "3",
                soccer.name,
                soccer.photo,
                AvatarColor.GREEN,
                trip.currency,
                photoOf,
                createSoccerDebt(trip).amountMinor,
            ),
        )
        return WatchBalancesUiState(
            isLoaded = true,
            totals = createTotals(trip),
            // Why: the largest first, as WatchBalancesMapper lists them: in a currency worth less than the dollar the
            // soccer's debt is the larger one
            groups = groups.sortedByDescending { abs(it.netMinor) },
            isOnline = true,
            queuedChangeCount = 0,
            lastSyncedAt = null,
            isConfirmingSignOut = false,
            isSignOutScheduled = false,
        )
    }

    private fun createTotals(trip: SampleTrip): List<WatchTotal> {
        val toReceive = createScaledAmount(trip, 20_581)
        val toPay = createScaledAmount(trip, 7_773)
        return listOf(
            // Why: the net is what the two figures under it leave, whatever the scaling rounded to
            WatchTotal(
                trip.currency,
                netMinor = toReceive - toPay,
                toReceiveMinor = toReceive,
                toPayMinor = toPay,
            ),
            WatchTotal(
                trip.foreignCurrency,
                netMinor = -2_400,
                toReceiveMinor = 0,
                toPayMinor = 2_400,
            ),
        )
    }

    private fun createGroupRow(
        id: String,
        name: String,
        photo: String,
        color: AvatarColor,
        currency: GroupCurrency,
        photoOf: (String) -> String?,
        netMinor: Long,
    ): WatchGroupRow = WatchGroupRow(
        groupId = id,
        name = name,
        initials = Initials.of(name),
        color = color,
        photoUrl = photoOf("groups/$photo.jpg"),
        currency = currency,
        netMinor = netMinor,
        isQueued = false,
    )

    private fun createScaledAmount(
        trip: SampleTrip,
        englishMinor: Long,
    ): Long {
        val english = SampleTrips.tripOf("en").sideGroups[SOCCER].billTotalMinor
        return (englishMinor * trip.sideGroups[SOCCER].billTotalMinor / english.toDouble()).roundToLong()
    }
}
