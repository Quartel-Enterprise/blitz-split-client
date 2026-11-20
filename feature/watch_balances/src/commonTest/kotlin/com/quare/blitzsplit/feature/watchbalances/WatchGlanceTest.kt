package com.quare.blitzsplit.feature.watchbalances

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.watchbalances.presentation.mapper.WatchGlance
import com.quare.blitzsplit.feature.watchbalances.presentation.model.WatchTotal
import kotlin.test.Test
import kotlin.test.assertEquals

class WatchGlanceTest {
    @Test
    fun `GIVEN no profile on the watch WHEN choosing the glance THEN asks to connect`() {
        // Given
        val hasProfile = false

        // When
        val glance = WatchGlance.of(
            hasProfile = hasProfile,
            balances = balances(
                totals = listOf(
                    total(
                        GroupCurrency.BRL,
                        toReceiveMinor = 5_000,
                        toPayMinor = 0,
                    ),
                ),
            ),
        )

        // Then
        assertEquals(WatchGlance.NotConnected, glance)
    }

    @Test
    fun `GIVEN a settled currency first WHEN choosing the glance THEN shows the first open total`() {
        // Given
        val balances = balances(
            totals = listOf(
                total(
                    GroupCurrency.BRL,
                    toReceiveMinor = 0,
                    toPayMinor = 0,
                ),
                total(
                    GroupCurrency.USD,
                    toReceiveMinor = 0,
                    toPayMinor = 1_200,
                ),
            ),
        )

        // When
        val glance = WatchGlance.of(
            hasProfile = true,
            balances = balances,
        )

        // Then
        assertEquals(
            WatchGlance.Open(
                WatchTotal(
                    GroupCurrency.USD,
                    netMinor = -1_200,
                    toReceiveMinor = 0,
                    toPayMinor = 1_200,
                ),
            ),
            glance,
        )
    }
}
