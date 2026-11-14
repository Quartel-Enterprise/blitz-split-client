package com.quare.blitzsplit.core.expenses.domain.model

// Why: brand-neutral swatches for a card's icon; no bank or card brand logo is ever drawn.
enum class CardColor(
    val code: String,
) {
    PURPLE("purple"),
    BLUE("blue"),
    GREEN("green"),
    ORANGE("orange"),
    RED("red"),
    PINK("pink"),
    GOLD("gold"),
    GRAPHITE("graphite"),
    ;

    companion object {
        fun ofCode(code: String): CardColor? = entries.find { it.code == code }
    }
}
