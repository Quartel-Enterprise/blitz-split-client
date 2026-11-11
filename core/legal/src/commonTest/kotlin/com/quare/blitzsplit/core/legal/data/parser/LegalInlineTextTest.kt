package com.quare.blitzsplit.core.legal.data.parser

import com.quare.blitzsplit.core.legal.domain.model.LegalBlock
import com.quare.blitzsplit.core.legal.domain.model.LegalLink
import com.quare.blitzsplit.core.legal.domain.model.LegalSpan
import com.quare.blitzsplit.core.legal.domain.model.LegalText
import com.quare.blitzsplit.core.model.LegalPage
import kotlin.test.Test
import kotlin.test.assertEquals

class LegalInlineTextTest {
    private val parser = LegalDocumentParserImpl()

    @Test
    fun `GIVEN bold words in a paragraph WHEN parsing THEN only those words are bold`() {
        // Given
        val line = "We **never sell** your data."

        // When
        val spans = paragraphSpans(line)

        // Then
        assertEquals(listOf(span("We "), span("never sell", isBold = true), span(" your data.")), spans)
    }

    @Test
    fun `GIVEN a link to the terms WHEN parsing THEN the words open the terms in the app`() {
        // Given
        val line = "Read the [Terms of Service](/terms)."

        // When
        val spans = paragraphSpans(line)

        // Then
        assertEquals(
            listOf(
                span("Read the "),
                span(
                    "Terms of Service",
                    link = LegalLink.Page(
                        page = LegalPage.TERMS_OF_USE,
                        anchor = null,
                    ),
                ),
                span("."),
            ),
            spans,
        )
    }

    @Test
    fun `GIVEN a link to the privacy policy WHEN parsing THEN the words open the privacy policy in the app`() {
        // Given
        val line = "[Privacy Policy](/privacy)"

        // When
        val spans = paragraphSpans(line)

        // Then
        assertEquals(
            listOf(
                span(
                    "Privacy Policy",
                    link = LegalLink.Page(
                        page = LegalPage.PRIVACY_POLICY,
                        anchor = null,
                    ),
                ),
            ),
            spans,
        )
    }

    @Test
    fun `GIVEN a link to a section of the privacy policy WHEN parsing THEN the link names the page and the anchor`() {
        // Given
        val line = "See [what we keep](/privacy#what-we-keep)."

        // When
        val spans = paragraphSpans(line)

        // Then
        assertEquals(
            listOf(
                span("See "),
                span(
                    "what we keep",
                    link = LegalLink.Page(
                        page = LegalPage.PRIVACY_POLICY,
                        anchor = "what-we-keep",
                    ),
                ),
                span("."),
            ),
            spans,
        )
    }

    @Test
    fun `GIVEN an https link WHEN parsing THEN it is an external link with the whole address`() {
        // Given
        val line = "Hosted by [Supabase](https://supabase.com/privacy?lang=en#data)."

        // When
        val spans = paragraphSpans(line)

        // Then
        assertEquals(
            listOf(
                span("Hosted by "),
                span("Supabase", link = LegalLink.External("https://supabase.com/privacy?lang=en#data")),
                span("."),
            ),
            spans,
        )
    }

    @Test
    fun `GIVEN a link to a legal page by its full address WHEN parsing THEN it is that page and section`() {
        // Given
        val line = "See [what we keep](https://blitzsplit.com/privacy#keep)."

        // When
        val spans = paragraphSpans(line)

        // Then
        val link = LegalLink.Page(
            page = LegalPage.PRIVACY_POLICY,
            anchor = "keep",
        )
        assertEquals(
            listOf(
                span("See "),
                span("what we keep", link = link),
                span("."),
            ),
            spans,
        )
    }

    @Test
    fun `GIVEN a link to another page of the site WHEN parsing THEN it is an external link`() {
        // Given
        val line = "[Get the app](https://blitzsplit.com/download)"

        // When
        val spans = paragraphSpans(line)

        // Then
        assertEquals(
            listOf(span("Get the app", link = LegalLink.External("https://blitzsplit.com/download"))),
            spans,
        )
    }

    @Test
    fun `GIVEN a mailto link WHEN parsing THEN it is an external link`() {
        // Given
        val line = "Write to [privacy@blitzsplit.com](mailto:privacy@blitzsplit.com)"

        // When
        val spans = paragraphSpans(line)

        // Then
        assertEquals(
            listOf(
                span("Write to "),
                span("privacy@blitzsplit.com", link = LegalLink.External("mailto:privacy@blitzsplit.com")),
            ),
            spans,
        )
    }

    @Test
    fun `GIVEN a link to a site page that is not a legal text WHEN parsing THEN the words stay without a link`() {
        // Given
        val line = "See the [download page](/download) too."

        // When
        val spans = paragraphSpans(line)

        // Then
        assertEquals("See the download page too.", LegalText(spans).plain)
        assertEquals(listOf(null, null, null), spans.map { it.link })
    }

