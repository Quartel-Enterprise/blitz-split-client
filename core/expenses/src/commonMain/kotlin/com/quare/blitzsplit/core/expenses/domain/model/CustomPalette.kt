package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: the person's own colors fit in the picker beside the twelve named ones, two rows more on a phone, so the palette
 * holds as many; one more waits for one of them to be deleted. Two devices adding offline may go past it, which the
 * picker shows all the same.
 */
object CustomPalette {
    const val MAX_COLORS: Int = 12
}

// Why: "+" with a full palette, which the picker never offers; a stale tap lands here.
class CustomPaletteFullException : IllegalStateException("The custom color palette is full")
