package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.ColorDraft
import com.quare.blitzsplit.core.expenses.domain.model.ColorDraft.ColorChannel
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.tint
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColorDraftTest {
    private val tealDraft = ColorDraft.createStartingAt(CategoryColor.TEAL.tint)
    private val blueDraft = ColorDraft.createStartingAt(CustomColor(0x0000FF).tint)

    @Test
    fun `GIVEN a category with a named color WHEN opening New color THEN starts at that color base`() {
        // Given
        val tint = CategoryColor.TEAL.tint

        // When
        val draft = ColorDraft.createStartingAt(tint)

        // Then
        assertEquals("#00867C", draft.hexText)
        assertEquals(listOf("0", "134", "124"), draft.channels())
        assertEquals(CustomColor(CategoryColor.TEAL.baseRgb), draft.color)
        assertTrue(draft.canSave)
    }

    @Test
    fun `GIVEN every named color WHEN opening New color THEN each starts at its own base`() {
        // Given
        val colors = CategoryColor.entries

        // When
        val drafts = colors.map { ColorDraft.createStartingAt(it.tint) }

        // Then
        assertEquals(colors.map { CustomColor(it.baseRgb).hex }, drafts.map { it.hexText })
    }

    @Test
    fun `GIVEN a category with a custom color WHEN opening New color THEN starts at that color`() {
        // Given
        val tint = CustomColor(0x3A7BD5).tint

        // When
        val draft = ColorDraft.createStartingAt(tint)

        // Then
        assertEquals("#3A7BD5", draft.hexText)
        assertEquals(listOf("58", "123", "213"), draft.channels())
        assertEquals(CustomColor(0x3A7BD5), draft.color)
    }

    @Test
    fun `GIVEN a draft WHEN dragging the gradient THEN the hex and RGB fields follow`() {
        // Given
        val draft = tealDraft

        // When
        val dragged = draft.withHsv(240.0, 1.0, 1.0)

        // Then
        assertEquals(240.0, dragged.hue)
        assertEquals("#0000FF", dragged.hexText)
        assertEquals(listOf("0", "0", "255"), dragged.channels())
        assertEquals(CustomColor(0x0000FF), dragged.color)
    }

    @Test
    fun `GIVEN a draft WHEN typing a whole hex code THEN keeps it as typed and moves the gradient and RGB fields`() {
        // Given
        val typed = "#3a7bd5"

        // When
        val draft = tealDraft.withHexText(typed)

        // Then
        val expected = CustomColor(0x3A7BD5).toHsv()
        assertEquals("#3a7bd5", draft.hexText)
        assertEquals(listOf("58", "123", "213"), draft.channels())
        assertClose(expected.hue, draft.hue)
        assertClose(expected.saturation, draft.saturation)
        assertClose(expected.value, draft.value)
    }

    @Test
    fun `GIVEN a draft WHEN typing a whole hex code without a hash THEN the RGB fields follow`() {
        // Given
        val typed = "FFF59D"

        // When
        val draft = tealDraft.withHexText(typed)

        // Then
        assertEquals("FFF59D", draft.hexText)
        assertEquals(listOf("255", "245", "157"), draft.channels())
        assertEquals(CustomColor(0xFFF59D), draft.color)
    }

    @Test
    fun `GIVEN a draft WHEN typing half a hex code THEN keeps it and leaves the gradient and RGB fields`() {
        // Given
        val typed = "#3A7"

        // When
        val draft = tealDraft.withHexText(typed)

        // Then
        assertEquals("#3A7", draft.hexText)
        assertEquals(tealDraft.channels(), draft.channels())
        assertEquals(tealDraft.hue, draft.hue)
        assertEquals(tealDraft.saturation, draft.saturation)
        assertEquals(tealDraft.value, draft.value)
    }

    @Test
    fun `GIVEN a draft on a hue WHEN typing a gray THEN keeps the hue and drops the saturation`() {
        // Given
        val typed = "#808080"

        // When
        val draft = tealDraft.withHexText(typed)

        // Then
        assertEquals(tealDraft.hue, draft.hue)
        assertEquals(0.0, draft.saturation)
        assertClose(128 / 255.0, draft.value)
        assertEquals(listOf("128", "128", "128"), draft.channels())
    }

    @Test
    fun `GIVEN a draft on a hue WHEN typing black THEN keeps the hue and the saturation`() {
        // Given
        val typed = "#000000"

        // When
        val draft = tealDraft.withHexText(typed)

        // Then
        assertEquals(tealDraft.hue, draft.hue)
        assertEquals(tealDraft.saturation, draft.saturation)
        assertEquals(0.0, draft.value)
        assertEquals(listOf("0", "0", "0"), draft.channels())
        assertEquals(CustomColor(0x000000), draft.color)
    }

    @Test
    fun `GIVEN a draft WHEN typing letters and more than three digits in a channel THEN keeps up to three digits`() {
        // Given
        val typed = "1a2b3c4"

        // When
        val draft = tealDraft.withChannelText(ColorChannel.GREEN, typed)

        // Then
        assertEquals("123", draft.greenText)
    }

    @Test
    fun `GIVEN three whole channels WHEN typing one THEN the gradient and the hex code follow`() {
        // Given
        val typed = "255"

        // When
        val draft = blueDraft.withChannelText(ColorChannel.RED, typed)

        // Then
        assertEquals("#FF00FF", draft.hexText)
        assertEquals(listOf("255", "0", "255"), draft.channels())
        assertClose(300.0, draft.hue)
        assertEquals(CustomColor(0xFF00FF), draft.color)
    }

    @Test
    fun `GIVEN a channel typed with leading zeros WHEN the color follows THEN the field keeps what was typed`() {
        // Given
        val typed = "007"

        // When
        val draft = blueDraft.withChannelText(ColorChannel.RED, typed)

        // Then
        assertEquals("007", draft.redText)
        assertEquals("#0700FF", draft.hexText)
    }

    @Test
    fun `GIVEN a draft WHEN emptying a channel THEN the hex code and gradient stay`() {
        // Given
        val typed = ""

        // When
        val draft = blueDraft.withChannelText(ColorChannel.BLUE, typed)

        // Then
        assertEquals("", draft.blueText)
        assertEquals("#0000FF", draft.hexText)
        assertEquals(blueDraft.hue, draft.hue)
        assertFalse(draft.canSave)
    }

    @Test
    fun `GIVEN a draft WHEN typing a channel past 255 THEN the RGB is invalid the hex stays and it cannot save`() {
        // Given
        val typed = "300"

        // When
        val draft = blueDraft.withChannelText(ColorChannel.RED, typed)

        // Then
        assertEquals("300", draft.redText)
        assertEquals("#0000FF", draft.hexText)
        assertTrue(draft.isRgbInvalid)
        assertFalse(draft.canSave)
    }

    @Test
    fun `GIVEN channels up to 255 WHEN checking the RGB THEN it is valid`() {
        // Given
        val draft = blueDraft.withChannelText(ColorChannel.RED, "255")

        // When
        val invalid = draft.isRgbInvalid

        // Then
        assertFalse(invalid)
    }

    @Test
    fun `GIVEN a hex code being typed WHEN checking it THEN it is not invalid yet and cannot save`() {
        // Given
        val typed = listOf("", "#", "#3", "3A7B", "#3a7bd")

        // When
        val drafts = typed.map { tealDraft.withHexText(it) }

        // Then
        assertTrue(drafts.none { it.isHexInvalid })
        assertTrue(drafts.none { it.canSave })
    }

    @Test
    fun `GIVEN a character that is not hex WHEN checking the code THEN it is invalid`() {
        // Given
        val typed = "#3A7BDZ"

        // When
        val draft = tealDraft.withHexText(typed)

        // Then
        assertTrue(draft.isHexInvalid)
        assertFalse(draft.canSave)
    }

    @Test
    fun `GIVEN seven digits without a hash WHEN checking the code THEN it is invalid as too long`() {
        // Given
        val typed = "3A7BD5F"

        // When
        val draft = tealDraft.withHexText(typed)

        // Then
        assertTrue(draft.isHexInvalid)
        assertFalse(draft.canSave)
    }

    @Test
    fun `GIVEN more than a hash and six digits typed WHEN keeping the code THEN it is cut at seven characters`() {
        // Given
        val typed = "#3A7BD5FFFF"

        // When
        val draft = tealDraft.withHexText(typed)

        // Then
        assertEquals("#3A7BD5", draft.hexText)
        assertEquals(CustomColor(0x3A7BD5), draft.color)
    }

    @Test
    fun `GIVEN a whole hex code typed WHEN reading the draft color THEN is exactly the typed code`() {
        // Given
        val typed = listOf("#3A7BD6", "#010203", "#FEFEFE", "#7F7F80")

        // When
        val colors = typed.map { tealDraft.withHexText(it).color }

        // Then
        assertEquals(typed.map { CustomColor.fromHexOrNull(it) }, colors)
        assertTrue(typed.all { tealDraft.withHexText(it).canSave })
    }

    @Test
    fun `GIVEN half a hex code WHEN reading the draft color THEN is the gradient color`() {
        // Given
        val typed = "#3A7"

        // When
        val color = tealDraft.withHexText(typed).color

        // Then
        assertEquals(CustomColor(CategoryColor.TEAL.baseRgb), color)
    }

    private fun ColorDraft.channels(): List<String> = listOf(redText, greenText, blueText)

    private fun assertClose(
        expected: Double,
        actual: Double,
    ) {
        assertTrue(abs(expected - actual) < 1e-9, "expected $expected, got $actual")
    }
}
