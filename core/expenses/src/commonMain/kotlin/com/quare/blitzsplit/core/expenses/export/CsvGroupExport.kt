package com.quare.blitzsplit.core.expenses.export

/*
 * Why: the sections one under the other, each its title, its headers and its rows, a blank line between them. Commas
 * between cells and dots in the decimals, as RFC 4180 has it, so the file reads the same in any language.
 */
internal class CsvGroupExport : GroupExportEncoder {
    // Why: what a spreadsheet would run as a formula at the start of a cell someone typed (a bill's description).
    private val formulaStarts = setOf('=', '+', '-', '@', '\t', '\r')

    override fun encode(
        export: GroupExport,
        words: GroupExportWords,
    ): ByteArray {
        val text = GroupExportTables.of(export, words).joinToString(LINE_BREAK) { table ->
            buildString {
                append(lineOf(listOf(table.title))).append(LINE_BREAK)
                append(lineOf(table.headers)).append(LINE_BREAK)
                table.rows.forEach { row -> append(lineOf(row.map(::toField))).append(LINE_BREAK) }
            }
        }
        return (BYTE_ORDER_MARK + text).encodeToByteArray()
    }

    // Why: a text someone typed that a spreadsheet would run as a formula is kept as text by a leading apostrophe.
    private fun toField(cell: ExportCell): String {
        val text = cell.toPlainText()
        return if (cell is ExportCell.Text && text.firstOrNull() in formulaStarts) "'$text" else text
    }

    private fun lineOf(fields: List<String>): String = fields.joinToString(",") { field ->
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }

    private companion object {
        // Why: Excel only reads a CSV as UTF-8 (accents, "Fixar no Início") when the file opens with this mark.
        const val BYTE_ORDER_MARK = "\uFEFF"

        // Why: RFC 4180's line break, which every spreadsheet reads.
        const val LINE_BREAK = "\r\n"
    }
}
