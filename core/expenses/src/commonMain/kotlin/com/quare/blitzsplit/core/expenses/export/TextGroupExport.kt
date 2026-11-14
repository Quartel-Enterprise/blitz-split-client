package com.quare.blitzsplit.core.expenses.export

/*
 * Why: a plain text to read or paste in a chat: the group's name and the day, then each section as a table lined up in
 * columns, the amounts to the right.
 */
internal class TextGroupExport : GroupExportEncoder {
    override fun encode(
        export: GroupExport,
        words: GroupExportWords,
    ): ByteArray {
        val sections = GroupExportTables.of(export, words).map(::sectionOf)
        val text = (listOf("${export.groupName}\n${export.exportedOn}\n") + sections).joinToString("\n")
        return text.encodeToByteArray()
    }

    private fun sectionOf(table: ExportTable): String {
        val cells = table.rows.map { row -> row.map { it.toPlainText().replace('\n', ' ') } }
        val widths = table.headers.indices.map { column ->
            (listOf(table.headers[column]) + cells.map { it[column] }).maxOf { it.length }
        }
        val isRightAligned = table.headers.indices.map { column -> table.rows.any { it[column] is ExportCell.Amount } }

        fun lineOf(fields: List<String>): String = fields
            .mapIndexed { column, field ->
                if (isRightAligned[column]) field.padStart(widths[column]) else field.padEnd(widths[column])
            }.joinToString(COLUMN_GAP)
            .trimEnd()
        return buildString {
            append(table.title).append('\n')
            append(lineOf(table.headers)).append('\n')
            cells.forEach { append(lineOf(it)).append('\n') }
        }
    }

    private companion object {
        const val COLUMN_GAP = "  "
    }
}
