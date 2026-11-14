package com.quare.blitzsplit.core.expenses.export

import com.quare.blitzsplit.core.model.GroupCurrency
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: reads the workbook back with java.util.zip and the JDK's XML parser, which commonTest doesn't have.
class XlsxGroupExportTest {
    private val encoder = XlsxGroupExport()
    private val words = exportWords()

    @Test
    fun `GIVEN an export WHEN writing the workbook THEN the zip holds the workbook parts and one sheet per section`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                ),
            ),
        )

        // When
        val parts = partsOf(encoder.encode(export, words))

        // Then
        assertEquals(
            setOf(
                "[Content_Types].xml",
                "_rels/.rels",
                "xl/workbook.xml",
                "xl/_rels/workbook.xml.rels",
                "xl/styles.xml",
                "xl/worksheets/sheet1.xml",
                "xl/worksheets/sheet2.xml",
                "xl/worksheets/sheet3.xml",
            ),
            parts.keys,
        )
    }

    @Test
    fun `GIVEN an export WHEN writing the workbook THEN every part is stored with its right CRC-32`() {
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
        val zip = encoder.encode(export, words)

        // Then
        ZipInputStream(ByteArrayInputStream(zip)).use { stream ->
            generateSequence { stream.nextEntry }.forEach { entry ->
                // Why: ZipInputStream itself fails on a stored entry whose CRC-32 doesn't match its bytes.
                val data = stream.readBytes()
                assertEquals(ZipEntry.STORED, entry.method, entry.name)
                assertEquals(CRC32().apply { update(data) }.value, entry.crc, entry.name)
                assertEquals(data.size.toLong(), entry.size, entry.name)
            }
        }
    }

    @Test
    fun `GIVEN text with XML characters WHEN writing the workbook THEN every part is well formed XML`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                    description = "Fish & <Chips> \"x\" 'y'",
                ),
            ),
        )

        // When
        val parts = partsOf(encoder.encode(export, words))

        // Then
        parts.forEach { (name, data) ->
            val parsed = runCatching { xmlOf(data) }
            assertTrue(parsed.isSuccess, "$name: ${parsed.exceptionOrNull()}")
        }
    }

    @Test
    fun `GIVEN the UI words WHEN writing the workbook THEN the sheets are named after the sections`() {
        // Given
        val export = exportOf()
        val portuguese = exportWords(
            bills = "Contas",
            payments = "Pagamentos",
            balances = "Saldos",
        )

        // When
        val parts = partsOf(encoder.encode(export, portuguese))

        // Then
        assertEquals(listOf("Contas", "Pagamentos", "Saldos"), sheetNamesOf(parts))
    }

    @Test
    fun `GIVEN names with characters Excel refuses WHEN writing the workbook THEN sheet names drop them`() {
        // Given
        val export = exportOf()
        val refused = exportWords(
            bills = "Bills: 2026/11 [all]?*",
            payments = "Pay\\ments",
        )

        // When
        val parts = partsOf(encoder.encode(export, refused))

        // Then
        assertEquals(listOf("Bills 202611 all", "Payments", "Balances"), sheetNamesOf(parts))
    }

    @Test
    fun `GIVEN a section name longer than 31 characters WHEN writing the workbook THEN its sheet name is cut at 31`() {
        // Given
        val export = exportOf()
        val long = exportWords(bills = "Bills of the trip to Rio de Janeiro in November")

        // When
        val parts = partsOf(encoder.encode(export, long))

        // Then
        assertEquals("Bills of the trip to Rio de Jan", sheetNamesOf(parts).first())
        assertEquals(31, sheetNamesOf(parts).first().length)
    }

    @Test
    fun `GIVEN names differing only in case WHEN writing the workbook THEN the sheet names stay unique`() {
        // Given
        val export = exportOf()
        val clashing = exportWords(
            bills = "Money",
            payments = "money",
            balances = "MONEY",
        )

        // When
        val parts = partsOf(encoder.encode(export, clashing))

        // Then
        val names = sheetNamesOf(parts)
        assertEquals(3, names.map { it.lowercase() }.toSet().size, "$names")
        assertEquals("Money", names.first())
        assertTrue(names.all { it.length <= 31 })
    }

    @Test
    fun `GIVEN two long clashing names WHEN writing the workbook THEN unique names still fit in 31 characters`() {
        // Given
        val export = exportOf()
        val title = "A section name that is far too long for Excel"
        val clashing = exportWords(
            bills = title,
            payments = title,
        )

        // When
        val parts = partsOf(encoder.encode(export, clashing))

        // Then
        val names = sheetNamesOf(parts)
        assertEquals(3, names.map { it.lowercase() }.toSet().size, "$names")
        assertTrue(names.all { it.length <= 31 }, "$names")
    }

    @Test
    fun `GIVEN an export WHEN writing the workbook THEN each sheet freezes its header row`() {
        // Given
        val export = exportOf()

        // When
        val parts = partsOf(encoder.encode(export, words))

        // Then
        (1..3).forEach { sheet ->
            val pane = xmlOf(parts.getValue("xl/worksheets/sheet$sheet.xml")).elements("pane").single()
            assertEquals("1", pane.getAttribute("ySplit"))
            assertEquals("frozen", pane.getAttribute("state"))
            assertEquals("A2", pane.getAttribute("topLeftCell"))
        }
    }

    @Test
    fun `GIVEN an export WHEN writing the workbook THEN the header cells use a bold font`() {
        // Given
        val export = exportOf()

        // When
        val parts = partsOf(encoder.encode(export, words))

        // Then
        val header = rowOf(
            parts,
            sheet = 1,
            row = 1,
        )
        assertEquals("Date", textOf(header.getValue("A1")))
        val style = header.getValue("A1").getAttribute("s").toInt()
        assertTrue(header.values.all { it.getAttribute("s").toInt() == style })
        val styles = xmlOf(parts.getValue("xl/styles.xml"))
        val fontId = styles
            .elements("cellXfs")
            .single()
            .childElements("xf")[style]
            .getAttribute("fontId")
            .toInt()
        val font = styles.elements("fonts").single().childElements("font")[fontId]
        assertEquals(1, font.childElements("b").size)
    }

    @Test
    fun `GIVEN a bill on 2026-11-10 WHEN writing the workbook THEN its date is the Excel serial in date format`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                    spentAt = NOV_10_NOON,
                ),
            ),
        )

        // When
        val parts = partsOf(encoder.encode(export, words))

        // Then
        val date = rowOf(
            parts,
            sheet = 1,
            row = 2,
        ).getValue("A2")
        assertEquals("", date.getAttribute("t"))
        assertEquals("46336", date.childElements("v").single().textContent)
        val style = date.getAttribute("s").toInt()
        val styles = xmlOf(parts.getValue("xl/styles.xml"))
        val numberFormat = styles
            .elements("cellXfs")
            .single()
            .childElements("xf")[style]
            .getAttribute("numFmtId")
        assertEquals("14", numberFormat)
    }

    @Test
    fun `GIVEN a bill of 120 reais and 50 centavos WHEN writing the workbook THEN its amount is a numeric cell`() {
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
        val parts = partsOf(encoder.encode(export, words))

        // Then
        val amount = rowOf(
            parts,
            sheet = 1,
            row = 2,
        ).getValue("D2")
        assertEquals("", amount.getAttribute("t"))
        assertEquals("120.50", amount.childElements("v").single().textContent)
    }

    @Test
    fun `GIVEN a negative balance in yen WHEN writing the workbook THEN it is a numeric cell with no decimals`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "ramen",
                    amountMinor = 1_500L,
                    currency = GroupCurrency.JPY,
                ),
            ),
        )

        // When
        val parts = partsOf(encoder.encode(export, words))

        // Then
        val sheet = xmlOf(parts.getValue("xl/worksheets/sheet3.xml"))
        val values = sheet.elements("c").filter { it.getAttribute("t") != "inlineStr" }.map { cell ->
            cell.childElements("v").single().textContent
        }
        assertTrue("-1500" in values, "$values")
    }

    @Test
    fun `GIVEN XML and control characters WHEN writing the workbook THEN the cell is escaped inline text`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                    description = "Fish & <Chips>\u0001 \"x\"",
                ),
            ),
        )

        // When
        val parts = partsOf(encoder.encode(export, words))

        // Then
        val description = rowOf(
            parts,
            sheet = 1,
            row = 2,
        ).getValue("B2")
        assertEquals("inlineStr", description.getAttribute("t"))
        assertEquals("Fish & <Chips> \"x\"", textOf(description))
    }

    @Test
    fun `GIVEN a member out of a bill WHEN writing the workbook THEN the member cells are left out`() {
        // Given
        val export = exportOf(
            bills = listOf(
                exportBill(
                    id = "dinner",
                    amountMinor = 100L,
                ),
            ),
        )

        // When
        val parts = partsOf(encoder.encode(export, words))

        // Then
        val header = rowOf(
            parts,
            sheet = 1,
            row = 1,
        ).mapValues { (_, cell) -> textOf(cell) }
        val bruno = header.filterValues { it == "Paid · Bruno" || it == "Share · Bruno" }.keys
        val row = rowOf(
            parts,
            sheet = 1,
            row = 2,
        )
        assertEquals(2, bruno.size)
        bruno.forEach { reference -> assertNull(row[reference.replace("1", "2")], reference) }
    }

    @Test
    fun `GIVEN no bills nor payments WHEN writing the workbook THEN those sheets have only the header row`() {
        // Given
        val export = exportOf()

        // When
        val parts = partsOf(encoder.encode(export, words))

        // Then
        listOf(1, 2).forEach { sheet ->
            val rows = xmlOf(parts.getValue("xl/worksheets/sheet$sheet.xml")).elements("row")
            assertEquals(listOf("1"), rows.map { it.getAttribute("r") })
        }
    }

    private fun partsOf(zip: ByteArray): Map<String, ByteArray> =
        ZipInputStream(ByteArrayInputStream(zip)).use { stream ->
            generateSequence { stream.nextEntry }.associate { it.name to stream.readBytes() }
        }

    private fun xmlOf(data: ByteArray): Document = DocumentBuilderFactory
        .newInstance()
        .apply { isNamespaceAware = true }
        .newDocumentBuilder()
        .parse(ByteArrayInputStream(data))

    private fun sheetNamesOf(parts: Map<String, ByteArray>): List<String> =
        xmlOf(parts.getValue("xl/workbook.xml")).elements("sheet").map { it.getAttribute("name") }

    // Why: a row's cells by their reference ("A2"), as the sheet writes them.
    private fun rowOf(
        parts: Map<String, ByteArray>,
        sheet: Int,
        row: Int,
    ): Map<String, Element> = xmlOf(parts.getValue("xl/worksheets/sheet$sheet.xml"))
        .elements("row")
        .single { it.getAttribute("r") == row.toString() }
        .childElements("c")
        .associateBy { it.getAttribute("r") }

    private fun textOf(cell: Element): String = cell.getElementsByTagNameNS("*", "t").item(0).textContent

    private fun Document.elements(name: String): List<Element> = getElementsByTagNameNS("*", name).let { nodes ->
        (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun Element.childElements(name: String): List<Element> = (0 until childNodes.length)
        .map { childNodes.item(it) }
        .filterIsInstance<Element>()
        .filter { it.localName == name }
}
