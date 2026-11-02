package com.quare.blitzsplit.core.profile.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PixBrCodeTest {
    @Test
    fun `GIVEN a phone key 98 reais and Ana WHEN building the code THEN gives the known copia e cola`() {
        // Given
        val key = "+5521987654321"

        // When
        val code = PixBrCode.of(
            key = key,
            amountMinor = 9_800L,
            receiverName = "Ana",
        )

        // Then
        assertEquals(
            "00020126360014br.gov.bcb.pix0114+5521987654321520400005303986540598.005802BR5903Ana6006BRASIL" +
                "62070503***6304D3C1",
            code,
        )
    }

    @Test
    fun `GIVEN an email key and an accented name WHEN building the code THEN writes the name without accents`() {
        // Given
        val key = "ana.souza@email.com"

        // When
        val code = PixBrCode.of(
            key = key,
            amountMinor = 1_050L,
            receiverName = "José da Silva",
        )

        // Then
        assertEquals(
            "00020126410014br.gov.bcb.pix0119ana.souza@email.com520400005303986540510.505802BR5913Jose da Silva" +
                "6006BRASIL62070503***6304DD6F",
            code,
        )
    }

    @Test
    fun `GIVEN a CPF key and more than a thousand reais WHEN building the code THEN gives the known copia e cola`() {
        // Given
        val key = "12345678909"

        // When
        val code = PixBrCode.of(
            key = key,
            amountMinor = 123_456L,
            receiverName = "Pierre",
        )

        // Then
        assertEquals(
            "00020126330014br.gov.bcb.pix01111234567890952040000530398654071234.565802BR5906Pierre6006BRASIL" +
                "62070503***630463F4",
            code,
        )
    }

    @Test
    fun `GIVEN a random key WHEN building the code THEN the merchant account holds the GUI and the key`() {
        // Given
        val key = "123e4567-e12b-12d1-a456-426655440000"

        // When
        val code = PixBrCode.of(
            key = key,
            amountMinor = 9_800L,
            receiverName = "Ana",
        )

        // Then
        val merchantAccount = fieldsOf(code).getValue("26")
        assertEquals(
            mapOf(
                "00" to "br.gov.bcb.pix",
                "01" to key,
            ),
            fieldsOf(merchantAccount),
        )
        assertTrue(code.contains("2658"))
    }

    @Test
    fun `GIVEN any key WHEN building the code THEN carries the fixed fields in order`() {
        // Given
        val key = "+5521987654321"

        // When
        val code = PixBrCode.of(
            key = key,
            amountMinor = 9_800L,
            receiverName = "Ana",
        )

        // Then
        val fields = fieldsOf(code)
        assertEquals(listOf("00", "26", "52", "53", "54", "58", "59", "60", "62", "63"), fields.keys.toList())
        assertEquals("01", fields["00"])
        assertEquals("0000", fields["52"])
        assertEquals("986", fields["53"])
        assertEquals("BR", fields["58"])
        assertEquals("BRASIL", fields["60"])
        assertEquals("0503***", fields["62"])
    }

    @Test
    fun `GIVEN no amount WHEN building the code THEN leaves the amount out for the payer to type`() {
        // Given
        val amountMinor: Long? = null

        // When
        val code = PixBrCode.of(
            key = "+5521987654321",
            amountMinor = amountMinor,
            receiverName = "Ana",
        )

        // Then
        assertEquals(
            "00020126360014br.gov.bcb.pix0114+55219876543215204000053039865802BR5903Ana6006BRASIL62070503***" +
                "63049396",
            code,
        )
        assertFalse(fieldsOf(code).containsKey("54"))
    }

    @Test
    fun `GIVEN a zero amount WHEN building the code THEN leaves the amount out as with none`() {
        // Given
        val amountMinor = 0L

        // When
        val code = PixBrCode.of(
            key = "+5521987654321",
            amountMinor = amountMinor,
            receiverName = "Ana",
        )

        // Then
        assertEquals(
            PixBrCode.of(
                key = "+5521987654321",
                amountMinor = null,
                receiverName = "Ana",
            ),
            code,
        )
        assertFalse(fieldsOf(code).containsKey("54"))
    }

    @Test
    fun `GIVEN one centavo WHEN building the code THEN writes 0_01 and pads the check digits to four`() {
        // Given
        val amountMinor = 1L

        // When
        val code = PixBrCode.of(
            key = "+5521987654321",
            amountMinor = amountMinor,
            receiverName = "Ana",
        )

        // Then
        assertEquals("0.01", fieldsOf(code)["54"])
        assertEquals(
            "00020126360014br.gov.bcb.pix0114+552198765432152040000530398654040.015802BR5903Ana6006BRASIL" +
                "62070503***63040AFE",
            code,
        )
    }

    @Test
    fun `GIVEN 100 million reais WHEN building the code THEN writes the whole amount with two decimals`() {
        // Given
        val amountMinor = 10_000_000_000L

        // When
        val code = PixBrCode.of(
            key = "+5521987654321",
            amountMinor = amountMinor,
            receiverName = "Ana",
        )

        // Then
        assertTrue(code.contains("5412100000000.00"))
        assertTrue(code.endsWith("63042A2C"))
    }

    @Test
    fun `GIVEN a name longer than 25 letters WHEN building the code THEN cuts it to 25`() {
        // Given
        val receiverName = "Maria Aparecida dos Santos Oliveira"

        // When
        val code = PixBrCode.of(
            key = "+5521987654321",
            amountMinor = 9_800L,
            receiverName = receiverName,
        )

        // Then
        assertEquals("Maria Aparecida dos Santo", fieldsOf(code)["59"])
        assertTrue(code.contains("5925Maria Aparecida dos Santo6006"))
        assertTrue(code.endsWith("6304B35D"))
    }

    @Test
    fun `GIVEN a name of only emoji and symbols WHEN building the code THEN writes PIX as the name`() {
        // Given
        val receiverName = "🎉!!"

        // When
        val code = PixBrCode.of(
            key = "+5521987654321",
            amountMinor = 9_800L,
            receiverName = receiverName,
        )

        // Then
        assertEquals("PIX", fieldsOf(code)["59"])
        assertTrue(code.endsWith("63044EC5"))
    }

    @Test
    fun `GIVEN an empty name WHEN building the code THEN writes PIX as the name`() {
        // Given
        val receiverName = ""

        // When
        val code = PixBrCode.of(
            key = "+5521987654321",
            amountMinor = 9_800L,
            receiverName = receiverName,
        )

        // Then
        assertEquals("PIX", fieldsOf(code)["59"])
    }

    @Test
    fun `GIVEN a name with symbols and an accent WHEN building the code THEN keeps only letters digits and spaces`() {
        // Given
        val receiverName = "Ana-Lú (BR)"

        // When
        val code = PixBrCode.of(
            key = "+5521987654321",
            amountMinor = 9_800L,
            receiverName = receiverName,
        )

        // Then
        assertEquals("AnaLu BR", fieldsOf(code)["59"])
        assertTrue(code.endsWith("63047FD4"))
    }

    @Test
    fun `GIVEN accented capitals WHEN building the code THEN keeps them capital without the accent`() {
        // Given
        val receiverName = "Élise Ñuñez"

        // When
        val code = PixBrCode.of(
            key = "123e4567-e12b-12d1-a456-426655440000",
            amountMinor = 9_800L,
            receiverName = receiverName,
        )

        // Then
        assertEquals("Elise Nunez", fieldsOf(code)["59"])
        assertTrue(code.endsWith("63046AE6"))
    }

    @Test
    fun `GIVEN a name with a caron WHEN building the code THEN removes the accent and keeps the letter`() {
        // Given
        val receiverName = "Šárka"

        // When
        val code = PixBrCode.of(
            key = "+5521987654321",
            amountMinor = 9_800L,
            receiverName = receiverName,
        )

        // Then
        assertEquals("Sarka", fieldsOf(code)["59"])
        assertTrue(code.endsWith("6304BF1B"))
    }

    @Test
    fun `GIVEN a name of only spaces WHEN building the code THEN writes PIX as the name`() {
        // Given
        val receiverName = "   "

        // When
        val code = PixBrCode.of(
            key = "+5521987654321",
            amountMinor = 9_800L,
            receiverName = receiverName,
        )

        // Then
        assertEquals("PIX", fieldsOf(code)["59"])
    }

    @Test
    fun `GIVEN spaces around the name WHEN building the code THEN writes the name without them`() {
        // Given
        val receiverName = "  Ana  "

        // When
        val code = PixBrCode.of(
            key = "+5521987654321",
            amountMinor = 9_800L,
            receiverName = receiverName,
        )

        // Then
        assertEquals("Ana", fieldsOf(code)["59"])
    }

    @Test
    fun `GIVEN a cut that ends on a space WHEN building the code THEN drops that space`() {
        // Given
        val receiverName = "Maria Aparecida dos Sant Oliveira"

        // When
        val code = PixBrCode.of(
            key = "+5521987654321",
            amountMinor = 9_800L,
            receiverName = receiverName,
        )

        // Then
        assertEquals("Maria Aparecida dos Sant", fieldsOf(code)["59"])
    }

    // Why: reads the EMV fields back (two-digit id, two-digit length, the value), so a case checks one field alone.
    private fun fieldsOf(payload: String): Map<String, String> {
        val fields = linkedMapOf<String, String>()
        var index = 0
        while (index < payload.length) {
            val id = payload.substring(index, index + 2)
            val length = payload.substring(index + 2, index + 4).toInt()
            fields[id] = payload.substring(index + 4, index + 4 + length)
            index += 4 + length
        }
        return fields
    }
}
