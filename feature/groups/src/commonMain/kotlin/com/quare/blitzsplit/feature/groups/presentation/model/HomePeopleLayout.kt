package com.quare.blitzsplit.feature.groups.presentation.model

/*
 * Why: how Início's "Saldos com pessoas" draws its people, the same on the three UIs (beta feedback): a row of faces
 * left one person alone on a phone with the rest of the width empty. A list row (face, name, amount and "te deve" on
 * one line) is under half a face's height, so while the rows take at most two lines they free height and use the
 * width; past that the faces, side by side and scrolling, take less. Each UI passes the section's own width in dp (pt
 * on Apple, CSS px on the web): the rows sit in as many columns as fit, one on a phone upright, two lying down or on a
 * foldable open, more on a tablet, the Mac and the desktop.
 */
data class HomePeopleLayout(
    // Why: false for the row of faces.
    val showsRows: Boolean,
    // Why: how many columns the rows sit in, 1 or more; ignored for the faces.
    val columns: Int,
) {
    companion object {
        // Why: a row's narrowest: a face, a name of about ten letters and the amount without cutting either.
        const val ROW_MIN_WIDTH = 280f
        const val MAX_ROW_LINES = 2

        // Why: the gap the three UIs leave between two columns, so each column keeps its ROW_MIN_WIDTH.
        const val COLUMN_GAP = 8f

        fun of(
            peopleCount: Int,
            sectionWidth: Float,
        ): HomePeopleLayout {
            val columns = ((sectionWidth + COLUMN_GAP) / (ROW_MIN_WIDTH + COLUMN_GAP)).toInt().coerceAtLeast(1)
            val lines = (peopleCount + columns - 1) / columns
            return HomePeopleLayout(
                showsRows = peopleCount > 0 && lines <= MAX_ROW_LINES,
                columns = columns.coerceAtMost(peopleCount.coerceAtLeast(1)),
            )
        }
    }
}
