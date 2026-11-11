package com.quare.blitzsplit.core.legal.data.parser

import com.quare.blitzsplit.core.legal.domain.model.LegalBlock
import com.quare.blitzsplit.core.legal.domain.model.LegalDate
import com.quare.blitzsplit.core.legal.domain.model.LegalDocument
import com.quare.blitzsplit.core.legal.domain.model.LegalSpan
import com.quare.blitzsplit.core.legal.domain.model.LegalTableDetail
import com.quare.blitzsplit.core.legal.domain.model.LegalTableRow
import com.quare.blitzsplit.core.legal.domain.model.LegalText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LegalDocumentParserImplTest {
    private val parser = LegalDocumentParserImpl()

    @Test
    fun `GIVEN a title and a date in the front matter WHEN parsing THEN the document has that title and that day`() {
        // Given
        val markdown = "---\ntitle: Privacy Policy\nupdated: 2026-11-05\n---\n\nWe keep little."

        // When
        val document = parser.parse(markdown)

        // Then
        assertEquals(
            LegalDocument(
                title = "Privacy Policy",
                updated = LegalDate(
                    year = 2026,
                    month = 11,
                    day = 5,
                ),
                blocks = listOf(LegalBlock.Paragraph(plain("We keep little."))),
            ),
            document,
        )
    }

    @Test
    fun `GIVEN a valid front matter and no body WHEN parsing THEN the document has no blocks`() {
        // Given
        val markdown = "---\ntitle: Terms of Service\nupdated: 2026-01-31\n---\n"

        // When
        val document = parser.parse(markdown)

        // Then
        assertEquals(
            LegalDocument(
                title = "Terms of Service",
                updated = LegalDate(2026, 1, 31),
                blocks = emptyList(),
            ),
            document,
        )
    }

    @Test
    fun `GIVEN windows line endings WHEN parsing THEN reads the same document`() {
        // Given
        val markdown = "---\r\ntitle: Privacy Policy\r\nupdated: 2026-11-05\r\n---\r\n\r\nOne.\r\n\r\nTwo.\r\n"

        // When
        val document = parser.parse(markdown)

        // Then
        assertEquals("Privacy Policy", document?.title)
        assertEquals(
            listOf(LegalBlock.Paragraph(plain("One.")), LegalBlock.Paragraph(plain("Two."))),
            document?.blocks,
        )
    }

    @Test
    fun `GIVEN a title that holds a colon WHEN parsing THEN keeps the whole title`() {
        // Given
        val markdown = "---\ntitle: Privacy: how we treat your data\nupdated: 2026-11-05\n---\n"

        // When
        val document = parser.parse(markdown)

        // Then
        assertEquals("Privacy: how we treat your data", document?.title)
    }

    @Test
    fun `GIVEN no front matter WHEN parsing THEN there is no document`() {
        // Given
        val markdown = "## Who we are {#who}\n\nWe are Blitz Split."

        // When
        val document = parser.parse(markdown)

        // Then
        assertNull(document)
    }

    @Test
    fun `GIVEN an html error page WHEN parsing THEN there is no document`() {
        // Given
        val markdown = "<!doctype html>\n<html><body>Bad gateway</body></html>"

        // When
        val document = parser.parse(markdown)

        // Then
        assertNull(document)
    }

    @Test
    fun `GIVEN an empty text WHEN parsing THEN there is no document`() {
        // Given
        val markdown = ""

        // When
        val document = parser.parse(markdown)

        // Then
        assertNull(document)
    }

    @Test
    fun `GIVEN a front matter that never closes WHEN parsing THEN there is no document`() {
        // Given
        val markdown = "---\ntitle: Privacy Policy\nupdated: 2026-11-05\n\nWe keep little."

        // When
        val document = parser.parse(markdown)

        // Then
        assertNull(document)
    }

    @Test
    fun `GIVEN a front matter without a title WHEN parsing THEN there is no document`() {
        // Given
        val markdown = "---\nupdated: 2026-11-05\n---\n\nWe keep little."

        // When
        val document = parser.parse(markdown)

        // Then
        assertNull(document)
    }

    @Test
    fun `GIVEN a front matter with an empty title WHEN parsing THEN there is no document`() {
        // Given
        val markdown = "---\ntitle:\nupdated: 2026-11-05\n---\n\nWe keep little."

        // When
        val document = parser.parse(markdown)

        // Then
        assertNull(document)
    }

    @Test
    fun `GIVEN a front matter without the updated date WHEN parsing THEN there is no document`() {
        // Given
        val markdown = "---\ntitle: Privacy Policy\n---\n\nWe keep little."

        // When
        val document = parser.parse(markdown)

        // Then
        assertNull(document)
    }

    @Test
    fun `GIVEN an updated date written in words WHEN parsing THEN there is no document`() {
        // Given
        val markdown = "---\ntitle: Privacy Policy\nupdated: November 5, 2026\n---\n"

        // When
        val document = parser.parse(markdown)

        // Then
        assertNull(document)
    }

    @Test
    fun `GIVEN an updated date without leading zeros WHEN parsing THEN there is no document`() {
        // Given
        val markdown = "---\ntitle: Privacy Policy\nupdated: 2026-1-5\n---\n"

        // When
        val document = parser.parse(markdown)

        // Then
        assertNull(document)
    }

    @Test
    fun `GIVEN an updated date in month 13 WHEN parsing THEN there is no document`() {
        // Given
        val markdown = "---\ntitle: Privacy Policy\nupdated: 2026-13-05\n---\n"

        // When
        val document = parser.parse(markdown)

        // Then
        assertNull(document)
    }

    @Test
    fun `GIVEN an updated date on day 32 WHEN parsing THEN there is no document`() {
        // Given
        val markdown = "---\ntitle: Privacy Policy\nupdated: 2026-11-32\n---\n"

        // When
        val document = parser.parse(markdown)

        // Then
        assertNull(document)
    }

    @Test
    fun `GIVEN an updated date on day zero WHEN parsing THEN there is no document`() {
        // Given
        val markdown = "---\ntitle: Privacy Policy\nupdated: 2026-11-00\n---\n"

        // When
        val document = parser.parse(markdown)

        // Then
        assertNull(document)
    }

    @Test
    fun `GIVEN sections with anchors WHEN parsing THEN numbers them in order and keeps each anchor`() {
        // Given
        val markdown = document("## Who we are {#who}\n\n## What we keep {#keep}\n\n## Your rights {#your-rights-2}")

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        assertEquals(
            listOf(
                LegalBlock.Section(
                    number = 1,
                    anchor = "who",
                    title = plain("Who we are"),
                ),
                LegalBlock.Section(
                    number = 2,
                    anchor = "keep",
                    title = plain("What we keep"),
                ),
                LegalBlock.Section(
                    number = 3,
                    anchor = "your-rights-2",
                    title = plain("Your rights"),
                ),
            ),
            blocks,
        )
    }

    @Test
    fun `GIVEN a section without an anchor WHEN parsing THEN it still shows and counts as a section`() {
        // Given
        val markdown = document("## Who we are\n\n## What we keep {#keep}")

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        assertEquals(
            listOf(
                LegalBlock.Section(
                    number = 1,
                    anchor = "",
                    title = plain("Who we are"),
                ),
                LegalBlock.Section(
                    number = 2,
                    anchor = "keep",
                    title = plain("What we keep"),
                ),
            ),
            blocks,
        )
    }

    @Test
    fun `GIVEN a third level title WHEN parsing THEN it is a heading and not a section`() {
        // Given
        val markdown = document("## Data {#data}\n\n### On your device\n\n## Rights {#rights}")

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        assertEquals(
            listOf(
                LegalBlock.Section(
                    number = 1,
                    anchor = "data",
                    title = plain("Data"),
                ),
                LegalBlock.Heading(plain("On your device")),
                LegalBlock.Section(
                    number = 2,
                    anchor = "rights",
                    title = plain("Rights"),
                ),
            ),
            blocks,
        )
    }

    @Test
    fun `GIVEN lines separated by blank lines WHEN parsing THEN each line is one paragraph`() {
        // Given
        val markdown = document("The first paragraph.\n\nThe second paragraph.\n\n\nThe third paragraph.")

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        assertEquals(
            listOf(
                LegalBlock.Paragraph(plain("The first paragraph.")),
                LegalBlock.Paragraph(plain("The second paragraph.")),
                LegalBlock.Paragraph(plain("The third paragraph.")),
            ),
            blocks,
        )
    }

    @Test
    fun `GIVEN a paragraph wrapped over two lines WHEN parsing THEN it is one paragraph joined by a space`() {
        // Given
        val markdown = document("The first line\nand its second line.")

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        assertEquals(listOf(LegalBlock.Paragraph(plain("The first line and its second line."))), blocks)
    }

    @Test
    fun `GIVEN consecutive bullet lines WHEN parsing THEN they are one list`() {
        // Given
        val markdown = document("- Your name\n- Your photo\n- Your groups")

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        assertEquals(
            listOf(LegalBlock.Bullets(listOf(plain("Your name"), plain("Your photo"), plain("Your groups")))),
            blocks,
        )
    }

    @Test
    fun `GIVEN bullet lines split by a blank line WHEN parsing THEN they are two lists`() {
        // Given
        val markdown = document("- Your name\n\n- Your photo")

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        assertEquals(
            listOf(
                LegalBlock.Bullets(listOf(plain("Your name"))),
                LegalBlock.Bullets(listOf(plain("Your photo"))),
            ),
            blocks,
        )
    }

    @Test
    fun `GIVEN a list right under a paragraph WHEN parsing THEN the paragraph and the list are separate blocks`() {
        // Given
        val markdown = document("We keep:\n- Your name\n- Your photo\nNothing else.")

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        assertEquals(
            listOf(
                LegalBlock.Paragraph(plain("We keep:")),
                LegalBlock.Bullets(listOf(plain("Your name"), plain("Your photo"))),
                LegalBlock.Paragraph(plain("Nothing else.")),
            ),
            blocks,
        )
    }

    @Test
    fun `GIVEN a callout with a title and a paragraph WHEN parsing THEN it is one callout with that paragraph`() {
        // Given
        val markdown = document("> **Note**\n>\n> You can delete your account at any time.")

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        val callout = blocks?.singleOrNull() as? LegalBlock.Callout
        assertNotNull(callout)
        assertEquals("Note", callout.title.plain)
        assertEquals(plain("You can delete your account at any time."), callout.paragraph)
        assertEquals(emptyList(), callout.items)
    }

    @Test
    fun `GIVEN a callout with a title and a list WHEN parsing THEN it is one callout with the items alone`() {
        // Given
        val markdown = document("> **In short**\n>\n> - The app works offline\n> - An account is optional")

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        val callout = blocks?.singleOrNull() as? LegalBlock.Callout
        assertNotNull(callout)
        assertEquals("In short", callout.title.plain)
        assertNull(callout.paragraph)
        assertEquals(listOf(plain("The app works offline"), plain("An account is optional")), callout.items)
    }

    @Test
    fun `GIVEN a callout with only its title WHEN parsing THEN it has no paragraph and no items`() {
        // Given
        val markdown = document("> **Note**")

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        val callout = blocks?.singleOrNull() as? LegalBlock.Callout
        assertNotNull(callout)
        assertEquals("Note", callout.title.plain)
        assertNull(callout.paragraph)
        assertEquals(emptyList(), callout.items)
    }

    @Test
    fun `GIVEN two callouts split by a blank line WHEN parsing THEN they are two callouts`() {
        // Given
        val markdown = document("> **First**\n>\n> One.\n\n> **Second**\n>\n> Two.")

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        assertEquals(
            listOf("First" to "One.", "Second" to "Two."),
            blocks?.map { block -> (block as LegalBlock.Callout).let { it.title.plain to it.paragraph?.plain } },
        )
    }

    @Test
    fun `GIVEN a table WHEN parsing THEN it has the header cells and one row for each body line`() {
        // Given
        val markdown = document(
            "| Purpose | Data | Legal basis |\n" +
                "|---|---|---|\n" +
                "| Run the app | Name, photo | Contract |\n" +
                "| Keep it safe | Device id | Legitimate interest |",
        )

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        assertEquals(
            listOf(
                LegalBlock.Table(
                    header = listOf(plain("Purpose"), plain("Data"), plain("Legal basis")),
                    rows = listOf(
                        LegalTableRow(listOf(plain("Run the app"), plain("Name, photo"), plain("Contract"))),
                        LegalTableRow(listOf(plain("Keep it safe"), plain("Device id"), plain("Legitimate interest"))),
                    ),
                ),
            ),
            blocks,
        )
    }

    @Test
    fun `GIVEN a table row with an empty cell WHEN listing the details of the row THEN the empty cell is left out`() {
        // Given
        val markdown = document("| Purpose | Data | Legal basis |\n|---|---|---|\n| Run the app | | Contract |")
        val table = parser.parse(markdown)?.blocks?.singleOrNull() as? LegalBlock.Table

        // When
        val details = table?.detailsOf(table.rows.single())

        // Then
        assertEquals(
            listOf(
                LegalTableDetail(
                    label = plain("Legal basis"),
                    text = plain("Contract"),
                ),
            ),
            details,
        )
    }

    @Test
    fun `GIVEN a text saved with a byte order mark and a blank first line WHEN parsing THEN it still reads`() {
        // Given
        val markdown = "\uFEFF\n" + document("Hello.")

        // When
        val title = parser.parse(markdown)?.title

        // Then
        assertEquals("Privacy Policy", title)
    }

    @Test
    fun `GIVEN a separator row with spaces and alignment colons WHEN parsing THEN the separator is not a row`() {
        // Given
        val markdown = document("| Purpose | Data |\n| :--- | ---: |\n| Run the app | Name |")

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        assertEquals(
            listOf(
                LegalBlock.Table(
                    header = listOf(plain("Purpose"), plain("Data")),
                    rows = listOf(LegalTableRow(listOf(plain("Run the app"), plain("Name")))),
                ),
            ),
            blocks,
        )
    }

    @Test
    fun `GIVEN a table cell in bold WHEN parsing THEN the cell keeps the bold`() {
        // Given
        val markdown = document("| Purpose | Data |\n|---|---|\n| **Run the app** | Name |")

        // When
        val table = parser.parse(markdown)?.blocks?.singleOrNull() as? LegalBlock.Table

        // Then
        assertEquals(
            LegalText(
                listOf(
                    LegalSpan(
                        text = "Run the app",
                        isBold = true,
                        link = null,
                    ),
                ),
            ),
            table
                ?.rows
                ?.single()
                ?.cells
                ?.first(),
        )
    }

    @Test
    fun `GIVEN a table row shorter than the header WHEN parsing THEN the row is filled with empty cells`() {
        // Given
        val markdown = document("| Purpose | Data | Legal basis |\n|---|---|---|\n| Run the app |")

        // When
        val table = parser.parse(markdown)?.blocks?.singleOrNull() as? LegalBlock.Table

        // Then
        assertEquals(
            listOf("Run the app", "", ""),
            table
                ?.rows
                ?.single()
                ?.cells
                ?.map { it.plain },
        )
    }

    @Test
    fun `GIVEN a table row longer than the header WHEN parsing THEN the row keeps only the cells of the header`() {
        // Given
        val markdown = document("| Purpose | Data |\n|---|---|\n| Run the app | Name | Contract |")

        // When
        val table = parser.parse(markdown)?.blocks?.singleOrNull() as? LegalBlock.Table

        // Then
        assertEquals(
            listOf("Run the app", "Name"),
            table
                ?.rows
                ?.single()
                ?.cells
                ?.map { it.plain },
        )
    }

    @Test
    fun `GIVEN every kind of block in one text WHEN parsing THEN reads them in order`() {
        // Given
        val markdown = document(
            """
            > **In short**
            >
            > - The app works offline

            ## What we keep {#keep}

            We keep little.

            ### On your device

            - Your groups
            - Your bills

            | Purpose | Data |
            |---|---|
            | Run the app | Name |

            > **Note**
            >
            > Nothing is sold.

            ## Contact {#contact}

            Write to us.
            """.trimIndent(),
        )

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        assertEquals(
            listOf(
                "Callout",
                "Section 1 keep",
                "Paragraph",
                "Heading",
                "Bullets 2",
                "Table 1",
                "Callout",
                "Section 2 contact",
                "Paragraph",
            ),
            blocks?.map(::describe),
        )
    }

    @Test
    fun `GIVEN lines of Markdown the app does not know WHEN parsing THEN each one reads as a paragraph`() {
        // Given
        val markdown =
            document("# A first level title\n\n#### A fourth level title\n\n1. A numbered item\n\n-not a bullet\n\n---")

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        assertEquals(
            listOf(
                LegalBlock.Paragraph(plain("# A first level title")),
                LegalBlock.Paragraph(plain("#### A fourth level title")),
                LegalBlock.Paragraph(plain("1. A numbered item")),
                LegalBlock.Paragraph(plain("-not a bullet")),
                LegalBlock.Paragraph(plain("---")),
            ),
            blocks,
        )
    }

    @Test
    fun `GIVEN block marks with nothing after them WHEN parsing THEN it still gives a document`() {
        // Given
        val markdown =
            document("## \n\n### \n\n- \n\n>\n\n|\n\n||\n\n|---|\n\n> - \n\n**\n\n[]()\n\n<mark></mark>\n\n## {#}")

        // When
        val document = parser.parse(markdown)

        // Then
        assertNotNull(document)
    }

    private fun describe(block: LegalBlock): String = when (block) {
        is LegalBlock.Section -> "Section ${block.number} ${block.anchor}"
        is LegalBlock.Heading -> "Heading"
        is LegalBlock.Paragraph -> "Paragraph"
        is LegalBlock.Bullets -> "Bullets ${block.items.size}"
        is LegalBlock.Callout -> "Callout"
        is LegalBlock.Table -> "Table ${block.rows.size}"
    }

    private fun document(body: String): String = "---\ntitle: Privacy Policy\nupdated: 2026-11-05\n---\n\n$body\n"

    private fun plain(text: String): LegalText = LegalText(
        listOf(
            LegalSpan(
                text = text,
                isBold = false,
                link = null,
            ),
        ),
    )
}
