package com.quare.blitzsplit.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class GroupTypeTest {
    @Test
    fun `GIVEN a new group WHEN reading the default type THEN it is Event`() {
        // Given
        val expected = GroupType.EVENT

        // When
        val default = GroupType.default

        // Then
        assertEquals(expected, default)
    }

    @Test
    fun `GIVEN the type pickers WHEN listing the types THEN Event comes first and Other last`() {
        // Given
        val expected = listOf(GroupType.EVENT, GroupType.TRIP, GroupType.HOME, GroupType.OTHER)

        // When
        val types = GroupType.entries

        // Then
        assertEquals(expected, types)
    }

    @Test
    fun `GIVEN every type WHEN reading its stored code THEN each keeps the code it was saved with`() {
        // Given
        val expected = listOf("event", "trip", "home", "other")

        // When
        val codes = GroupType.entries.map { it.code }

        // Then
        assertEquals(expected, codes)
    }
}
