package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: the twelve colors a category may have (7.7), the same on every platform. The code is what Room and the server
 * store for a group's main category, so it never changes even if an entry is renamed. Distinct from the five avatar
 * colors, so the app's ten main categories each get a color of their own.
 */
enum class CategoryColor(
    val code: String,
    // Why: where "+"'s picker starts from a category with this color: its light data tone, the hue at full strength.
    val baseRgb: Int,
) {
    BLUE("blue", 0x2A75DC),
    ORANGE("orange", 0xAE650C),
    GREEN("green", 0x418642),
    PURPLE("purple", 0x8E65B8),
    YELLOW("yellow", 0x857800),
    RED("red", 0xCC4A57),
    TEAL("teal", 0x00867C),
    PINK("pink", 0xB55882),
    INDIGO("indigo", 0x6E6EC3),
    LIME("lime", 0x678020),
    BROWN("brown", 0x986E53),
    GRAY("gray", 0x74777C),
    ;

    companion object {
        fun fromCodeOrNull(code: String?): CategoryColor? = entries.firstOrNull { it.code == code }
    }
}
