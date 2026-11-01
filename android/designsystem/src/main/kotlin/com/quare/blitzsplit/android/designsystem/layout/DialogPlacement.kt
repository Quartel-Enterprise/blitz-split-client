package com.quare.blitzsplit.android.designsystem.layout

import androidx.compose.runtime.Composable

/*
 * Why: where a dialog sits on a foldable, never over the hinge. A book draws it in the end half (right in English and
 * Portuguese) with the other half under the scrim, a tabletop in the top half; anything else centers it.
 */
enum class DialogPlacement {
    Centered,
    EndHalf,
    TopHalf,
}

/*
 * Why: the canvas' default per posture. A tabletop lying down has the book's halves and draws its dialogs like the
 * book's (flows 1 and 7); an upright tabletop draws them in its top half. A flow whose boards put them in a tabletop's
 * top half even lying down (flow 2) passes TopHalf itself.
 */
@Composable
fun rememberDialogPlacement(): DialogPlacement {
    val fold = rememberFoldLayout()
    return when {
        fold.bookSplit != null -> DialogPlacement.EndHalf
        fold.tabletopSplit != null -> DialogPlacement.TopHalf
        else -> DialogPlacement.Centered
    }
}