    @Test
    fun `GIVEN a link with a scheme the app does not open WHEN parsing THEN the words stay without a link`() {
        // Given
        val line = "[Call us](tel:+5511999999999) or [run](javascript:alert) or [plain](http://example.com)"

        // When
        val spans = paragraphSpans(line)

        // Then
        assertEquals("Call us or run or plain", LegalText(spans).plain)
        assertEquals(emptyList(), spans.mapNotNull { it.link })
    }

    @Test
    fun `GIVEN a link to a section of the same page WHEN parsing THEN the words stay without a link`() {
        // Given
        val line = "See [section 3](#keep)."

        // When
        val spans = paragraphSpans(line)

        // Then
        assertEquals("See section 3.", LegalText(spans).plain)
        assertEquals(emptyList(), spans.mapNotNull { it.link })
    }

    @Test
    fun `GIVEN marked words WHEN parsing THEN the tags are dropped and the words kept`() {
        // Given
        val line = "Based in <mark>city to decide</mark>, Brazil."

        // When
        val spans = paragraphSpans(line)

        // Then
        assertEquals("Based in city to decide, Brazil.", LegalText(spans).plain)
        assertEquals(emptyList(), spans.filter { it.isBold || it.link != null })
    }

    @Test
    fun `GIVEN a bold link WHEN parsing THEN the words are bold and linked`() {
        // Given
        val line = "**[Terms](/terms)**"

        // When
        val spans = paragraphSpans(line)

        // Then
        assertEquals(
            listOf(
                span(
                    "Terms",
                    isBold = true,
                    link = LegalLink.Page(
                        page = LegalPage.TERMS_OF_USE,
                        anchor = null,
                    ),
                ),
            ),
            spans,
        )
    }

    @Test
    fun `GIVEN bold words inside the text of a link WHEN parsing THEN only they are bold and all of it is linked`() {
        // Given
        val line = "[the **whole** text](https://blitzsplit.com)"

        // When
        val spans = paragraphSpans(line)

        // Then
        val link = LegalLink.External("https://blitzsplit.com")
        assertEquals(
            listOf(
                span("the ", link = link),
                span(
                    "whole",
                    isBold = true,
                    link = link,
                ),
                span(" text", link = link),
            ),
            spans,
        )
    }

    @Test
    fun `GIVEN marked words inside bold WHEN parsing THEN they stay bold without the tags`() {
        // Given
        val line = "**Until <mark>30 days</mark> later**"

        // When
        val spans = paragraphSpans(line)

        // Then
        assertEquals("Until 30 days later", LegalText(spans).plain)
        assertEquals(listOf(true), spans.map { it.isBold }.distinct())
    }

    @Test
    fun `GIVEN bold marks that never close WHEN parsing THEN the line reads as it was written`() {
        // Given
        val line = "2 ** 3 is eight"

        // When
        val spans = paragraphSpans(line)

        // Then
        assertEquals(listOf(span("2 ** 3 is eight")), spans)
    }

    @Test
    fun `GIVEN bold and links in a section title a heading and a bullet WHEN parsing THEN each reads them`() {
        // Given
        val markdown =
            document("## Your **rights** {#rights}\n\n### See the [Terms](/terms)\n\n- **Access**: ask for a copy")

        // When
        val blocks = parser.parse(markdown)?.blocks

        // Then
        assertEquals(
            listOf(
                LegalBlock.Section(
                    number = 1,
                    anchor = "rights",
                    title = LegalText(listOf(span("Your "), span("rights", isBold = true))),
                ),
                LegalBlock.Heading(
                    LegalText(
                        listOf(span("See the "), span("Terms", link = LegalLink.Page(LegalPage.TERMS_OF_USE, null))),
                    ),
                ),
                LegalBlock.Bullets(listOf(LegalText(listOf(span("Access", isBold = true), span(": ask for a copy"))))),
            ),
            blocks,
        )
    }

    @Test
    fun `GIVEN a line with bold and a link WHEN reading its plain text THEN joins the words of every span`() {
        // Given
        val text = LegalText(
            listOf(
                span("Read the "),
                span(
                    "Terms",
                    isBold = true,
                    link = LegalLink.Page(LegalPage.TERMS_OF_USE, null),
                ),
                span(" first."),
            ),
        )

        // When
        val plain = text.plain

        // Then
        assertEquals("Read the Terms first.", plain)
    }

    @Test
    fun `GIVEN a line without spans WHEN reading its plain text THEN it is empty`() {
        // Given
        val text = LegalText(emptyList())

        // When
        val plain = text.plain

        // Then
        assertEquals("", plain)
    }

    private fun paragraphSpans(line: String): List<LegalSpan> {
        val block = parser.parse(document(line))?.blocks?.singleOrNull() as? LegalBlock.Paragraph
        return block?.text?.spans ?: error("Not one paragraph: $line")
    }

    private fun document(body: String): String = "---\ntitle: Privacy Policy\nupdated: 2026-11-05\n---\n\n$body\n"

    private fun span(
        text: String,
        isBold: Boolean = false,
        link: LegalLink? = null,
    ): LegalSpan = LegalSpan(
        text = text,
        isBold = isBold,
        link = link,
    )
}
