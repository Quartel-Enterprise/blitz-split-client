package com.quare.blitzsplit.core.legal.domain.model

sealed interface LegalBlock {
    /*
     * Why: a numbered section's title ("3 Where your data lives"). anchor is what a link to it names
     * (/privacy#keep), in the text's own language.
     */
    data class Section(
        val number: Int,
        val anchor: String,
        val title: LegalText,
    ) : LegalBlock

    // Why: a smaller title inside a section.
    data class Heading(
        val text: LegalText,
    ) : LegalBlock

    data class Paragraph(
        val text: LegalText,
    ) : LegalBlock

    data class Bullets(
        val items: List<LegalText>,
    ) : LegalBlock

    /*
     * Why: a highlighted box: the summary that opens the text ("In short", with items) or a note inside a section
     * (with a paragraph).
     */
    data class Callout(
        val title: LegalText,
        val paragraph: LegalText?,
        val items: List<LegalText>,
    ) : LegalBlock

    /*
     * Why: a table of few columns ("Purpose", "Data", "Legal basis"). On a narrow screen each row reads as a card: its
     * first cell the title, the others under their column's name.
     */
    data class Table(
        val header: List<LegalText>,
        val rows: List<LegalTableRow>,
    ) : LegalBlock
}
