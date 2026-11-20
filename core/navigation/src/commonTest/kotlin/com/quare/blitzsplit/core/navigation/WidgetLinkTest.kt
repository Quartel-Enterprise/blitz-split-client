package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.HomeTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WidgetLinkTest {
    @Test
    fun `GIVEN the balance widget link WHEN reading it THEN opens the Balances tab`() {
        // Given
        val url = "borarachar://balances/"

        // When
        val tab = WidgetLink.homeTabOf(url)

        // Then
        assertEquals(HomeTab.BALANCES, tab)
    }

    @Test
    fun `GIVEN another app link WHEN reading it THEN opens nothing`() {
        // Given
        val url = "borarachar://auth-callback#access_token=abc"

        // When
        val tab = WidgetLink.homeTabOf(url)

        // Then
        assertNull(tab)
    }
}
