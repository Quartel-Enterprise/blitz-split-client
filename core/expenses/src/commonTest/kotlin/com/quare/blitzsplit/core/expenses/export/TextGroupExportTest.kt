package com.quare.blitzsplit.core.expenses.export

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextGroupExportTest {
    private val encoder = TextGroupExport()
    private val words = exportWords()

    @Test
    fun `GIVEN an export on the 14th WHEN writing the text THEN it opens with the group name and the day`() {
        // Given
        val export = exportOf(nowMillis = NOV_14_AFTERNOON)

        // When
        val lines = encoder.encode(export, words).decodeToString().lines()

        // Then
        assertEquals(listOf("Trip to Rio", "2026-11-14"), lines.take(2))
    }

    @Test
    fun `GIVEN a bill and a payment WHEN writing the text THEN each section opens with its title then its headers`() {
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
        val lines = encoder.encode(export, words).decodeToString().lines()

        // Then
        val titles = listOf("Bills", "Payments", "Balances").map { lines.indexOf(it) }
        assertTrue(titles.all { it > 1 }, "Titles at $titles in $lines")
        assertEquals(titles.sorted(), titles)
        assertTrue(lines[titles[0] + 1].startsWith("Date"))
        assertTrue(lines[titles[1] + 1].startsWith("Date"))
        assertTrue(lines[titles[2] + 1].startsWith("Member"))
    }

    @Test
    fun `GIVEN payments of different lengths WHEN writing the text THEN the amounts end on the same column`() {
        // Given
        val export = exportOf(
            payments = listOf(
                exportPayment(
                    id = "small",
                    amountMinor = 500L,
                    from = PIERRE,
                    to = ANA,
                    paidAt = NOV_10_NOON,
                ),
                exportPayment(
                    id = "large",
                    amountMinor = 1_234_550L,
                    from = ANA,
                    to = PIERRE,
                    paidAt = NOV_11_NOON,
                ),
            ),
        )

        // When
        val lines = encoder.encode(export, words).decodeToString().lines()

        // Then
        val small = lines.first { it.startsWith("2026-11-10") }
        val large = lines.first { it.startsWith("2026-11-11") }
        assertEquals(small.indexOf("5.00") + "5.00".length, large.indexOf("12345.50") + "12345.50".length)
    }

    @Test
    fun `GIVEN payments of different lengths WHEN writing the text THEN the names start on the same column`() {
        // Given
        val export = exportOf(
            payments = listOf(
                exportPayment(
                    id = "small",
                    amountMinor = 500L,
                    from = PIERRE,
                    to = ANA,
                    paidAt = NOV_10_NOON,
                ),
                exportPayment(
                    id = "large",
                    amountMinor = 1_234_550L,
                    from = ANA,
                    to = PIERRE,
                    paidAt = NOV_11_NOON,
                ),
            ),
        )

        // When
        val lines = encoder.encode(export, words).decodeToString().lines()

        // Then
        val small = lines.first { it.startsWith("2026-11-10") }
        val large = lines.first { it.startsWith("2026-11-11") }
        assertEquals(small.indexOf("Ana"), large.indexOf("Pierre"))
        assertTrue(small.indexOf("Pierre") < small.indexOf("Ana"))
    }

    @Test
    fun `GIVEN a description with a line break WHEN writing the text THEN the row stays on one line`() {
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
        val lines = encoder.encode(export, words).decodeToString().lines()

        // Then
        assertTrue(lines.any { it.startsWith("2026-11-10") && it.contains("Line one Line two") })
    }
}
