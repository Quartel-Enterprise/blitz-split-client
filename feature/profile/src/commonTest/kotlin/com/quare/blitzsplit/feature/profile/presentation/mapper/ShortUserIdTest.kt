package com.quare.blitzsplit.feature.profile.presentation.mapper

import kotlin.test.Test
import kotlin.test.assertEquals

class ShortUserIdTest {
    @Test
    fun `GIVEN a server user id WHEN shortening it THEN keeps its first eight and last four characters`() {
        // Given
        val userId = "3f8c2a91-6b4e-4d2a-9c7f-1e5b8a0d4f27"

        // When
        val short = userId.toShortUserId()

        // Then
        assertEquals("3f8c2a91…4f27", short)
    }

    @Test
    fun `GIVEN an id of thirteen characters WHEN shortening it THEN shows it whole`() {
        // Given
        val userId = "abcdefghijklm"

        // When
        val short = userId.toShortUserId()

        // Then
        assertEquals("abcdefghijklm", short)
    }

    @Test
    fun `GIVEN an id of fourteen characters WHEN shortening it THEN shortens it`() {
        // Given
        val userId = "abcdefghijklmn"

        // When
        val short = userId.toShortUserId()

        // Then
        assertEquals("abcdefgh…klmn", short)
    }

    @Test
    fun `GIVEN a short id WHEN shortening it THEN shows it whole`() {
        // Given
        val userId = "user-1"

        // When
        val short = userId.toShortUserId()

        // Then
        assertEquals("user-1", short)
    }
}
