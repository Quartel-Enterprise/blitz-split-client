package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: a card's color picks from a category's (Pierre's call, feedback of November 15, 2026): the twelve named colors and
 * the person's own palette, stored as the same codes ("teal", "#3A7BD5"). Never a bank's or a card brand's logo.
 */
object CardTints {
    // Why: what a new card starts on, and what a card saved without a color draws.
    val default: CategoryTint = CategoryColor.PURPLE.tint

    /*
     * Why: the first cards were saved with eight keys of their own. Purple, blue, green, orange, red and pink are named
     * colors already; gold reads as yellow and graphite as gray, the nearest of the twelve by hue. Nothing is rewritten:
     * the card keeps its old key until it is saved again. A code this version doesn't know reads as none.
     */
    fun fromCodeOrNull(code: String?): CategoryTint? = CategoryTint.fromCodeOrNull(code) ?: legacyColors[code]?.tint

    /*
     * Why: a card tile's fill (0xRRGGBB), the same in light and dark: the color's light data tone, a named color's base or
     * a custom color's derived one, which the white card glyph reads on (4.5:1), a light yellow included.
     */
    fun fillRgbOf(tint: CategoryTint?): Int = when (val color = tint ?: default) {
        is CategoryTint.Named -> color.color.baseRgb
        is CategoryTint.Custom -> color.color.toTones().lightData
    }

    private val legacyColors = mapOf(
        "gold" to CategoryColor.YELLOW,
        "graphite" to CategoryColor.GRAY,
    )
}
