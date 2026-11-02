package com.quare.blitzsplit.core.model

/*
 * Why: "Forma" of "Ícone do grupo" (6.4d): "Biscoito", "Pétala", "Quadrado" and "Círculo", in the order Android lists
 * them. Only Android draws it; the Apple apps and the web keep every group in their rounded square. The code is what
 * Room and the API store, so it never changes even if an entry is renamed.
 */
enum class GroupShape(
    val code: String,
) {
    COOKIE("cookie"),
    PETAL("petal"),
    SQUARE("square"),
    CIRCLE("circle"),
    ;

    // Why: Android's "Novo grupo" steps through the shapes as it does the colors.
    fun next(): GroupShape = entries[(ordinal + 1) % entries.size]

    companion object {
        // Why: the shape "Novo grupo" starts with, as the boards draw a new group.
        val default: GroupShape = COOKIE

        // Why: a group made before shapes keeps the rounded square every group had.
        val fallback: GroupShape = SQUARE

        fun fromCodeOrNull(code: String?): GroupShape? = entries.firstOrNull { it.code == code }
    }
}
