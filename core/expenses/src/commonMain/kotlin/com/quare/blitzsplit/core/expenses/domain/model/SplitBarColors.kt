package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: a bar that splits one amount among people (Divisão padrão's example) gives each person a color of their own, so
 * two people whose avatars share a color (a photo over the default Blue) never read as one segment (beta feedback of
 * 2026-11-15). Five category colors whose data tones sit apart from each other and from the money green and the debt
 * orange (CIEDE2000 of 17 or more between neighbors, 18 or more from either status color), each next to the ones it
 * differs from most. The sixth person takes the first color again, so neighbors in the list still differ; two people
 * of one color meet in the bar only when everyone between them is out of the split.
 */
object SplitBarColors {
    val palette: List<CategoryColor> = listOf(
        CategoryColor.BLUE,
        CategoryColor.YELLOW,
        CategoryColor.PINK,
        CategoryColor.LIME,
        CategoryColor.PURPLE,
    )

    // Why: by the person's place in the group's list, so a color stays with them while others join or leave the split.
    fun getAt(index: Int): CategoryColor = palette[index.mod(palette.size)]
}
