package com.quare.blitzsplit.core.expenses.customcolor

import com.quare.blitzsplit.core.expenses.domain.model.CategoryTint
import com.quare.blitzsplit.core.expenses.domain.model.ColorDraft
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.CustomPalette

/*
 * Why: the person's own colors in a category's color picker, the same on every screen that has one. Not a screen's
 * UiState: CustomColorController produces it and each screen copies it into its own.
 */
data class CustomColorState(
    // Why: the swatches after the twelve named colors, in the order they were added.
    val palette: List<CustomColor>,
    // Why: "New color" is open while not null.
    val draft: ColorDraft?,
    // Why: "Delete this color?", or "This color is in use", is open while not null.
    val deletion: CustomColorDeletion?,
) {
    // Why: "+" while the palette has room; a full one says to delete a color first.
    val canAdd: Boolean
        get() = palette.size < CustomPalette.MAX_COLORS

    /*
     * Why: the swatches after the named ones in a picker showing `selected`: the palette, then the selected color if it
     * isn't in it (a group category another member colored), so the color the category has always shows picked.
     */
    fun customSwatchesFor(selected: CategoryTint?): List<CustomColor> {
        val extra = (selected as? CategoryTint.Custom)?.color?.takeIf { it !in palette }
        return palette + listOfNotNull(extra)
    }

    // Why: only the person's own colors offer to be deleted, never another member's shown for a group category.
    fun isDeletable(color: CustomColor): Boolean = color in palette

    companion object {
        val idle = CustomColorState(
            palette = emptyList(),
            draft = null,
            deletion = null,
        )
    }
}
