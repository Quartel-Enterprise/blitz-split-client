package com.quare.blitzsplit.feature.groups

import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.groups.presentation.model.HomePerson
import com.quare.blitzsplit.feature.groups.presentation.model.HomeSummary
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState
import com.quare.blitzsplit.feature.groups.presentation.model.SyncBadge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeUiStateTest {
    @Test
    fun `GIVEN the groups not read yet WHEN Inicio draws THEN shows neither the home the first use nor Rachar conta`() {
        // Given
        val state = state(isLoaded = false)

        // When
        val shown = listOf(state.showsHome, state.showsFirstUse, state.showsSplitBillButton)

        // Then
        assertEquals(listOf(false, false, false), shown)
    }

    @Test
    fun `GIVEN no group read from the device WHEN Inicio draws THEN shows only the first use`() {
        // Given
        val state = state(groupCount = 0)

        // When
        val shown = listOf(state.showsHome, state.showsFirstUse, state.showsSplitBillButton)

        // Then
        assertEquals(listOf(false, true, false), shown)
    }

    @Test
    fun `GIVEN a first download before the groups are read WHEN Inicio draws THEN shows the home not the first use`() {
        // Given
        val state = state(
            isLoaded = false,
            isFirstDownload = true,
        )

        // When
        val shown = listOf(state.showsHome, state.showsFirstUse, state.showsSplitBillButton)

        // Then
        assertEquals(listOf(true, false, false), shown)
    }

    @Test
    fun `GIVEN a first download with no group yet WHEN Inicio draws THEN shows the home without the first use`() {
        // Given
        val state = state(
            groupCount = 0,
            isFirstDownload = true,
        )

        // When
        val shown = listOf(state.showsHome, state.showsFirstUse, state.showsSplitBillButton)

        // Then
        assertEquals(listOf(true, false, false), shown)
    }

    @Test
    fun `GIVEN a group on the device WHEN Inicio draws THEN shows the home and Rachar conta`() {
        // Given
        val state = state(groupCount = 1)

        // When
        val shown = listOf(state.showsHome, state.showsFirstUse, state.showsSplitBillButton)

        // Then
        assertEquals(listOf(true, false, true), shown)
    }

    @Test
    fun `GIVEN everyone settled WHEN Inicio draws THEN hides the faces`() {
        // Given
        val state = state(people = emptyList())

        // When
        val showsPeople = state.showsPeople

        // Then
        assertFalse(showsPeople)
    }

    @Test
    fun `GIVEN someone with an open balance WHEN Inicio draws THEN shows the faces`() {
        // Given
        val state = state(
            people = listOf(
                HomePerson(
                    key = "name:ana",
                    name = "Ana",
                    initials = "AN",
                    color = AvatarColor.PEACH,
                    currency = GroupCurrency.BRL,
                    netMinor = -3000L,
                    isGuest = false,
                ),
            ),
        )

        // When
        val showsPeople = state.showsPeople

        // Then
        assertTrue(showsPeople)
    }

    @Test
    fun `GIVEN no internet WHEN Inicio draws THEN Ler cupom fiscal is disabled`() {
        // Given
        val state = state(isOnline = false)

        // When
        val canScanReceipt = state.canScanReceipt

        // Then
        assertFalse(canScanReceipt)
    }

    @Test
    fun `GIVEN internet WHEN Inicio draws THEN Ler cupom fiscal is enabled`() {
        // Given
        val state = state(isOnline = true)

        // When
        val canScanReceipt = state.canScanReceipt

        // Then
        assertTrue(canScanReceipt)
    }

    @Test
    fun `GIVEN each sync badge WHEN Inicio draws THEN only the failed one can be retried`() {
        // Given
        val badges = listOf(null, SyncBadge.OFFLINE, SyncBadge.SYNCING, SyncBadge.FAILED)

        // When
        val retryable = badges.map { state(syncBadge = it).canRetrySync }

        // Then
        assertEquals(listOf(false, false, false, true), retryable)
    }

    @Test
    fun `GIVEN one person you owe and one who owes you WHEN Inicio draws THEN Voce deve settles up with that one`() {
        // Given
        val bruno = person(
            key = "name:bruno",
            netMinor = -12_600L,
        )
        val state = state(
            people = listOf(
                bruno,
                person(
                    key = "name:diego",
                    netMinor = 4500L,
                ),
            ),
        )

        // When
        val onlyPersonToPay = state.onlyPersonToPay

        // Then
        assertEquals(bruno, onlyPersonToPay)
    }

    @Test
    fun `GIVEN two people you owe WHEN Inicio draws THEN Voce deve opens Saldos instead`() {
        // Given
        val state = state(
            people = listOf(
                person(
                    key = "name:bruno",
                    netMinor = -12_600L,
                ),
                person(
                    key = "name:carla",
                    netMinor = -3000L,
                ),
            ),
        )

        // When
        val onlyPersonToPay = state.onlyPersonToPay

        // Then
        assertNull(onlyPersonToPay)
    }

    private fun person(
        key: String,
        netMinor: Long,
    ): HomePerson = HomePerson(
        key = key,
        name = key.removePrefix("name:"),
        initials = key.removePrefix("name:").take(2).uppercase(),
        color = AvatarColor.PEACH,
        currency = GroupCurrency.BRL,
        netMinor = netMinor,
        isGuest = false,
    )

    private fun state(
        isLoaded: Boolean = true,
        groupCount: Int = 1,
        isFirstDownload: Boolean = false,
        isOnline: Boolean = true,
        syncBadge: SyncBadge? = null,
        people: List<HomePerson> = emptyList(),
    ): HomeUiState = HomeUiState(
        displayName = "Pierre",
        avatarInitial = "P",
        avatarColor = AvatarColor.GREEN,
        photoUrl = null,
        accountProvider = null,
        syncBadge = syncBadge,
        pendingChangeCount = 0,
        isFirstDownload = isFirstDownload,
        isOnline = isOnline,
        isLoaded = isLoaded,
        groupCount = groupCount,
        people = people,
        summary = HomeSummary(totals = emptyList()),
        groups = emptyList(),
        billDays = emptyList(),
        showsRecoveryCodeCard = false,
        showsRecoveryCodePrompt = false,
        recordedPayment = null,
        deletedGroupNotice = null,
        isRefreshing = false,
        activityUnreadCount = 0,
    )
}
