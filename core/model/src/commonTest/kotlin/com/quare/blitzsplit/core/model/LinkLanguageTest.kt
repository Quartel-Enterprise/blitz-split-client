package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class LinkLanguageTest {
    @Test
    fun `GIVEN a link without a query WHEN adding the language THEN it is the first parameter of the link`() {
        // Given
        val link = "https://blitzsplit.com/u/7Hk2pQ8xZa"

        // When
        val result = LinkLanguage.buildLink(link, "pt")

        // Then
        assertEquals("https://blitzsplit.com/u/7Hk2pQ8xZa?lang=pt", result)
    }

    @Test
    fun `GIVEN a link with a query WHEN adding the language THEN it comes after what the link already says`() {
        // Given
        val link = "https://blitzsplit.com/b/bill?comentario=comment"

        // When
        val result = LinkLanguage.buildLink(link, "es")

        // Then
        assertEquals("https://blitzsplit.com/b/bill?comentario=comment&lang=es", result)
    }
}
