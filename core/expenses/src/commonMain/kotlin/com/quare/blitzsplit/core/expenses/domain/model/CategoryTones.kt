package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: what a category's color draws with, in light and dark, each 0xRRGGBB: the badge (container) with its icon or text
 * (content), and the mark that carries the category in a chart (data). The twelve named colors have theirs written in
 * each platform's palette; a custom color's come from here, so the three UIs draw the same tones.
 */
data class CategoryTones(
    val lightContainer: Int,
    val lightContent: Int,
    val darkContainer: Int,
    val darkContent: Int,
    val lightData: Int,
    val darkData: Int,
)
