package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupNameRulesTest {
    @Test
    fun `GIVEN a name of plain letters and spaces WHEN counting its length THEN counts each one`() {
        // Given
        val name = "Viagem Floripa"

        // When
        val length = GroupNameRules.lengthOf(name)

        // Then
        assertEquals(14, length)
    }

    @Test
    fun `GIVEN an empty name WHEN counting its length THEN is zero`() {
        // Given
        val name = ""

        // When
        val length = GroupNameRules.lengthOf(name)

        // Then
        assertEquals(0, length)
    }

    @Test
    fun `GIVEN a name with an emoji WHEN counting its length THEN the emoji counts as one letter`() {
        // Given
        val name = "Praia 🌴"

        // When
        val length = GroupNameRules.lengthOf(name)

        // Then
        assertEquals(7, length)
    }

    @Test
    fun `GIVEN a name of emojis over the limit WHEN counting the capped name THEN is exactly the limit`() {
        // Given
        val capped = GroupNameRules.toCapped("🌴".repeat(45))

        // When
        val length = GroupNameRules.lengthOf(capped)

        // Then
        assertEquals(GroupNameRules.MAX_LENGTH, length)
    }

    @Test
    fun `GIVEN a name of exactly 40 letters WHEN counting its length THEN reaches the limit and is within it`() {
        // Given
        val name = "A".repeat(39) + "🌴"

        // When
        val length = GroupNameRules.lengthOf(name)

        // Then
        assertEquals(GroupNameRules.MAX_LENGTH, length)
        assertTrue(GroupNameRules.isWithinLimit(name))
    }
}
