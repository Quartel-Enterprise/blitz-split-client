package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTint
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.tint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryTintTest {
    @Test
    fun `GIVEN every named key WHEN reading it as a tint THEN gives that named color`() {
        // Given
        val codes = CategoryColor.entries.map { it.code }

        // When
        val tints = codes.map { CategoryTint.fromCodeOrNull(it) }

        // Then
        assertEquals(CategoryColor.entries.map { CategoryTint.Named(it) }, tints)
    }

    @Test
    fun `GIVEN a hex code with a hash in any case WHEN reading it as a tint THEN gives the custom color`() {
        // Given
        val codes = listOf("#3A7BD5", "#3a7bd5")

        // When
        val tints = codes.map { CategoryTint.fromCodeOrNull(it) }

        // Then
        assertEquals(List(2) { CategoryTint.Custom(CustomColor(0x3A7BD5)) }, tints)
    }

    @Test
    fun `GIVEN a hex code without a hash WHEN reading it as a tint THEN gives none`() {
        // Given
        val code = "3A7BD5"

        // When
        val tint = CategoryTint.fromCodeOrNull(code)

        // Then
        assertNull(tint)
    }

    @Test
    fun `GIVEN an unknown code a short hex or no code WHEN reading it as a tint THEN gives none`() {
        // Given
        val codes = listOf("violet", "TEAL", "#FA0", "#3A7BDZ", "", null)

        // When
        val tints = codes.map { CategoryTint.fromCodeOrNull(it) }

        // Then
        assertTrue(tints.all { it == null })
    }

    @Test
    fun `GIVEN a named tint WHEN reading its code THEN is the named key`() {
        // Given
        val tint = CategoryColor.TEAL.tint

        // When
        val code = tint.code

        // Then
        assertEquals("teal", code)
    }

    @Test
    fun `GIVEN a custom tint WHEN reading its code THEN is its uppercase zero padded hex`() {
        // Given
        val tints = listOf(CustomColor(0x3a7bd5).tint, CustomColor(0x0000FF).tint)

        // When
        val codes = tints.map { it.code }

        // Then
        assertEquals(listOf("#3A7BD5", "#0000FF"), codes)
    }

    @Test
    fun `GIVEN a tint WHEN reading its code back THEN gives the same tint`() {
        // Given
        val tints = listOf(CategoryColor.LIME.tint, CustomColor(0x1A2340).tint)

        // When
        val readBack = tints.map { CategoryTint.fromCodeOrNull(it.code) }

        // Then
        assertEquals(tints, readBack)
    }

    @Test
    fun `GIVEN a named tint WHEN reading its analytics code THEN is the named key`() {
        // Given
        val tint = CategoryColor.ORANGE.tint

        // When
        val code = tint.analyticsCode

        // Then
        assertEquals("orange", code)
    }

    @Test
    fun `GIVEN a custom tint WHEN reading its analytics code THEN is custom and never the hex`() {
        // Given
        val tint = CustomColor(0x3A7BD5).tint

        // When
        val code = tint.analyticsCode

        // Then
        assertEquals("custom", code)
    }
}
