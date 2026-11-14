package com.quare.blitzsplit.core.expenses.export

/*
 * Why: an Excel workbook (Office Open XML) without a library: a sheet per section, named by the UI's words, the header
 * row bold and frozen, dates as real dates and amounts as numbers with their currency's decimals, so a sum or a
 * filter works at once. Strings are written inline, so the workbook needs no shared strings part.
 */
internal class XlsxGroupExport : GroupExportEncoder {
    // Why: the characters Excel refuses in a sheet's name.
    private val sheetNameForbidden = setOf(':', '\\', '/', '?', '*', '[', ']')

    override fun encode(
        export: GroupExport,
        words: GroupExportWords,
    ): ByteArray {
        val tables = GroupExportTables.of(export, words)
        val digits = tables
            .flatMap { table -> table.rows.flatten() }
            .filterIsInstance<ExportCell.Amount>()
            .map { it.currency.fractionDigits }
            .distinct()
            .sorted()
        val amountStyles = digits.withIndex().associate { (index, digit) -> digit to FIRST_AMOUNT_STYLE + index }
        val names = sheetNamesOf(tables.map { it.title })
        val sheets = tables.mapIndexed { index, table ->
            "xl/worksheets/sheet${index + 1}.xml" to sheetOf(table, amountStyles).encodeToByteArray()
        }
        val parts = listOf(
            "[Content_Types].xml" to contentTypesOf(tables.size),
            "_rels/.rels" to ROOT_RELATIONSHIPS,
            "xl/workbook.xml" to workbookOf(names),
            "xl/_rels/workbook.xml.rels" to workbookRelationshipsOf(tables.size),
            "xl/styles.xml" to stylesOf(digits),
        ).map { (name, xml) -> name to xml.encodeToByteArray() }
        return StoredZip.of(parts + sheets)
    }

