package com.quare.blitzsplit.core.expenses.export

/*
 * Why: the file "Export group data" made, which each UI only hands to the system (Android's save sheet, Apple's file
 * exporter, the browser's download): its bytes, its name with the extension, and its type.
 */
class GroupExportFile(
    val fileName: String,
    val format: GroupExportFormat,
    val bytes: ByteArray,
) {
    val mimeType: String
        get() = format.mimeType
}
