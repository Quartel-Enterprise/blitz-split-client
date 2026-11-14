package com.quare.blitzsplit.core.expenses.export

/*
 * Why: "Export group data" (the group's ⋮ menu) writes one of these, picked by the person: a spreadsheet as CSV or
 * Excel, the raw data as JSON, or a plain text to read or paste. Each UI hands the file to the system as this type.
 */
enum class GroupExportFormat(
    val extension: String,
    val mimeType: String,
) {
    CSV("csv", "text/csv"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    JSON("json", "application/json"),
    TXT("txt", "text/plain"),
}
