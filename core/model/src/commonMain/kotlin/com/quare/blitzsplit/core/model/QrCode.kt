package com.quare.blitzsplit.core.model

/*
 * Why: the modules of a QR code, computed once in Kotlin so the three UIs draw the same code. Row by row, size by
 * size, true for a dark module; the UIs add the quiet zone around it.
 */
data class QrCode(
    val size: Int,
    val modules: List<Boolean>,
) {
    fun isDark(
        row: Int,
        column: Int,
    ): Boolean = modules[row * size + column]
}
