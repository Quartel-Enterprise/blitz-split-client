package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: the twelve colors a category may have (7.7), the same on every platform. The code is what Room and the server
 * store for a group's main category, so it never changes even if an entry is renamed. Distinct from the five avatar
 * colors, so the app's ten main categories each get a color of their own.
 */
enum class CategoryColor(
    val code: String,
) {
    BLUE("blue"),
    ORANGE("orange"),
    GREEN("green"),
    PURPLE("purple"),
    YELLOW("yellow"),
    RED("red"),
    TEAL("teal"),
    PINK("pink"),
    INDIGO("indigo"),
    LIME("lime"),
    BROWN("brown"),
    GRAY("gray"),
    ;

    companion object {
        fun fromCodeOrNull(code: String?): CategoryColor? = entries.firstOrNull { it.code == code }
    }
}
