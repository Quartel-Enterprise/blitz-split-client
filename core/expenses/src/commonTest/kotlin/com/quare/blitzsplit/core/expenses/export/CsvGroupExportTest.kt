package com.quare.blitzsplit.core.expenses.export

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CsvGroupExportTest {
    private val encoder = CsvGroupExport()
    private val words = exportWords()

    @Test
    fun `GIVEN an export WHEN writing the CSV THEN the file opens with the UTF-8 byte order mark`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                    description = "Açaí",
                ),
            ),
        )

        // When
        val bytes = encoder.encode(export, words)

        // Then
        assertEquals(listOf(0xEF, 0xBB, 0xBF), bytes.take(3).map { it.toInt() and 0xFF })
        assertTrue(bytes.decodeToString().contains("Açaí"))
    }

    @Test
    fun `GIVEN an export WHEN writing the CSV THEN every line ends with CRLF`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                ),
            ),
            payments = listOf(
                exportPayment(
                    id = "pix",
                    amountMinor = 40L,
                ),
            ),
        )

        // When
        val text = csvOf(encoder.encode(export, words))

        // Then
        assertTrue(text.endsWith("\r\n"))
        assertFalse(Regex("[^\r]\n").containsMatchIn(text))
    }

    @Test
    fun `GIVEN a bill and a payment WHEN writing the CSV THEN sections are title headers rows and a blank line`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                ),
            ),
            payments = listOf(
                exportPayment(
                    id = "pix",
                    amountMinor = 40L,
                ),
            ),
        )

        // When
        val lines = linesOf(encoder.encode(export, words))

        // Then
        assertEquals("Bills", lines[0])
        assertTrue(lines[1].startsWith("Date,Description,Category,Amount,Currency"))
        assertEquals("", lines[3])
        assertEquals("Payments", lines[4])
        assertEquals("Date,From,To,Amount,Currency", lines[5])
        assertEquals("2026-11-11,Ana,Pierre,0.40,BRL", lines[6])
        assertEquals("", lines[7])
        assertEquals("Balances", lines[8])
        assertEquals("Member,Currency,Balance", lines[9])
        assertEquals(listOf("Pierre,BRL,0.60", "Ana,BRL,-0.60", "Bruno,BRL,0.00"), lines.subList(10, 13))
    }

    @Test
    fun `GIVEN a bill Pierre paid for Ana WHEN writing the CSV THEN ISO date dot decimals and empty Bruno cells`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 12_050L,
                ),
            ),
        )

        // When
        val lines = linesOf(encoder.encode(export, words))

        // Then
        val row = lines[1].split(",").zip(lines[2].split(",")).toMap()
        assertEquals("2026-11-10", row.getValue("Date"))
        assertEquals("120.50", row.getValue("Amount"))
        assertEquals("120.50", row.getValue("Paid · Pierre"))
        assertEquals("", row.getValue("Share · Pierre"))
        assertEquals("120.50", row.getValue("Share · Ana"))
        assertEquals("", row.getValue("Paid · Bruno"))
        assertEquals("", row.getValue("Share · Bruno"))
    }

    @Test
    fun `GIVEN no bills nor payments WHEN writing the CSV THEN those sections are only title and headers`() {
        // Given
        val export = exportOf()

        // When
        val lines = linesOf(encoder.encode(export, words))

        // Then
        assertEquals("Bills", lines[0])
        assertEquals("", lines[2])
        assertEquals("Payments", lines[3])
        assertEquals("Date,From,To,Amount,Currency", lines[4])
        assertEquals("", lines[5])
        assertEquals("Balances", lines[6])
    }

    @Test
    fun `GIVEN a description with a comma WHEN writing the CSV THEN the field is quoted`() {
        // Given
        val export =
            exportOf(
                bills = listOf(
                    exportBill(
                        id = "dinner",
                        amountMinor = 100L,
                        description = "Rice, beans",
                    ),
                ),
            )

        // When
        val lines = linesOf(encoder.encode(export, words))

        // Then
        assertTrue(lines[2].startsWith("2026-11-10,\"Rice, beans\",Food,"))
    }

    @Test
    fun `GIVEN a description with quotes WHEN writing the CSV THEN the field is quoted with its quotes doubled`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                    description = "Bar \"do Zé\"",
                ),
            ),
        )

        // When
        val lines = linesOf(encoder.encode(export, words))

        // Then
        assertTrue(lines[2].startsWith("2026-11-10,\"Bar \"\"do Zé\"\"\",Food,"))
    }

    @Test
    fun `GIVEN a description with a line break WHEN writing the CSV THEN the field is quoted keeping the line break`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                    description = "Line one\nLine two",
                ),
            ),
        )

        // When
        val text = csvOf(encoder.encode(export, words))

        // Then
        assertTrue(text.contains("2026-11-10,\"Line one\nLine two\",Food,"))
    }

    @Test
    fun `GIVEN descriptions that look like formulas WHEN writing the CSV THEN each gets a leading apostrophe`() {
        // Given
        val descriptions = listOf("=SUM(A1)", "+55 lunch", "-tip", "@cmd", "\tTab")
        val export = exportOf(
            bills = descriptions.mapIndexed { index, description ->
                exportBill(
                    id = "bill-$index",
                    amountMinor = 100L,
                    description = description,
                    spentAt = NOV_10_NOON + index,
                )
            },
        )

        // When
        val lines = linesOf(encoder.encode(export, words))

        // Then
        assertEquals(
            listOf("'=SUM(A1)", "'+55 lunch", "'-tip", "'@cmd", "'\tTab"),
            lines.subList(2, 7).map { it.split(",")[1] },
        )
    }

    @Test
    fun `GIVEN a description opening with CR WHEN writing the CSV THEN it gets an apostrophe inside quotes`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                    description = "\r=1+1",
                ),
            ),
        )

        // When
        val text = csvOf(encoder.encode(export, words))

        // Then
        assertTrue(text.contains("2026-11-10,\"'\r=1+1\",Food,"))
    }

    @Test
    fun `GIVEN a member named like a formula WHEN writing the CSV THEN the name gets a leading apostrophe`() {
        // Given
        val group = exportGroup(members = listOf(exportMember(PIERRE), exportMember(ANA, name = "=HYPERLINK(1)")))
        val export = exportOf(
            group = group,
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                ),
            ),
        )

        // When
        val lines = linesOf(encoder.encode(export, words))

        // Then
        assertTrue("'=HYPERLINK(1),BRL,-1.00" in lines)
    }

    @Test
    fun `GIVEN a negative balance WHEN writing the CSV THEN the amount gets no apostrophe`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 1_000L,
                ),
            ),
        )

        // When
        val lines = linesOf(encoder.encode(export, words))

        // Then
        assertTrue("Ana,BRL,-10.00" in lines)
    }

    @Test
    fun `GIVEN a category named like a formula WHEN writing the CSV THEN it gets a leading apostrophe`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                    categoryId = "evil",
                ),
            ),
            groupCategories = listOf(
                groupCategory(
                    id = "evil",
                    name = "+cmd",
                ),
            ),
        )

        // When
        val lines = linesOf(encoder.encode(export, words))

        // Then
        assertTrue(lines[2].startsWith("2026-11-10,Dinner,'+cmd,"))
    }

    private fun csvOf(bytes: ByteArray): String = bytes.decodeToString().removePrefix(Char(0xFEFF).toString())

    private fun linesOf(bytes: ByteArray): List<String> = csvOf(bytes).split("\r\n")
}
