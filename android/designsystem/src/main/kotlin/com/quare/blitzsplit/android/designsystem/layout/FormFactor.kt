package com.quare.blitzsplit.android.designsystem.layout

/*
 * Why: the canvas draws each screen once per size it names (phone, phone lying down, tablet upright and lying down,
 * foldable upright, book, tabletop), and each flow picks its layout and presentation per size. A foldable is told by
 * the hinge it reports, not by its window alone: a Galaxy Z Fold held as a book is 673×841 with a vertical hinge, the
 * size of a Pixel Fold upright, whose hinge runs across.
 */
enum class FormFactor {
    Phone,
    PhoneLandscape,
    TabletPortrait,
    TabletLandscape,
    FoldableUpright,
    Book,
    Tabletop,
}
