package com.quare.blitzsplit.core.expenses.export

// Why: one per format, each turning the same GroupExport into its file's bytes.
internal fun interface GroupExportEncoder {
    fun encode(
        export: GroupExport,
        words: GroupExportWords,
    ): ByteArray
}
