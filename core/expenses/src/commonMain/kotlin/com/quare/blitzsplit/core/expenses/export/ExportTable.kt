package com.quare.blitzsplit.core.expenses.export

import com.quare.blitzsplit.core.model.GroupCurrency
import kotlinx.datetime.LocalDate

// Why: a section of the export (Bills, Payments, Balances) as CSV, Excel and the text draw it: a title, headers, rows.
class ExportTable(
    val title: String,
    val headers: List<String>,
    val rows: List<List<ExportCell>>,
)

// Why: what a cell holds, so Excel writes a date and a number where CSV and the text write their text.
sealed interface ExportCell {
    data class Text(
        val value: String,
    ) : ExportCell

    data class Day(
        val date: LocalDate,
    ) : ExportCell

    data class Amount(
        val minor: Long,
        val currency: GroupCurrency,
    ) : ExportCell

    // Why: a member who neither paid nor is in a bill's split.
    data object Empty : ExportCell
}

// Why: one column of a table: its header and what each row puts in it.
internal class ExportColumn<T>(
    val header: String,
    val cell: (T) -> ExportCell,
)

internal fun <T> tableOf(
    title: String,
    columns: List<ExportColumn<T>>,
    rows: List<T>,
): ExportTable = ExportTable(
    title = title,
    headers = columns.map { it.header },
    rows = rows.map { row -> columns.map { it.cell(row) } },
)

/*
 * Why: "120.50", "-10.00", "1500" for yen: every decimal of the currency with a dot and no grouping, which a
 * spreadsheet, a script and a person all read the same in any language. The text of a cell in CSV, JSON and the text.
 */
internal fun decimalOf(
    minor: Long,
    currency: GroupCurrency,
): String {
    val digits = currency.fractionDigits
    val sign = if (minor < 0) "-" else ""
    val absolute = minor.toString().removePrefix("-")
    if (digits == 0) return sign + absolute
    val padded = absolute.padStart(digits + 1, '0')
    return sign + padded.dropLast(digits) + "." + padded.takeLast(digits)
}

// Why: the text of a cell where no spreadsheet types it (CSV, the text file): ISO dates, decimals with a dot.
internal fun ExportCell.toPlainText(): String = when (this) {
    is ExportCell.Text -> value
    is ExportCell.Day -> date.toString()
    is ExportCell.Amount -> decimalOf(minor, currency)
    ExportCell.Empty -> ""
}