    private fun sheetOf(
        table: ExportTable,
        amountStyles: Map<Int, Int>,
    ): String = buildString {
        append(XML_HEADER)
        append("""<worksheet xmlns="$MAIN_NS">""")
        append("""<sheetViews><sheetView workbookViewId="0">""")
        append("""<pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>""")
        append("</sheetView></sheetViews>")
        append("<cols>")
        widthsOf(table).forEachIndexed { index, width ->
            append("""<col min="${index + 1}" max="${index + 1}" width="$width" customWidth="1"/>""")
        }
        append("</cols><sheetData>")
        append("""<row r="1">""")
        table.headers.forEachIndexed { column, header -> append(buildTextCell(column, 1, header, STYLE_HEADER)) }
        append("</row>")
        table.rows.forEachIndexed { index, row ->
            val rowNumber = index + 2
            append("""<row r="$rowNumber">""")
            row.forEachIndexed { column, cell ->
                val ref = referenceOf(column, rowNumber)
                when (cell) {
                    is ExportCell.Text -> append(buildTextCell(column, rowNumber, cell.value, null))

                    is ExportCell.Day -> append(
                        """<c r="$ref" s="$STYLE_DATE"><v>${cell.date.toEpochDays() + EXCEL_EPOCH_OFFSET}</v></c>""",
                    )

                    is ExportCell.Amount -> append(
                        """<c r="$ref" s="${amountStyles.getValue(cell.currency.fractionDigits)}">""" +
                            "<v>${decimalOf(cell.minor, cell.currency)}</v></c>",
                    )

                    ExportCell.Empty -> Unit
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    private fun buildTextCell(
        column: Int,
        row: Int,
        text: String,
        style: Int?,
    ): String {
        val styleAttribute = style?.let { """ s="$it"""" }.orEmpty()
        return """<c r="${referenceOf(column, row)}" t="inlineStr"$styleAttribute>""" +
            """<is><t xml:space="preserve">${toEscaped(text)}</t></is></c>"""
    }

    // Why: wide enough for the longest text of the column, within reason.
    private fun widthsOf(table: ExportTable): List<Int> = table.headers.indices.map { column ->
        val longest = (listOf(table.headers[column]) + table.rows.map { it[column].toPlainText() }).maxOf { it.length }
        (longest + COLUMN_PADDING).coerceIn(MIN_COLUMN_WIDTH, MAX_COLUMN_WIDTH)
    }

    // Why: "A1", "B1", … "Z1", "AA1": a cell's place, as each cell of a sheet names it.
    private fun referenceOf(
        column: Int,
        row: Int,
    ): String {
        var index = column + 1
        val letters = StringBuilder()
        while (index > 0) {
            val remainder = (index - 1) % LETTERS
            letters.insert(0, 'A' + remainder)
            index = (index - 1) / LETTERS
        }
        return "$letters$row"
    }

    // Why: Excel refuses a workbook whose sheet names repeat, hold :\/?*[] or pass 31 characters.
    private fun sheetNamesOf(titles: List<String>): List<String> {
        val used = mutableSetOf<String>()
        return titles.mapIndexed { index, title ->
            val clean = title.filterNot { it in sheetNameForbidden }.trim().take(MAX_SHEET_NAME)
            var name = clean.ifEmpty { "Sheet${index + 1}" }
            var copy = 2
            while (!used.add(name.lowercase())) {
                val suffix = " ($copy)"
                name = clean.take(MAX_SHEET_NAME - suffix.length) + suffix
                copy++
            }
            name
        }
    }

    private fun contentTypesOf(sheetCount: Int): String = buildString {
        append(XML_HEADER)
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="$CONTENT_TYPE.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="$CONTENT_TYPE.styles+xml"/>""")
        (1..sheetCount).forEach {
            append("""<Override PartName="/xl/worksheets/sheet$it.xml" ContentType="$CONTENT_TYPE.worksheet+xml"/>""")
        }
        append("</Types>")
    }

    private fun workbookOf(names: List<String>): String = buildString {
        append(XML_HEADER)
        append("""<workbook xmlns="$MAIN_NS" xmlns:r="$DOCUMENT_RELATIONSHIPS"><sheets>""")
        names.forEachIndexed { index, name ->
            append("""<sheet name="${toEscaped(name)}" sheetId="${index + 1}" r:id="rId${index + 1}"/>""")
        }
        append("</sheets></workbook>")
    }

    private fun workbookRelationshipsOf(sheetCount: Int): String = buildString {
        append(XML_HEADER)
        append("""<Relationships xmlns="$RELATIONSHIPS_NS">""")
        (1..sheetCount).forEach {
            append(
                """<Relationship Id="rId$it" Type="$DOCUMENT_RELATIONSHIPS/worksheet" """ +
                    """Target="worksheets/sheet$it.xml"/>""",
            )
        }
        append(
            """<Relationship Id="rId${sheetCount + 1}" Type="$DOCUMENT_RELATIONSHIPS/styles" Target="styles.xml"/>""",
        )
        append("</Relationships>")
    }

    // Why: one amount style per number of decimals the export's currencies have (none for yen, 8 for bitcoin).
    private fun stylesOf(digits: List<Int>): String = buildString {
        val customDigits = digits.filter { it != 0 && it != 2 }
        val formats = digits.associateWith { digit ->
            when (digit) {
                0 -> FORMAT_WHOLE
                2 -> FORMAT_CENTS
                else -> FIRST_CUSTOM_FORMAT + customDigits.indexOf(digit)
            }
        }
        append(XML_HEADER)
        append("""<styleSheet xmlns="$MAIN_NS">""")
        if (customDigits.isNotEmpty()) {
            append("""<numFmts count="${customDigits.size}">""")
            customDigits.forEach { digit ->
                append("""<numFmt numFmtId="${formats.getValue(digit)}" formatCode="#,##0.${"0".repeat(digit)}"/>""")
            }
            append("</numFmts>")
        }
        append("""<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font>""")
        append("""<font><b/><sz val="11"/><name val="Calibri"/></font></fonts>""")
        append("""<fills count="2"><fill><patternFill patternType="none"/></fill>""")
        append("""<fill><patternFill patternType="gray125"/></fill></fills>""")
        append("""<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""")
        append("""<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""")
        append("""<cellXfs count="${FIRST_AMOUNT_STYLE + digits.size}">""")
        append("""<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""")
        append("""<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>""")
        append(
            """<xf numFmtId="$SHORT_DATE_FORMAT" fontId="0" fillId="0" borderId="0" xfId="0" """ +
                """applyNumberFormat="1"/>""",
        )
        digits.forEach { digit ->
            append(
                """<xf numFmtId="${formats.getValue(digit)}" fontId="0" fillId="0" borderId="0" xfId="0" """ +
                    """applyNumberFormat="1"/>""",
            )
        }
        append("</cellXfs>")
        append("""<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>""")
        append("</styleSheet>")
    }

    // Why: XML's five escapes, and no control character, which XML 1.0 refuses even escaped.
    private fun toEscaped(text: String): String = buildString {
        text.forEach { char ->
            when {
                char == '&' -> append("&amp;")
                char == '<' -> append("&lt;")
                char == '>' -> append("&gt;")
                char == '"' -> append("&quot;")
                char < ' ' && char != '\t' && char != '\n' && char != '\r' -> Unit
                else -> append(char)
            }
        }
    }

    private companion object {
        const val XML_HEADER = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""
        const val MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        const val RELATIONSHIPS_NS = "http://schemas.openxmlformats.org/package/2006/relationships"
        const val DOCUMENT_RELATIONSHIPS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
        const val CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml"

        // Why: the days between Excel's day 0 (1899-12-30) and 1970-01-01, which a date cell counts from.
        const val EXCEL_EPOCH_OFFSET = 25569L

        // Why: Excel's longest sheet name.
        const val MAX_SHEET_NAME = 31

        // Why: cellXfs of styles.xml: 0 plain, 1 the bold header row, 2 a date in the reader's own short format.
        const val STYLE_HEADER = 1
        const val STYLE_DATE = 2
        const val FIRST_AMOUNT_STYLE = 3

        // Why: Excel's own number formats with thousands, "#,##0" and "#,##0.00", and where custom ones start.
        const val FORMAT_WHOLE = 3
        const val FORMAT_CENTS = 4
        const val FIRST_CUSTOM_FORMAT = 164
        const val SHORT_DATE_FORMAT = 14
        const val MIN_COLUMN_WIDTH = 8
        const val MAX_COLUMN_WIDTH = 60
        const val COLUMN_PADDING = 2
        const val LETTERS = 26
        const val ROOT_RELATIONSHIPS = XML_HEADER +
            """<Relationships xmlns="$RELATIONSHIPS_NS">""" +
            """<Relationship Id="rId1" Type="$DOCUMENT_RELATIONSHIPS/officeDocument" Target="xl/workbook.xml"/>""" +
            "</Relationships>"
    }
}
