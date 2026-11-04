package com.quare.blitzsplit.core.expenses.domain

import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.model.TypedText
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.test.Test
import kotlin.test.assertEquals

class TypedTextTest {
    @Test
    fun `GIVEN an amount pasted with its symbol and letters WHEN typed THEN reads the amount in it`() {
        // Given
        val text = "R$ 1.234,56abc"

        // When
        val kept = TypedText.toAmountText("12,34", "12,34$text", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("1.234,56", kept)
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
        val amount = TypedText.toSplitValueText("", text, SplitMode.AMOUNT, GroupCurrency.BRL, false, ',')
        val percent = TypedText.toSplitValueText("", text, SplitMode.PERCENT, GroupCurrency.BRL, false, ',')

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

    @Test
    fun `GIVEN reais and a comma WHEN typing digits one at a time THEN they come in from the right`() {
        // Given
        val keys = "123456"

        // When
        val texts = typeOneByOne(keys, GroupCurrency.BRL, ',')

        // Then
        assertEquals(listOf("0,01", "0,12", "1,23", "12,34", "123,45", "1.234,56"), texts)
    }

    @Test
    fun `GIVEN reais and a dot WHEN typing digits one at a time THEN groups thousands with the comma`() {
        // Given
        val keys = "123456"

        // When
        val texts = typeOneByOne(keys, GroupCurrency.BRL, '.')

        // Then
        assertEquals(listOf("0.01", "0.12", "1.23", "12.34", "123.45", "1,234.56"), texts)
    }

    @Test
    fun `GIVEN yen and a comma WHEN typing digits one at a time THEN writes no decimals and groups with the dot`() {
        // Given
        val keys = "12345"

        // When
        val texts = typeOneByOne(keys, GroupCurrency.JPY, ',')

        // Then
        assertEquals(listOf("1", "12", "123", "1.234", "12.345"), texts)
    }

    @Test
    fun `GIVEN yen and a dot WHEN typing digits one at a time THEN writes no decimals and groups with the comma`() {
        // Given
        val keys = "12345"

        // When
        val texts = typeOneByOne(keys, GroupCurrency.JPY, '.')

        // Then
        assertEquals(listOf("1", "12", "123", "1,234", "12,345"), texts)
    }

    @Test
    fun `GIVEN chilean pesos WHEN typing a digit THEN it is a whole peso`() {
        // Given
        val previous = "99"

        // When
        val kept = TypedText.toAmountText(previous, "${previous}9", GroupCurrency.CLP, false, ',')

        // Then
        assertEquals("999", kept)
    }

    @Test
    fun `GIVEN an Arabic decimal separator WHEN typing digits THEN falls back to the dot and comma`() {
        // Given
        val keys = "123456"

        // When
        val texts = typeOneByOne(keys, GroupCurrency.BRL, '٫')

        // Then
        assertEquals("1,234.56", texts.last())
    }

    @Test
    fun `GIVEN an amount WHEN typing a comma after it THEN nothing changes`() {
        // Given
        val previous = "1,23"

        // When
        val kept = TypedText.toAmountText(previous, "$previous,", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("1,23", kept)
    }

    @Test
    fun `GIVEN an amount WHEN typing a dot after it THEN nothing changes`() {
        // Given
        val previous = "1,23"

        // When
        val kept = TypedText.toAmountText(previous, "$previous.", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("1,23", kept)
    }

    @Test
    fun `GIVEN an amount WHEN typing a letter THEN nothing changes`() {
        // Given
        val previous = "1,23"

        // When
        val kept = TypedText.toAmountText(previous, "${previous}a", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("1,23", kept)
    }

    @Test
    fun `GIVEN an empty field WHEN typing a separator THEN it stays empty`() {
        // Given
        val previous = ""

        // When
        val kept = TypedText.toAmountText(previous, ",", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("", kept)
    }

    @Test
    fun `GIVEN an amount WHEN deleting its last character THEN removes the last digit`() {
        // Given
        val previous = "1,23"

        // When
        val kept = TypedText.toAmountText(previous, previous.dropLast(1), GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("0,12", kept)
    }

    @Test
    fun `GIVEN thousands WHEN deleting the last character THEN regroups what is left`() {
        // Given
        val previous = "1.234,56"

        // When
        val kept = TypedText.toAmountText(previous, previous.dropLast(1), GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("123,45", kept)
    }

    @Test
    fun `GIVEN one cent WHEN deleting its last digit THEN the field is empty`() {
        // Given
        val previous = "0,01"

        // When
        val kept = TypedText.toAmountText(previous, previous.dropLast(1), GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("", kept)
    }

    @Test
    fun `GIVEN an empty field WHEN typing a zero THEN it stays empty`() {
        // Given
        val previous = ""

        // When
        val kept = TypedText.toAmountText(previous, "0", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("", kept)
    }

    @Test
    fun `GIVEN an amount WHEN the field holds the same text again THEN keeps it`() {
        // Given
        val previous = "1.234,56"

        // When
        val kept = TypedText.toAmountText(previous, previous, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("1.234,56", kept)
    }

    @Test
    fun `GIVEN reais with the 11 digits of the cap WHEN typing another digit THEN keeps the field`() {
        // Given
        val previous = "100.000.000,00"

        // When
        val kept = TypedText.toAmountText(previous, "${previous}1", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("100.000.000,00", kept)
    }

    @Test
    fun `GIVEN reais with 10 digits WHEN typing an 11th over the cap THEN keeps it for the form error`() {
        // Given
        val previous = "99.999.999,99"

        // When
        val kept = TypedText.toAmountText(previous, "${previous}9", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("999.999.999,99", kept)
    }

    @Test
    fun `GIVEN yen with the 9 digits of the largest amount WHEN typing another digit THEN keeps the field as it was`() {
        // Given
        val previous = "123.456.789"

        // When
        val kept = TypedText.toAmountText(previous, "${previous}1", GroupCurrency.JPY, false, ',')

        // Then
        assertEquals("123.456.789", kept)
    }

    @Test
    fun `GIVEN an empty field WHEN pasting one decimal digit THEN reads it as tens of cents`() {
        // Given
        val pasted = "12,5"

        // When
        val kept = TypedText.toAmountText("", pasted, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("12,50", kept)
    }

    @Test
    fun `GIVEN an empty field WHEN pasting a whole number THEN reads it in reais`() {
        // Given
        val pasted = "50"

        // When
        val kept = TypedText.toAmountText("", pasted, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("50,00", kept)
    }

    @Test
    fun `GIVEN an empty field WHEN pasting an amount with its symbol THEN reads the amount`() {
        // Given
        val pasted = "R$ 1.234,56"

        // When
        val kept = TypedText.toAmountText("", pasted, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("1.234,56", kept)
    }

    @Test
    fun `GIVEN a device with the comma WHEN pasting an amount written in English THEN writes it with the comma`() {
        // Given
        val pasted = "1,234.56"

        // When
        val kept = TypedText.toAmountText("", pasted, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("1.234,56", kept)
    }

    @Test
    fun `GIVEN an amount WHEN pasting another after it THEN only the pasted one counts`() {
        // Given
        val previous = "12,34"

        // When
        val kept = TypedText.toAmountText(previous, "${previous}56,78", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("56,78", kept)
    }

    @Test
    fun `GIVEN an amount WHEN pasting a longer one over it THEN reads all of what was pasted`() {
        // Given
        val previous = "1,00"

        // When
        val kept = TypedText.toAmountText(previous, "250,75", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("250,75", kept)
    }

    @Test
    fun `GIVEN an amount WHEN pasting more digits than the largest amount has THEN keeps the field as it was`() {
        // Given
        val previous = "1,00"

        // When
        val kept = TypedText.toAmountText(previous, "1.000.000.000", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("1,00", kept)
    }

    @Test
    fun `GIVEN an amount WHEN pasting more digits than any amount can have THEN keeps the field as it was`() {
        // Given
        val previous = "1,00"

        // When
        val kept = TypedText.toAmountText(previous, "1234567890123", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("1,00", kept)
    }

    @Test
    fun `GIVEN reais WHEN pasting the largest amount THEN keeps it`() {
        // Given
        val pasted = "100000000"

        // When
        val kept = TypedText.toAmountText("", pasted, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("100.000.000,00", kept)
    }

    @Test
    fun `GIVEN an amount WHEN pasting letters after it THEN keeps the field as it was`() {
        // Given
        val previous = "1,00"

        // When
        val kept = TypedText.toAmountText(previous, "${previous}abc", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("1,00", kept)
    }

    @Test
    fun `GIVEN an empty field WHEN pasting zero THEN it stays empty`() {
        // Given
        val pasted = "0,00"

        // When
        val kept = TypedText.toAmountText("", pasted, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("", kept)
    }

    @Test
    fun `GIVEN Valores WHEN typing a digit in a split value THEN it comes in from the right`() {
        // Given
        val previous = "0,01"

        // When
        val kept = TypedText.toSplitValueText(previous, "0,012", SplitMode.AMOUNT, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("0,12", kept)
    }

    @Test
    fun `GIVEN Partes WHEN typing a split value with a separator THEN keeps only its digits`() {
        // Given
        val typed = "2,5"

        // When
        val kept = TypedText.toSplitValueText("2,", typed, SplitMode.SHARES, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("25", kept)
    }

    @Test
    fun `GIVEN percent WHEN typing a fourth digit THEN keeps the first three`() {
        // Given
        val typed = "1000"

        // When
        val kept = TypedText.toSplitValueText("100", typed, SplitMode.PERCENT, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("100", kept)
    }

    @Test
    fun `GIVEN reais with cents WHEN moving to yen THEN keeps the same number without the cents`() {
        // Given
        val text = "12,50"

        // When
        val converted = TypedText.toCurrency(text, GroupCurrency.BRL, GroupCurrency.JPY, false, ',')

        // Then
        assertEquals("12", converted)
    }

    @Test
    fun `GIVEN yen grouped WHEN moving to reais THEN keeps the same number with zero cents`() {
        // Given
        val text = "1.250"

        // When
        val converted = TypedText.toCurrency(text, GroupCurrency.JPY, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("1.250,00", converted)
    }

    @Test
    fun `GIVEN yen grouped by the comma WHEN moving to dollars on a device with the dot THEN writes it in English`() {
        // Given
        val text = "1,250"

        // When
        val converted = TypedText.toCurrency(text, GroupCurrency.JPY, GroupCurrency.USD, false, '.')

        // Then
        assertEquals("1,250.00", converted)
    }

    @Test
    fun `GIVEN an Arabic decimal separator WHEN moving to another currency THEN writes the dot as decimal`() {
        // Given
        val text = "1,250"

        // When
        val converted = TypedText.toCurrency(text, GroupCurrency.JPY, GroupCurrency.BRL, false, '٫')

        // Then
        assertEquals("1,250.00", converted)
    }

    @Test
    fun `GIVEN the same currency WHEN moving to it THEN keeps the text as it is`() {
        // Given
        val text = "12,5"

        // When
        val converted = TypedText.toCurrency(text, GroupCurrency.BRL, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("12,5", converted)
    }

    @Test
    fun `GIVEN a text that is no amount WHEN moving to another currency THEN the field is empty`() {
        // Given
        val text = "abc"

        // When
        val converted = TypedText.toCurrency(text, GroupCurrency.BRL, GroupCurrency.JPY, false, ',')

        // Then
        assertEquals("", converted)
    }

    @Test
    fun `GIVEN an empty field WHEN moving to another currency THEN it stays empty`() {
        // Given
        val text = ""

        // When
        val converted = TypedText.toCurrency(text, GroupCurrency.JPY, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("", converted)
    }

    @Test
    fun `GIVEN only cents WHEN moving to yen THEN the field is empty`() {
        // Given
        val text = "0,50"

        // When
        val converted = TypedText.toCurrency(text, GroupCurrency.BRL, GroupCurrency.JPY, false, ',')

        // Then
        assertEquals("", converted)
    }

    @Test
    fun `GIVEN an amount WHEN the field is selected whole and one digit typed THEN starts over from it`() {
        // Given
        val previous = "1.234,56"

        // When
        val kept = TypedText.toAmountText(previous, "7", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("0,07", kept)
    }

    @Test
    fun `GIVEN a refused comma still shown WHEN typing a digit after it THEN the digit comes in from the right`() {
        // Given
        val previous = "0,05"

        // When
        val kept = TypedText.toAmountText(previous, "0,05,1", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("0,51", kept)
    }

    @Test
    fun `GIVEN a field a key behind the amount WHEN typing the next digit THEN it comes in from the right`() {
        // Given
        val previous = "0,12"

        // When
        val kept = TypedText.toAmountText(previous, "0,0123", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("1,23", kept)
    }

    @Test
    fun `GIVEN a field a key behind the first digit WHEN typing the second THEN it comes in from the right`() {
        // Given
        val previous = "0,01"

        // When
        val kept = TypedText.toAmountText(previous, "12", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("0,12", kept)
    }

    @Test
    fun `GIVEN an empty field WHEN pasting cents after a zero THEN reads them as tens of cents`() {
        // Given
        val pasted = "0,5"

        // When
        val kept = TypedText.toAmountText("", pasted, GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("0,50", kept)
    }

    @Test
    fun `GIVEN a long amount WHEN pasting a shorter one over it THEN reads the pasted amount`() {
        // Given
        val previous = "1.234,56"

        // When
        val kept = TypedText.toAmountText(previous, "12,5", GroupCurrency.BRL, false, ',')

        // Then
        assertEquals("12,50", kept)
    }

    // Why: what the field holds after each key, as the ViewModel stores it and hands it back as the previous text.
    private fun typeOneByOne(
        keys: String,
        currency: GroupCurrency,
        decimalSeparator: Char,
    ): List<String> = keys
        .runningFold("") { previous, key ->
            TypedText.toAmountText(previous, previous + key, currency, false, decimalSeparator)
        }.drop(1)
}
