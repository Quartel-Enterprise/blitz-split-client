package com.quare.blitzsplit.core.groups.domain

import com.quare.blitzsplit.core.groups.domain.model.MemberColors
import com.quare.blitzsplit.core.model.AvatarColor
import kotlin.test.Test
import kotlin.test.assertEquals

class MemberColorsTest {
    @Test
    fun `GIVEN nobody yet WHEN picking the least used color THEN picks the first of the palette`() {
        // Given
        val colors = emptyList<AvatarColor>()

        // When
        val picked = MemberColors.findLeastUsed(colors)

        // Then
        assertEquals(AvatarColor.entries.first(), picked)
    }

    @Test
    fun `GIVEN blue and green taken WHEN picking the least used color THEN picks peach the first nobody has`() {
        // Given
        val colors = listOf(AvatarColor.GREEN, AvatarColor.BLUE)

        // When
        val picked = MemberColors.findLeastUsed(colors)

        // Then
        assertEquals(AvatarColor.PEACH, picked)
    }

    @Test
    fun `GIVEN every color taken once and blue twice WHEN picking the least used color THEN picks peach`() {
        // Given
        val colors = AvatarColor.entries + AvatarColor.BLUE

        // When
        val picked = MemberColors.findLeastUsed(colors)

        // Then
        assertEquals(AvatarColor.PEACH, picked)
    }

    @Test
    fun `GIVEN every color taken twice but sand once WHEN picking the least used color THEN picks sand`() {
        // Given
        val colors = AvatarColor.entries + AvatarColor.entries.filterNot { it == AvatarColor.SAND }

        // When
        val picked = MemberColors.findLeastUsed(colors)

        // Then
        assertEquals(AvatarColor.SAND, picked)
    }

    @Test
    fun `GIVEN only the last colors of the palette taken WHEN picking the least used color THEN picks blue`() {
        // Given
        val colors = listOf(AvatarColor.SAND, AvatarColor.LILAC, AvatarColor.SAND)

        // When
        val picked = MemberColors.findLeastUsed(colors)

        // Then
        assertEquals(AvatarColor.BLUE, picked)
    }
}
