package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.model.TypedText
import kotlin.test.Test
import kotlin.test.assertEquals

class TypedTextTest {
    @Test
    fun `GIVEN an amount with letters and too many digits WHEN typed THEN keeps digits and separators up to 16`() {
        // Given
        val text = "R$ 1.234,56abc99999999999"

        // When
        val kept = TypedText.toAmountText(text)

        // Then
        assertEquals("1.234,5699999999", kept)
    }

    @Test
    fun `GIVEN a percentage with a sign and four digits WHEN typed THEN keeps the first three digits`() {
        // Given
        val text = "1005%"

        // When
        val kept = TypedText.toPercentText(text)

        // Then
        assertEquals("100", kept)
    }

    @Test
    fun `GIVEN Valores and percent WHEN typing a split value THEN each keeps the text of its own field`() {
        // Given
        val text = "12,50"

        // When
        val amount = TypedText.toSplitValueText(text, SplitMode.AMOUNT)
        val percent = TypedText.toSplitValueText(text, SplitMode.PERCENT)

        // Then
        assertEquals("12,50", amount)
        assertEquals("125", percent)
    }

    @Test
    fun `GIVEN a decimal separator AmountInput cannot read WHEN picking the one to write THEN it is the dot`() {
        // Given
        val arabic = '٫'

        // When
        val separator = TypedText.amountSeparatorOf(arabic)

        // Then
        assertEquals('.', separator)
        assertEquals(',', TypedText.amountSeparatorOf(','))
    }
}
