package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayLinkTest {
    @Test
    fun `GIVEN a link with its language WHEN printing it THEN shows it without scheme nor query`() {
        // Given
        val link = "https://blitzsplit.com/app-download?lang=pt"

        // When
        val display = DisplayLink.of(link)

        // Then
        assertEquals("blitzsplit.com/app-download", display)
    }
}
