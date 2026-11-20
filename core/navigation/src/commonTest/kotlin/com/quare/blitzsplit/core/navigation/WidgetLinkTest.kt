package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.BalancesFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WidgetLinkTest {
    @Test
    fun `GIVEN the balance widget link WHEN reading it THEN opens Balances on All`() {
        // Given
        val url = "borarachar://balances/"

        // When
        val route = WidgetLink.routeOf(url)

        // Then
        assertEquals(Route.GlobalBalances(BalancesFilter.ALL), route)
    }

    @Test
    fun `GIVEN another app link WHEN reading it THEN opens nothing`() {
        // Given
        val url = "borarachar://auth-callback#access_token=abc"

        // When
        val route = WidgetLink.routeOf(url)

        // Then
        assertNull(route)
    }
}
