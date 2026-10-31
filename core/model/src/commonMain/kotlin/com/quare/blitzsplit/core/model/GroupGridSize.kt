package com.quare.blitzsplit.core.model

/*
 * Why: the grid's "Compacto", "Padrão" and "Grande". Each UI gives each size a minimum card width, and the columns
 * are as many as fit the list's width, never a number picked. Not DEFAULT, which Swift would rename.
 */
enum class GroupGridSize(
    val code: String,
) {
    COMPACT("compact"),
    STANDARD("standard"),
    LARGE("large"),
    ;

    companion object {
        fun fromCodeOrNull(code: String): GroupGridSize? = entries.firstOrNull { it.code == code }
    }
}
