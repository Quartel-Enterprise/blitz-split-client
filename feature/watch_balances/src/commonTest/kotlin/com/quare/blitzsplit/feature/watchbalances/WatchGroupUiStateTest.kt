package com.quare.blitzsplit.feature.watchbalances

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchDebt
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchGroupUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WatchGroupUiStateTest {
    private val carla = debt("carla", -9_800L)
    private val ana = debt("ana", 2_000L)

    @Test
    fun `GIVEN a single pair WHEN the group draws THEN the card names it and no row repeats it`() {
        // Given
        val state = stateWith(carla)

        // When
        val card = state.cardDebt
        val rows = state.rowDebts

        // Then
        assertEquals(carla, card)
        assertEquals(emptyList(), rows)
    }

    @Test
    fun `GIVEN two pairs WHEN the group draws THEN the card names nobody and each pair is a row`() {
        // Given
        val state = stateWith(carla, ana)

        // When
        val card = state.cardDebt
        val rows = state.rowDebts

        // Then
        assertNull(card)
        assertEquals(listOf(carla, ana), rows)
    }

    private fun debt(
        memberId: String,
        amountMinor: Long,
    ): WatchDebt = WatchDebt(
        memberId = memberId,
        name = memberId,
        initials = memberId.take(1).uppercase(),
        color = null,
        photoUrl = null,
        amountMinor = amountMinor,
    )

    private fun stateWith(vararg debts: WatchDebt): WatchGroupUiState = WatchGroupUiState(
        isLoaded = true,
        groupName = "Apê",
        currency = GroupCurrency.BRL,
        netMinor = debts.sumOf { it.amountMinor },
        debts = debts.toList(),
        confirming = null,
        isRecording = false,
        isQueued = false,
    )
}
