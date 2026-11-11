package com.quare.blitzsplit.core.legal.data.parser

import com.quare.blitzsplit.core.legal.domain.model.LegalBlock
import com.quare.blitzsplit.core.legal.domain.model.LegalDate
import com.quare.blitzsplit.core.legal.domain.model.LegalDocument
import com.quare.blitzsplit.core.legal.domain.model.LegalTableRow
import com.quare.blitzsplit.core.legal.domain.model.LegalText

class LegalDocumentParserImpl : LegalDocumentParser {
    private val frontMatterLine = Regex("([a-z_]+):(.*)")
    private val datePattern = Regex("""(\d{4})-(\d{2})-(\d{2})""")

    override fun parse(markdown: String): LegalDocument? {
        // Why: an editor's byte order mark or a blank first line is no reason to lose the whole text.
        val lines = markdown
            .removePrefix(BYTE_ORDER_MARK)
            .replace("\r\n", "\n")
            .trim()
            .split("\n")
        if (lines.firstOrNull() != FRONT_MATTER_FENCE) return null
        val frontMatterEnd = lines.drop(1).indexOf(FRONT_MATTER_FENCE) + 1
        if (frontMatterEnd == 0) return null
        val frontMatter = lines
            .subList(1, frontMatterEnd)
            .mapNotNull { line -> frontMatterLine.matchEntire(line)?.destructured }
            .associate { (key, value) -> key to value.trim() }
        val title = frontMatter["title"]?.takeIf { it.isNotEmpty() } ?: return null
        val updated = frontMatter["updated"]?.let(::parseDate) ?: return null
        return LegalDocument(
            title = title,
            updated = updated,
            blocks = BlockReader(lines.drop(frontMatterEnd + 1)).read(),
        )
    }

    private fun parseDate(value: String): LegalDate? {
        val (year, month, day) = datePattern.matchEntire(value)?.destructured ?: return null
        return LegalDate(
            year = year.toInt(),
            month = month.toInt(),
            day = day.toInt(),
        ).takeIf { it.month in 1..MONTHS && it.day in 1..MAX_DAYS }
    }

    // Why: one pass over the text's lines, a blank one closing whatever block was open.
    private class BlockReader(
        private val lines: List<String>,
    ) {
        private val blockMarks = listOf(SECTION_MARK, HEADING_MARK, BULLET_MARK, TABLE_MARK, CALLOUT_MARK)
        private val sectionAnchor = Regex("""(.*?)\s*\{#([a-z0-9-]+)\}\s*""")
        private val tableRule = Regex(":?-{3,}:?")
        private var index = 0
        private var sectionCount = 0

        fun read(): List<LegalBlock> = buildList {
            while (index < lines.size) {
                val line = lines[index]
                when {
                    line.isBlank() -> index++

                    line.startsWith(SECTION_MARK) -> add(readSection(line))

                    line.startsWith(HEADING_MARK) -> add(readHeading(line))

                    line.startsWith(
                        BULLET_MARK,
                    ) -> add(LegalBlock.Bullets(toBullets(takeLines { it.startsWith(BULLET_MARK) })))

                    line.startsWith(TABLE_MARK) -> add(readTable())

                    line.startsWith(CALLOUT_MARK) -> add(readCallout())

                    else -> add(readParagraph())
                }
            }
        }

        private fun readSection(line: String): LegalBlock.Section {
            index++
            sectionCount++
            val match = sectionAnchor.matchEntire(line.removePrefix(SECTION_MARK))
            return LegalBlock.Section(
                number = sectionCount,
                // Why: a section the site left without an anchor still shows; nothing links to it.
                anchor = match?.groupValues?.get(2).orEmpty(),
                title = LegalInlineParser.parse(match?.groupValues?.get(1) ?: line.removePrefix(SECTION_MARK)),
            )
        }

        private fun readHeading(line: String): LegalBlock.Heading {
            index++
            return LegalBlock.Heading(LegalInlineParser.parse(line.removePrefix(HEADING_MARK)))
        }

        private fun readParagraph(): LegalBlock.Paragraph {
            val text = takeLines { it.isNotBlank() && blockMarks.none(it::startsWith) }
                .ifEmpty { listOf(lines[index++]) }
                .joinToString(separator = " ") { it.trim() }
            return LegalBlock.Paragraph(LegalInlineParser.parse(text))
        }

        private fun toBullets(items: List<String>): List<LegalText> =
            items.map { LegalInlineParser.parse(it.removePrefix(BULLET_MARK)) }

        private fun readTable(): LegalBlock {
            val rows = takeLines { it.startsWith(TABLE_MARK) }.map(::splitRow)
            val body = rows.drop(1).filterNot { cells -> cells.all { tableRule.matches(it) } }
            val header = rows.first()
            return LegalBlock.Table(
                header = header.map(LegalInlineParser::parse),
                rows = body.map { cells ->
                    // Why: every row with the header's cells, so the UIs never index past one.
                    LegalTableRow(
                        List(header.size) { cell -> LegalInlineParser.parse(cells.getOrNull(cell).orEmpty()) },
                    )
                },
            )
        }

        private fun splitRow(line: String): List<String> = line
            .trim()
            .removePrefix(TABLE_MARK)
            .removeSuffix(TABLE_MARK)
            .split(TABLE_MARK)
            .map { it.trim() }

        private fun readCallout(): LegalBlock.Callout {
            val quoted = takeLines { it.startsWith(CALLOUT_MARK) }
                .map { it.removePrefix(CALLOUT_MARK).removePrefix(" ") }
                .filter { it.isNotBlank() }
            val title = quoted.firstOrNull().orEmpty()
            val body = quoted.drop(1)
            val items = body.filter { it.startsWith(BULLET_MARK) }
            val paragraph = body.filterNot { it.startsWith(BULLET_MARK) }.joinToString(separator = " ")
            return LegalBlock.Callout(
                title = LegalInlineParser.parse(title.removeSurrounding(BOLD_MARK)),
                paragraph = paragraph.takeIf { it.isNotEmpty() }?.let(LegalInlineParser::parse),
                items = toBullets(items),
            )
        }

        private fun takeLines(matches: (String) -> Boolean): List<String> {
            val start = index
            while (index < lines.size && matches(lines[index])) index++
            return lines.subList(start, index)
        }
    }

    private companion object {
        const val BYTE_ORDER_MARK = "\uFEFF"
        const val FRONT_MATTER_FENCE = "---"
        const val SECTION_MARK = "## "
        const val HEADING_MARK = "### "
        const val BULLET_MARK = "- "
        const val TABLE_MARK = "|"
        const val CALLOUT_MARK = ">"
        const val BOLD_MARK = "**"
        const val MONTHS = 12
        const val MAX_DAYS = 31
    }
}
